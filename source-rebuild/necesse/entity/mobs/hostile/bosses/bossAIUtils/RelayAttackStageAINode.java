package necesse.entity.mobs.hostile.bosses.bossAIUtils;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.DecoratorAINode;

public abstract class RelayAttackStageAINode<T extends Mob> extends DecoratorAINode<T> implements AttackStageInterface<T> {
   private AttackStageInterface<T> currentStageInterface;

   public RelayAttackStageAINode() {
      super(null);
   }

   public abstract AINode<T> getNextNode();

   @Override
   protected void setChild(AINode<T> child) {
      super.setChild(child);
      if (child instanceof AttackStageInterface) {
         this.currentStageInterface = (AttackStageInterface<T>)child;
      } else {
         this.currentStageInterface = null;
      }
   }

   @Override
   public AINodeResult tickChild(AINode<T> child, T mob, Blackboard<T> blackboard) {
      return child == null ? AINodeResult.SUCCESS : child.tick(mob, blackboard);
   }

   @Override
   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.setChild(this.getNextNode());
      if (this.currentStageInterface != null) {
         this.currentStageInterface.onStarted(mob, blackboard);
      }
   }

   @Override
   public void onEnded(T mob, Blackboard<T> blackboard) {
      if (this.currentStageInterface != null) {
         this.currentStageInterface.onEnded(mob, blackboard);
      }
   }
}
