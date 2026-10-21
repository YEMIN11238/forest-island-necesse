package necesse.entity.mobs.hostile.bosses.bossAIUtils;

import java.util.ArrayList;
import java.util.ListIterator;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.CompositeTypedAINode;

public class SimultaneousAttackStageManagerNode<T extends Mob> extends CompositeTypedAINode<T, AINode<T>> implements AttackStageInterface<T> {
   protected ArrayList<AINode<T>> runningChildren;
   public AINodeResult returnResult;

   public SimultaneousAttackStageManagerNode(AINodeResult returnResult) {
      this.returnResult = returnResult;
   }

   public SimultaneousAttackStageManagerNode() {
      this(AINodeResult.SUCCESS);
   }

   @Override
   protected AINodeResult tickChildren(
      AINode<T> lastRunningChild, AINodeResult runningChildResult, Iterable<AINode<T>> children, T mob, Blackboard<T> blackboard
   ) {
      if (this.runningChildren == null) {
         this.runningChildren = new ArrayList<>(this.children);

         for (AINode<T> child : this.runningChildren) {
            if (child instanceof AttackStageInterface) {
               ((AttackStageInterface)child).onStarted(mob, blackboard);
            }
         }
      }

      ListIterator<AINode<T>> iterator = this.runningChildren.listIterator();

      while (iterator.hasNext()) {
         AINode<T> child = iterator.next();
         AINodeResult result = child.lastResult = child.tick(mob, blackboard);
         if (result != AINodeResult.RUNNING) {
            iterator.remove();
            if (child instanceof AttackStageInterface) {
               ((AttackStageInterface)child).onEnded(mob, blackboard);
            }
         }
      }

      if (!this.runningChildren.isEmpty()) {
         return AINodeResult.RUNNING;
      }

      this.runningChildren = null;
      return this.returnResult;
   }

   @Override
   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.runningChildren = new ArrayList<>(this.children);

      for (AINode<T> child : this.runningChildren) {
         if (child instanceof AttackStageInterface) {
            ((AttackStageInterface)child).onStarted(mob, blackboard);
         }
      }
   }

   @Override
   public void onEnded(T mob, Blackboard<T> blackboard) {
      if (this.runningChildren != null) {
         for (AINode<T> child : this.runningChildren) {
            if (child instanceof AttackStageInterface) {
               ((AttackStageInterface)child).onEnded(mob, blackboard);
            }
         }

         this.runningChildren = null;
      }
   }
}
