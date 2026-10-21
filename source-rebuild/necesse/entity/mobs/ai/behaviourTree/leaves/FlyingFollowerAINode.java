package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.itemAttacker.FollowerPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;

public abstract class FlyingFollowerAINode<T extends Mob> extends AINode<T> {
   private FollowerPosition targetPoint = null;
   public int teleportDistance;
   public int stoppingDistance;
   public int teleportToAccuracy = 1;

   public FlyingFollowerAINode(int teleportDistance, int stoppingDistance) {
      this.teleportDistance = teleportDistance;
      this.stoppingDistance = stoppingDistance;
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob target = this.getFollowingMob(mob);
      if (target != null && target.isSamePlace(mob)) {
         this.targetPoint = this.getTargetPoint(mob, target, this.targetPoint);
         FollowerPosition finalPoint = this.targetPoint != null ? this.targetPoint : new FollowerPosition(0, 0);
         float dist = mob.getDistance(target.x, target.y);
         float posDist = (float)new Point(finalPoint.x, finalPoint.y).distance(0.0, 0.0);
         float targetDist = mob.getDistance(target.x + finalPoint.x, target.y + finalPoint.y);
         if (this.teleportDistance > 0 && dist > this.teleportDistance && targetDist > this.teleportDistance + posDist) {
            this.teleportTo(mob, target, this.teleportToAccuracy);
         } else {
            blackboard.mover.setCustomMovement(this, finalPoint.movementGetter.apply(target));
            if (this.targetPoint == null && targetDist < this.stoppingDistance) {
               blackboard.mover.stopMoving(mob);
            }
         }

         return AINodeResult.SUCCESS;
      } else {
         return AINodeResult.FAILURE;
      }
   }

   public void teleportTo(T mob, Mob target, int maxTileRange) {
      FollowerAINode.teleportCloseTo(mob, target, maxTileRange);
      this.getBlackboard().mover.stopMoving(mob);
      mob.sendMovementPacket(true);
   }

   public FollowerPosition getTargetPoint(T mob, Mob target, FollowerPosition currentPos) {
      ItemAttackerMob followingAttacker = mob.getFollowingItemAttacker();
      return followingAttacker == null ? null : followingAttacker.serverFollowersManager.getFollowerPos(mob, target, currentPos);
   }

   public abstract Mob getFollowingMob(T var1);
}
