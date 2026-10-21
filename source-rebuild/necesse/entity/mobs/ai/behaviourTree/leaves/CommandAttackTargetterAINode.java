package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;

public abstract class CommandAttackTargetterAINode<T extends Mob> extends AINode<T> {
   public String currentTargetKey = "currentTarget";
   public Mob lastTarget;

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob target = this.getTarget(mob);

      try {
         if (this.lastTarget != null && this.lastTarget != target) {
            blackboard.put(this.currentTargetKey, null);
         }

         if (target != null) {
            if (target.removed() || !mob.isSamePlace(target)) {
               this.resetTarget(mob);
               if (blackboard.getObject(Mob.class, this.currentTargetKey) == target) {
                  blackboard.put(this.currentTargetKey, null);
               }

               return AINodeResult.FAILURE;
            }

            if (mob.estimateCanMoveTo(target.getTileX(), target.getTileY(), target.canBeTargetedFromAdjacentTiles())) {
               this.tickTargetSet(mob, target);
               if (target.canBeTargeted(mob, null)) {
                  blackboard.put(this.currentTargetKey, target);
                  return AINodeResult.SUCCESS;
               }

               this.resetTarget(mob);
            } else if (blackboard.getObject(Mob.class, this.currentTargetKey) == target) {
               blackboard.put(this.currentTargetKey, null);
            }
         }
      } finally {
         this.lastTarget = target;
      }

      return AINodeResult.FAILURE;
   }

   public abstract Mob getTarget(T var1);

   public abstract void resetTarget(T var1);

   public abstract void tickTargetSet(T var1, Mob var2);
}
