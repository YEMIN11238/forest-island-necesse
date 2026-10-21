package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.util.MoveToTileAITask;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.level.maps.CollisionFilter;

public abstract class ChaserAINode<T extends Mob> extends MoveTaskAINode<T> {
   public BiPredicate<Mob, MoveToTileAITask.AIPathResult> moveIfFailedPath;
   public String targetKey;
   public long nextPathFindTime;
   public Mob lastTarget;
   public int attackDistance;
   public int stoppingDistance;
   public int lostTargetStopAttackingDistance = -1;
   public int minimumAttackDistance;
   public int runAwayDistance = -1;
   public int directSearchDistance;
   public boolean keepMovingDirectTargetIfLost = true;
   public int timeBeforeFirstAttack = 300;
   public boolean preferLand = true;
   public boolean smartPositioning;
   public boolean changePositionOnHit;
   public boolean forceMovePosition;
   public int moveSearchPositionRange = -1;
   public boolean changePositionConstantly;
   public int maxAttacksPerPosition = -1;
   public String chaserTargetKey = "chaserTarget";
   protected boolean movingToTarget = false;
   protected int attacksSincePositionChange;
   protected int canAttackTimer;
   protected boolean hasStartedAttack;

   public ChaserAINode(String targetKey, int attackDistance, boolean smartPositioning, boolean changePositionOnHit) {
      this.targetKey = targetKey;
      this.attackDistance = attackDistance;
      this.stoppingDistance = attackDistance - 20;
      this.smartPositioning = smartPositioning;
      this.changePositionOnHit = changePositionOnHit;
   }

   public ChaserAINode(int attackDistance, boolean smartPositioning, boolean changePositionOnHit) {
      this("currentTarget", attackDistance, smartPositioning, changePositionOnHit);
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      if (this.directSearchDistance == 0) {
         int pathMoveDistance = (int)mob.getPathMoveOffset().distance(0.0, 0.0);
         this.directSearchDistance = pathMoveDistance * 8 + 32;
      }

      blackboard.onEvent("resetPathTime", e -> this.nextPathFindTime = 0L);
      blackboard.onEvent("resetTarget", e -> this.getBlackboard().put(this.chaserTargetKey, null));
      blackboard.onEvent("chaserMovePosition", e -> this.forceMovePosition = true);
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
      if (blackboard.mover.isCurrentlyMovingFor(this) && blackboard.getObject(Mob.class, this.chaserTargetKey) == null && blackboard.mover.hasMobTarget()) {
         if (this.keepMovingDirectTargetIfLost) {
            Mob target = blackboard.mover.getTargetMob();
            blackboard.mover.directMoveTo(this, target.getX(), target.getY());
         } else {
            blackboard.mover.stopMoving(mob);
         }
      }

      blackboard.put(this.chaserTargetKey, null);
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.targetKey);
      if (this.lastTarget != target) {
         this.nextPathFindTime = 0L;
      }

