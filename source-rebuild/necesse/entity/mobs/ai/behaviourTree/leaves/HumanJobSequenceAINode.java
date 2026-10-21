package necesse.entity.mobs.ai.behaviourTree.leaves;

import necesse.engine.GameTileRange;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.entity.mobs.job.activeJob.ActiveJob;

public class HumanJobSequenceAINode<T extends HumanMob> extends SequenceAINode<T> {
   public static GameTileRange followingJobTileRange = new GameTileRange(12);
   public static GameTileRange followingSearchJobTileRange = new GameTileRange(followingJobTileRange.maxRange - 4);
   public final HumanJobSearchingAINode<T> humanJobSearchingAINode;
   public final HumanJobMoveToAINode<T> humanJobMoveToAINode;
   public final HumanDoJobAINode<T> humanDoJobAINode;

   public HumanJobSequenceAINode(boolean isBeforeFollowAI) {
      this.addChild(this.humanJobSearchingAINode = new HumanJobSearchingAINode<>(isBeforeFollowAI ? followingSearchJobTileRange : null));
      if (isBeforeFollowAI) {
         this.humanJobSearchingAINode.minSearchCooldownSeconds = 3;
         this.humanJobSearchingAINode.maxSearchCooldownSeconds = 6;
      }

      this.addChild(this.humanJobMoveToAINode = new HumanJobMoveToAINode<>(isBeforeFollowAI ? followingJobTileRange : null));
      this.addChild(this.humanDoJobAINode = new HumanDoJobAINode<>(isBeforeFollowAI ? followingJobTileRange : null));
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return super.tick(mob, blackboard);
   }

   public JobWorkerChatterHandler getChatterHandler() {
      ActiveJob currentJob = this.getBlackboard().getObject(ActiveJob.class, "currentJob");
      return currentJob == null ? null : currentJob.getChatterHandler();
   }
}
