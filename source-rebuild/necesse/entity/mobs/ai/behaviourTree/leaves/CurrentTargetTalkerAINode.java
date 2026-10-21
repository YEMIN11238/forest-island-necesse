package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;

public abstract class CurrentTargetTalkerAINode<T extends Mob> extends AINode<T> {
   public String chaserTargetKey;
   public boolean onlyTalkOnce;
   public int talkCooldown;
   public Mob lastTarget;
   public long lastTalkTime;

   public CurrentTargetTalkerAINode(String chaserTargetKey, boolean onlyTalkOnce, int talkCooldown) {
      this.chaserTargetKey = chaserTargetKey;
      this.onlyTalkOnce = onlyTalkOnce;
      this.talkCooldown = talkCooldown;
   }

   public CurrentTargetTalkerAINode(boolean onlyTalkOnce, int talkCooldown) {
      this("chaserTarget", onlyTalkOnce, talkCooldown);
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   @Override
   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      Mob target = blackboard.getObject(Mob.class, this.chaserTargetKey);

      try {
         if (target != null) {
            if (this.onlyTalkOnce) {
               if (target != this.lastTarget && this.lastTalkTime + this.talkCooldown < mob.getWorldEntity().getTime()) {
                  this.talk(mob, target);
                  this.lastTalkTime = mob.getWorldEntity().getTime();
                  return AINodeResult.SUCCESS;
               }
            } else if (this.lastTalkTime + this.talkCooldown < mob.getWorldEntity().getTime()) {
               this.talk(mob, target);
               this.lastTalkTime = mob.getWorldEntity().getTime();
               return AINodeResult.SUCCESS;
            }
         }

         return AINodeResult.FAILURE;
      } finally {
         this.lastTarget = target;
      }
   }

   public abstract void talk(T var1, Mob var2);
}