      this.lastTarget = target;
      if (target != null) {
         boolean forceMoveAround = this.forceMovePosition;
         this.forceMovePosition = false;
         if (!forceMoveAround && this.smartPositioning && this.changePositionOnHit) {
            Iterator distance = blackboard.getLastHits().iterator();
            if (distance.hasNext()) {
               AIWasHitEvent lastHit = (AIWasHitEvent)distance.next();
               forceMoveAround = true;
            }
         }

         float distance = mob.getDistance(target);
         boolean canHitTarget = false;
         if (this.isTargetWithinAttackRange(mob, target, distance, this.hasStartedAttack) && this.canHitTarget(mob, mob.x, mob.y, target)) {
            this.hasStartedAttack = true;
            canHitTarget = true;
            if (this.canAttackTimer >= this.timeBeforeFirstAttack && this.attackTarget(mob, target)) {
               this.attacksSincePositionChange++;
               if (this.maxAttacksPerPosition > 0 && this.attacksSincePositionChange >= this.maxAttacksPerPosition) {
                  forceMoveAround = true;
               }
            }

            this.canAttackTimer += 50;
         } else {
            this.hasStartedAttack = false;
            this.canAttackTimer -= 50;
            if (this.canAttackTimer < 0) {
               this.canAttackTimer = 0;
            }
         }

         if (this.smartPositioning && this.changePositionConstantly && !blackboard.mover.isCurrentlyMovingFor(this)) {
            forceMoveAround = true;
         }

         if (forceMoveAround) {
            this.movingToTarget = true;
            this.attacksSincePositionChange = 0;
            this.moveToTarget(mob, target, blackboard, null, () -> {
               this.movingToTarget = false;
               this.nextPathFindTime = 0L;
            });
         } else {
            boolean directChasing = false;
            if (!this.smartPositioning) {
               ChaserAINode.ChaseDirection directChaseDirection = this.getDirectChaseDirection(mob, target, distance, canHitTarget);
               switch (directChaseDirection) {
                  case INVALID:
                  default:
                     break;
                  case STAY:
                     directChasing = true;
                     blackboard.mover.stopMoving(mob);
                     break;
                  case TOWARDS:
                     directChasing = true;
                     blackboard.mover.setMobTarget(this, target);
                     break;
                  case AWAY:
                     directChasing = true;
                     Float targetDir = GameMath.normalize(mob.x - target.x, mob.y - target.y);
                     float angle = GameMath.getAngle(targetDir);
                     int totalDirections = 8;
                     int totalDirectionsAngle = 360 / totalDirections;
                     angle = GameMath.fixAngle(angle + totalDirectionsAngle / 2.0F);
                     int dir = (int)(angle / totalDirectionsAngle);
                     Float finalDir = new Float();
                     int keepDistance = Math.max(this.minimumAttackDistance, this.runAwayDistance);

                     for (int i = 0; i < totalDirections / 2; i++) {
                        int currentDir = dir - i;
                        finalDir = GameMath.getAngleDir(currentDir * totalDirectionsAngle);
                        int targetXPos = (int)(mob.x + finalDir.x * keepDistance);
                        int targetYPos = (int)(mob.y + finalDir.y * keepDistance);
                        if (!mob.getLevel().collides(new MovedRectangle(mob, targetXPos, targetYPos), mob.getLevelCollisionFilter())) {
                           break;
                        }

                        if (i != 0) {
                           currentDir = dir + i;
                           finalDir = GameMath.getAngleDir(currentDir * totalDirectionsAngle);
                           targetXPos = (int)(mob.x + finalDir.x * keepDistance);
                           targetYPos = (int)(mob.y + finalDir.y * keepDistance);
                           if (!mob.getLevel().collides(new MovedRectangle(mob, targetXPos, targetYPos), mob.getLevelCollisionFilter())) {
                              break;
                           }
                        }
                     }

                     int walkAwayDistance = keepDistance + mob.moveAccuracy;
                     blackboard.mover.setCustomMovement(this, new MobMovementRelative(target, finalDir.x * walkAwayDistance, finalDir.y * walkAwayDistance));
               }

               if (directChaseDirection != ChaserAINode.ChaseDirection.INVALID) {
                  this.nextPathFindTime = 0L;
                  this.movingToTarget = true;
                  this.attacksSincePositionChange = 0;
               }
            }

            if (!directChasing
               && (!canHitTarget || this.stoppingDistance > 0 && distance > this.stoppingDistance)
               && this.nextPathFindTime <= mob.getWorldEntity().getLocalTime()) {
               this.moveToTarget(mob, target, blackboard, (result, path) -> {
                  if (result != ChaserAINode.PathResult.SUCCESS && result != ChaserAINode.PathResult.MOVED_NO_SUCCESS) {
                     this.movingToTarget = false;
                     this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + 1000 * (GameRandom.globalRandom.nextInt(5) + 3);
                  } else {
                     this.movingToTarget = true;
                     this.attacksSincePositionChange = 0;
                     if (result == ChaserAINode.PathResult.MOVED_NO_SUCCESS) {
                        int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 5000, 0.1F);
                        this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + nextPathTimeAdd;
                     } else {
                        int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 500, 0.1F);
                        this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + nextPathTimeAdd;
                     }
                  }
               }, () -> {
                  this.movingToTarget = false;
                  this.nextPathFindTime = 0L;
               });
            }
         }
      } else {
         this.movingToTarget = false;
      }

      return this.lastTick(this.movingToTarget, target);
   }

   protected AINodeResult lastTick(boolean movingToTarget, Mob target) {
      if (movingToTarget) {
         this.getBlackboard().put(this.chaserTargetKey, target);
         return AINodeResult.SUCCESS;
      } else {
         return AINodeResult.FAILURE;
      }
   }

   public boolean isTargetWithinAttackRange(T mob, Mob target, float distanceToTarget, boolean hasStartedAttack) {
      int lostDistance = hasStartedAttack ? Math.max(this.attackDistance, this.lostTargetStopAttackingDistance) : this.attackDistance;
      return distanceToTarget <= lostDistance && distanceToTarget >= this.minimumAttackDistance;
   }

   public ChaserAINode.ChaseDirection getDirectChaseDirection(T mob, Mob target, float distanceToTarget, boolean canHitTarget) {
      if (distanceToTarget >= this.stoppingDistance && distanceToTarget >= this.directSearchDistance) {
         return ChaserAINode.ChaseDirection.INVALID;
      } else {
         CollisionFilter collisionFilter = mob.modifyChasingCollisionFilter(mob.getLevelCollisionFilter(), target);
         if (mob.getLevel().collides(new MovedRectangle(mob, target.getX(), target.getY()), collisionFilter)) {
            return ChaserAINode.ChaseDirection.INVALID;
         } else if (distanceToTarget < this.minimumAttackDistance || distanceToTarget < this.runAwayDistance) {
            return ChaserAINode.ChaseDirection.AWAY;
         } else {
            return canHitTarget && distanceToTarget < this.stoppingDistance ? ChaserAINode.ChaseDirection.STAY : ChaserAINode.ChaseDirection.TOWARDS;
         }
      }
   }

   public boolean canHitTarget(T mob, float fromX, float fromY, Mob target) {
      return hasLineOfSightToTarget(mob, fromX, fromY, target);
   }

   public static boolean hasLineOfSightToTarget(Mob mob, float fromX, float fromY, float startOffset, Mob target, float hitBoxWidth) {
      CollisionFilter collisionFilter = mob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target);
      if (startOffset != 0.0F) {
         Float dir = GameMath.normalize(target.x - fromX, target.y - fromY);
         fromX += dir.x * startOffset;
         fromY += dir.y * startOffset;
      }

      return hitBoxWidth > 0.0F
         ? !mob.getLevel().collides(new LineHitbox(fromX, fromY, target.x, target.y, hitBoxWidth), collisionFilter)
         : !mob.getLevel().collides(new java.awt.geom.Line2D.Float(fromX, fromY, target.x, target.y), collisionFilter);
   }

   public static boolean hasLineOfSightToTarget(Mob mob, float fromX, float fromY, Mob target, float hitBoxWidth) {
      return hasLineOfSightToTarget(mob, fromX, fromY, 0.0F, target, hitBoxWidth);
   }

   public static boolean hasLineOfSightToTarget(Mob mob, float fromX, float fromY, Mob target) {
      return hasLineOfSightToTarget(mob, fromX, fromY, target, 0.0F);
   }

   public static boolean hasLineOfSightToTarget(Mob from, Mob target, float hitBoxWidth) {
      return hasLineOfSightToTarget(from, from.x, from.y, target, hitBoxWidth);
   }

   public static boolean hasLineOfSightToTarget(Mob from, Mob target) {
      return hasLineOfSightToTarget(from, target, 0.0F);
   }

   public static boolean isTargetHitboxWithinRange(Mob mob, float fromX, float fromY, Mob target, float range) {
      Float dir = GameMath.normalize(target.x - fromX, target.y - fromY);
      return target.getHitBox().intersectsLine(fromX, fromY, fromX + dir.x * range, fromY + dir.y * range);
   }

   public abstract boolean attackTarget(T var1, Mob var2);

   public void moveToTarget(
      T mob, Mob target, Blackboard<T> blackboard, BiConsumer<ChaserAINode.PathResult, MoveToTileAITask.AIPathResult> moving, Runnable pathInvalidated
   ) {
      if (target == null) {
         if (moving != null) {
            moving.accept(ChaserAINode.PathResult.FAILED, null);
         }
      } else if (!this.smartPositioning) {
         this.moveToTileTask(target.getTileX(), target.getTileY(), null, path -> {
            boolean result = path.moveIfWithin(-1, this.moveIfFailedPath != null && this.moveIfFailedPath.test(target, path) ? -1 : 1, pathInvalidated);
            if (moving != null) {
               if (result) {
                  if (path.isResultWithin(1)) {
                     moving.accept(ChaserAINode.PathResult.SUCCESS, path);
                  } else {
                     moving.accept(ChaserAINode.PathResult.MOVED_NO_SUCCESS, path);
                  }
               } else {
                  moving.accept(ChaserAINode.PathResult.FAILED, path);
               }
            }

            return this.lastTick(this.movingToTarget, target);
         });
      } else {
         ArrayList<Point> firstPositions = new ArrayList<>();
         ArrayList<Point> secondPositions = new ArrayList<>();
         int searchTileRange;
         int searchLevelRange;
         if (this.moveSearchPositionRange >= 0) {
            searchTileRange = this.moveSearchPositionRange / 32;
            searchLevelRange = this.moveSearchPositionRange;
         } else {
            searchTileRange = (this.attackDistance - 1) / 32;
            searchLevelRange = this.attackDistance;
         }

         for (int x = -searchTileRange; x <= searchTileRange; x++) {
            int actualX = target.getTileX() + x;

            for (int y = -searchTileRange; y <= searchTileRange; y++) {
               int actualY = target.getTileY() + y;
               if (!mob.getLevel().isSolidTile(actualX, actualY)) {
                  float distance = target.getDistance(actualX * 32 + 16, actualY * 32 + 16);
                  if (!(distance < 64.0F) && !(distance > searchLevelRange) && this.canHitTarget(mob, actualX * 32 + 16, actualY * 32 + 16, target)) {
                     if (this.preferLand && mob.getLevel().isLiquidTile(actualX, actualY)) {
                        secondPositions.add(new Point(actualX, actualY));
                     } else {
                        firstPositions.add(new Point(actualX, actualY));
                     }
                  }
               }
            }
         }

         Point finalPoint = null;
         if (firstPositions.size() != 0) {
            finalPoint = firstPositions.get(GameRandom.globalRandom.nextInt(firstPositions.size()));
         } else if (secondPositions.size() != 0) {
            finalPoint = secondPositions.get(GameRandom.globalRandom.nextInt(secondPositions.size()));
         }

         if (finalPoint != null) {
            this.moveToTileTask(finalPoint.x, finalPoint.y, null, path -> {
               boolean result = path.moveIfWithin(-1, this.moveIfFailedPath != null && this.moveIfFailedPath.test(target, path) ? -1 : 1, pathInvalidated);
               if (moving != null) {
                  if (result) {
                     if (path.isResultWithin(1)) {
                        moving.accept(ChaserAINode.PathResult.SUCCESS, path);
                     } else {
                        moving.accept(ChaserAINode.PathResult.MOVED_NO_SUCCESS, path);
                     }
                  } else {
                     moving.accept(ChaserAINode.PathResult.FAILED, path);
                  }
               }

               return this.lastTick(this.movingToTarget, target);
            });
         } else {
            if (moving != null) {
               moving.accept(ChaserAINode.PathResult.FAILED, null);
            }
         }
      }
   }

   public enum ChaseDirection {
      TOWARDS,
      STAY,
      AWAY,
      INVALID;
   }

   protected enum PathResult {
      SUCCESS,
      MOVED_NO_SUCCESS,
      FAILED;
   }
}
