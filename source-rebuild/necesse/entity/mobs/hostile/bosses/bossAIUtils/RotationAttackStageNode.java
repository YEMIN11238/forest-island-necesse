package necesse.entity.mobs.hostile.bosses.bossAIUtils;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.CompositeAINode;

public class RotationAttackStageNode<T extends Mob> extends CompositeAINode<T> implements AttackStageInterface<T> {
   public int currentNode = -1;
   public boolean hasStarted;

   public RotationAttackStageNode(AINode<T>... nodes) {
      for (AINode<T> node : nodes) {
         this.addChild(node);
      }
   }

   @Override
   protected AINodeResult tickChildren(
      AINode<T> lastRunningChild, AINodeResult runningChildResult, Iterable<AINode<T>> children, T mob, Blackboard<T> blackboard
   ) {
      if (this.hasStarted && runningChildResult != null && runningChildResult != AINodeResult.RUNNING) {
         this.hasStarted = false;
         this.runningNode = null;
         return runningChildResult;
      }

      AINode<T> child = this.children.get(this.currentNode);
      AINodeResult result = child.lastResult = child.tick(mob, blackboard);
      if (result == AINodeResult.RUNNING) {
         this.runningNode = child;
      } else {
         this.runningNode = null;
      }

      return result;
   }

   @Override
   public void onStarted(T mob, Blackboard<T> blackboard) {
      this.currentNode = (this.currentNode + 1) % this.children.size();
      this.hasStarted = true;
      AINode<T> next = this.children.get(this.currentNode);
      if (next instanceof AttackStageInterface) {
         ((AttackStageInterface)next).onStarted(mob, blackboard);
      }
   }

   @Override
   public void onEnded(T mob, Blackboard<T> blackboard) {
      this.hasStarted = false;
      AINode<T> last = this.children.get(this.currentNode);
      if (last instanceof AttackStageInterface) {
         ((AttackStageInterface)last).onEnded(mob, blackboard);
      }
   }
}
