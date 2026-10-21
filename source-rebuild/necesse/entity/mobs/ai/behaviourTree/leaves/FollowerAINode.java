package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameRandom;
import necesse.engine.util.MovedRectangle;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.itemAttacker.FollowerPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.gfx.gameTooltips.ListGameTooltips;

public abstract class FollowerAINode<T extends Mob> extends MoveTaskAINode<T> {
   private FollowerPosition targetPoint = null;
   private int attachedStage;
   private long nextPathFindTime;
   public int teleportDistance;
   public int stoppingDistance;
   public int directSearchDistance;
   public int teleportToAccuracy = 1;

   public FollowerAINode(int teleportDistance, int stoppingDistance) {
      this.teleportDistance = teleportDistance;
      this.stoppingDistance = stoppingDistance;
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      if (this.directSearchDistance == 0) {
         int pathMoveDistance = (int)mob.getPathMoveOffset().distance(0.0, 0.0);
         this.directSearchDistance = pathMoveDistance * 8 + 32;
      }

      blackboard.onWasHit(e -> this.nextPathFindTime = 0L);
      blackboard.onEvent("resetPathTime", e -> this.nextPathFindTime = 0L);
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      Mob target = this.getFollowingMob(mob);
      if (target != null && target.isSamePlace(mob)) {
         ItemAttackerMob followingAttacker = mob.getFollowingItemAttacker();
         this.targetPoint = followingAttacker == null ? null : followingAttacker.serverFollowersManager.getFollowerPos(mob, target, this.targetPoint);
         FollowerPosition finalPoint = this.targetPoint != null ? this.targetPoint : new FollowerPosition(0, 0);
         float dist = mob.getDistance(target.x, target.y);
         float posDist = (float)new Point(finalPoint.x, finalPoint.y).distance(0.0, 0.0);
         float targetDist = mob.getDistance(target.x + finalPoint.x, target.y + finalPoint.y);
         if (this.teleportDistance > 0 && dist > this.teleportDistance && targetDist > this.teleportDistance + posDist) {
            this.teleportTo(mob, target, this.teleportToAccuracy);
            this.attachedStage = 0;
         } else {
            MovedRectangle col = new MovedRectangle(mob, (int)(target.x + finalPoint.x), (int)(target.y + finalPoint.y));
            ComputedValue<Boolean> isTargetPathClear = new ComputedValue<>(() -> !mob.getLevel().collides(col, mob.getLevelCollisionFilter()));
            if (this.attachedStage == 0) {
               if (isTargetPathClear.get()) {
                  this.attachedStage = 2;
               } else if (this.nextPathFindTime <= mob.getWorldEntity().getLocalTime()) {
                  this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + 1000L;
                  return this.moveToTileTask(target.getTileX(), target.getTileY(), null, path -> {
                     if (path.moveIfWithin(-1, -1, () -> this.nextPathFindTime = 0L)) {
                        int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 1000, 0.1F);
                        this.nextPathFindTime = mob.getWorldEntity().getLocalTime() + nextPathTimeAdd;
                     }

                     return AINodeResult.SUCCESS;
                  });
               }
            } else if (this.attachedStage == 1) {
               if (targetDist > posDist + Math.max(this.stoppingDistance, this.directSearchDistance)) {
                  this.attachedStage = 0;
               } else if ((targetDist < this.stoppingDistance || targetDist < this.directSearchDistance) && isTargetPathClear.get()) {
                  this.attachedStage = 2;
               } else {
                  MobMovement mobMovement = finalPoint.movementGetter.apply(target);
                  blackboard.mover.setCustomMovement(this, mobMovement);
               }
            } else {
               blackboard.mover.setCustomMovement(this, finalPoint.movementGetter.apply(target));
               if (this.targetPoint == null && targetDist < this.stoppingDistance) {
                  blackboard.mover.stopMoving(mob);
               }

               if (!isTargetPathClear.get()) {
                  this.attachedStage = 0;
               }
            }
         }

         return AINodeResult.SUCCESS;
      } else {
         if (blackboard.mover.isCurrentlyMovingFor(this)) {
            blackboard.mover.stopMoving(mob);
         }

         return AINodeResult.FAILURE;
      }
   }

   public void teleportTo(T mob, Mob target, int maxTileRange) {
      teleportCloseTo(mob, target, maxTileRange);
      this.getBlackboard().mover.stopMoving(mob);
      mob.sendMovementPacket(true);
      this.nextPathFindTime = 0L;
   }

   public abstract Mob getFollowingMob(T var1);

   public static void teleportCloseTo(Mob mob, Mob target, int maxTileRange) {
      Point finalPos = getTeleportCloseToPos(mob, target, maxTileRange);
      mob.setPos(finalPos.x, finalPos.y, true);
   }

   public static Point getTeleportCloseToPos(Mob mob, Mob target, int maxTileRange) {
      int tileX = target.getTileX();
      int tileY = target.getTileY();
      Point pathOffset = mob.getPathMoveOffset();
      ArrayList<Point> possibleSpawns = new ArrayList<>();

      for (int x = tileX - maxTileRange; x <= tileX + maxTileRange; x++) {
         for (int y = tileY - maxTileRange; y <= tileY + maxTileRange; y++) {
            if (x != tileX || y != tileY) {
               int posX = x * 32 + pathOffset.x;
               int posY = y * 32 + pathOffset.y;
               if (!mob.collidesWith(mob.getLevel(), posX, posY)) {
                  possibleSpawns.add(new Point(posX, posY));
               }
            }
         }
      }

      Point finalPoint = GameRandom.globalRandom.getOneOf(possibleSpawns);
      if (finalPoint == null) {
         finalPoint = new Point(target.getX(), target.getY());
      }

      return finalPoint;
   }

   @Override
   public void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      tooltips.add("attachedStage: " + this.attachedStage);
      tooltips.add("nextPathFindTime: " + (this.nextPathFindTime - this.mob().getWorldEntity().getLocalTime()));
   }
}
