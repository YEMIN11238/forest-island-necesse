package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.geom.Point2D.Float;
import java.util.function.BiPredicate;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.ConfuseWanderAIEvent;
import necesse.entity.mobs.ai.behaviourTree.util.MoveToTileAITask;
import necesse.gfx.gameTooltips.ListGameTooltips;

public abstract class CollisionChaserAINode<T extends Mob> extends MoveTaskAINode<T> {
   public BiPredicate<Mob, MoveToTileAITask.AIPathResult> moveIfFailedPath;
   public String targetKey;
   public long nextPathFindTime;
   public boolean pathIfStopped;
   public int directSearchDistance;
   public int stoppingDistance;
   public boolean keepMovingDirectTargetIfLost = true;
   public MobHitCooldowns hitCooldowns = new MobHitCooldowns();
   public String chaserTargetKey = "chaserTarget";
   public int attackMoveCooldown = 1000;
   public float attacksCausesConfusionChance = 0.5F;
   public int minPathSuccessWaitTime = 500;
   public int minPathFailedWaitTime = 5000;
   public int maxPathFailedWaitTime = 8000;
   protected long moveCooldownTimer;
   protected boolean movingToTarget = false;

   public CollisionChaserAINode(String targetKey) {
      this.targetKey = targetKey;
   }

   public CollisionChaserAINode() {
      this("currentTarget");
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      if (this.directSearchDistance == 0) {
         int pathMoveDistance = (int)mob.getPathMoveOffset().distance(0.0, 0.0);
         this.directSearchDistance = pathMoveDistance * 8 + 32;
      }

      blackboard.onEvent("resetPathTime", e -> this.nextPathFindTime = 0L);
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

   public static boolean simpleAttack(Mob mob, Mob target, GameDamage damage, int knockback) {
      if (damage != null && target.canBeHit(mob)) {
         Mob from = mob;
         Mob followingMob = mob.getFollowingMob();
         if (followingMob != null) {
            from = followingMob;
         }

         target.isServerHit(damage, target.x - from.x, target.y - from.y, knockback, mob);
         return true;
      } else {
         return false;
      }
   }

   public abstract boolean attackTarget(T var1, Mob var2);

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.targetKey);
      if (target != null && target.getCollision().intersects(mob.getCollision()) && this.hitCooldowns.canHit(target) && mob.canAttack()) {
         if (this.attacksCausesConfusionChance > 0.0F && GameRandom.globalRandom.getChance(this.attacksCausesConfusionChance)) {
            blackboard.mover.stopMoving(mob);
            Float hitDir = GameMath.normalize(target.x - mob.x, target.y - mob.y);
            Float perpDir = GameMath.getPerpendicularDir(hitDir);
            float perpAngle = GameMath.getAngle(perpDir);
            float nextAngle = GameMath.fixAngle(perpAngle + GameRandom.globalRandom.getIntBetween(-45, 45));
            Float finalDir = GameMath.getAngleDir(nextAngle);
            if (GameRandom.globalRandom.nextBoolean()) {
               finalDir = new Float(-finalDir.x, -finalDir.y);
            }

            int confusionTime = GameRandom.globalRandom.getIntBetween(500, 1000);
            ConfuseWanderAIEvent event = new ConfuseWanderAIEvent(confusionTime, finalDir);
            blackboard.submitEvent("confuseWander", event);
         }

         if (this.attackMoveCooldown > 0) {
            blackboard.mover.stopMoving(mob);
            this.moveCooldownTimer = mob.getTime() + this.attackMoveCooldown;
         }

         if (this.attackTarget(mob, target)) {
            this.hitCooldowns.startCooldown(target);
         }
      }

      return super.tick(mob, blackboard);
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.targetKey);
      boolean isOnMoveCooldown = this.moveCooldownTimer >= mob.getTime();
      if (target != null && !isOnMoveCooldown) {
         float dist = mob.getDistance(target);
         if ((dist < this.directSearchDistance || dist < this.stoppingDistance)
            && !mob.getLevel()
               .collides(new MovedRectangle(mob, target.getX(), target.getY()), mob.modifyChasingCollisionFilter(mob.getLevelCollisionFilter(), target))) {
            if (dist < this.stoppingDistance) {
               blackboard.mover.stopMoving(mob);
            } else {
               blackboard.mover.setMobTarget(this, target, true);
            }

            this.nextPathFindTime = 0L;
            this.movingToTarget = true;
         } else if (this.nextPathFindTime <= mob.getLocalTime() || this.pathIfStopped && (mob.hasArrivedAtTarget() || !blackboard.mover.isMoving())) {
            this.pathIfStopped = false;
            return this.moveToTileTask(target.getTileX(), target.getTileY(), null, path -> {
               if (path.moveIfWithin(-1, this.moveIfFailedPath != null && this.moveIfFailedPath.test(mob, path) ? -1 : 1, () -> {
                  this.movingToTarget = false;
                  this.nextPathFindTime = 0L;
               })) {
                  if (path.isResultWithin(1)) {
                     int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, this.minPathSuccessWaitTime, 0.1F);
                     this.nextPathFindTime = mob.getLocalTime() + nextPathTimeAdd;
                     this.pathIfStopped = true;
                  } else {
                     int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, this.minPathFailedWaitTime, 0.1F);
                     this.nextPathFindTime = mob.getLocalTime() + nextPathTimeAdd;
                  }

                  if (path.result != null && path.result.iterations == 0) {
                     if (dist < this.stoppingDistance) {
                        blackboard.mover.stopMoving(mob);
                     } else {
                        blackboard.mover.setMobTarget(this, target, true);
                     }
                  }

                  this.movingToTarget = true;
               } else {
                  this.movingToTarget = false;
                  this.nextPathFindTime = mob.getLocalTime() + GameRandom.globalRandom.getIntBetween(this.minPathFailedWaitTime, this.maxPathFailedWaitTime);
               }

               return this.lastTick(this.movingToTarget, this.moveCooldownTimer >= mob.getTime(), target);
            });
         }
      } else {
         this.movingToTarget = false;
      }

      return this.lastTick(this.movingToTarget, isOnMoveCooldown, target);
   }

   protected AINodeResult lastTick(boolean movingToTarget, boolean isOnMoveCooldown, Mob target) {
      if (movingToTarget) {
         this.getBlackboard().put(this.chaserTargetKey, target);
         return AINodeResult.SUCCESS;
      } else {
         return isOnMoveCooldown ? AINodeResult.SUCCESS : AINodeResult.FAILURE;
      }
   }

   @Override
   public void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      tooltips.add("movingToTarget: " + this.movingToTarget);
      tooltips.add("nextPathFindTime: " + (this.nextPathFindTime - this.mob().getLocalTime()));
      tooltips.add("pathIfStopped: " + this.pathIfStopped);
   }
}
