package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanMobChatterHandler;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;

public class HumanChatterHandlerTargetAINode<T extends HumanMob> extends MoveTaskAINode<T> {
   protected HumanMobChatterHandler chatterHandler;
   protected boolean tickedNode;

   public JobWorkerChatterHandler getChatterHandler() {
      return this.tickedNode ? this.chatterHandler : null;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      this.chatterHandler = new HumanMobChatterHandler(mob, true);
   }

   public void init(T mob, Blackboard<T> blackboard) {
      this.tickedNode = false;
   }

   protected void onInterruptRunning(T mob, Blackboard<T> blackboard) {
      super.onInterruptRunning(mob, blackboard);
      this.tickedNode = false;
   }

   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (mob.isOnStrike()) {
         return AINodeResult.FAILURE;
      } else {
         this.tickedNode = true;
         this.chatterHandler.serverTick();
         if (this.chatterHandler.isInConversation()) {
            GameMessage targetName = this.chatterHandler.getCurrentlyInteractingWith().chatter.getChatterSimpleName();
            GameMessage activityDescription = new LocalMessage("activities", "chatting", "target", targetName);
            mob.setActivity("job", 10000, activityDescription);
            mob.regenRecreation(50.0F, 0.5F, SettlerRecreationExhaustionManager.SOCIALIZING_IDENTIFIER);
            return AINodeResult.RUNNING;
         } else {
            return AINodeResult.FAILURE;
         }
      }
   }
}
