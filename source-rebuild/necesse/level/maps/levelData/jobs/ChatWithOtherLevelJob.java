package necesse.level.maps.levelData.jobs;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanMobChatterHandler;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobWorkerChatter;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.MobActiveJob;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;

public class ChatWithOtherLevelJob extends EntityLevelJob<Mob> {
   public JobWorkerChatterHandler targetHandler;

   public ChatWithOtherLevelJob(JobWorkerChatterHandler targetHandler) {
      super(targetHandler.getChatterMob());
      this.targetHandler = targetHandler;
   }

   @Override
   public String getRecreationExhaustionIdentifier() {
      return SettlerRecreationExhaustionManager.SOCIALIZING_IDENTIFIER;
   }

   @Override
   public boolean isValid() {
      return !super.isValid() ? false : !this.targetHandler.isInConversation();
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return 10;
   }

   public static <T extends ChatWithOtherLevelJob> JobSequence getJobSequence(HumanMob humanMob, FoundJob<T> foundJob) {
      GameMessage targetName = foundJob.job.targetHandler.chatter.getChatterSimpleName();
      GameMessage activityDescription = new LocalMessage("activities", "chatting", "target", targetName);
      return new SingleJobSequence(foundJob.job.getActiveJob(humanMob, foundJob.priority), activityDescription)
         .withPerformedLevelJob(foundJob.job)
         .markRecreationJob();
   }

   public ActiveJob getActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority) {
      return new ChatWithOtherLevelJob.ChatWithOtherActiveJob(humanMob, priority);
   }

   public static JobTypeHandler.JobStreamSupplier<? extends ChatWithOtherLevelJob> getJobStreamer() {
      return (worker, handler) -> {
         JobWorkerChatter chatter = (JobWorkerChatter)worker;
         ZoneTester restrictZone = worker.getJobRestrictZone();
         return JobWorkerChatterHandler.findChattersToInteractWith(chatter, 30, true, false, restrictZone).stream().map(ChatWithOtherLevelJob::new);
      };
   }

   public static JobTypeHandler.SubHandler<ChatWithOtherLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<ChatWithOtherLevelJob>setJobHandler(ChatWithOtherLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(() -> !humanMob.isOnStrike() && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement()))
            .setExtraJobStreamer(false, getJobStreamer());
      } else {
         return null;
      }
   }

   private class ChatWithOtherActiveJob extends MobActiveJob<Mob> {
      protected HumanMob humanMob;
      protected HumanMobChatterHandler chatterHandler;
      protected boolean hasArrivedAtTarget;

      public ChatWithOtherActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority) {
         super(humanMob, priority, ChatWithOtherLevelJob.this.targetHandler.getChatterMob());
         this.humanMob = humanMob;
         this.acceptAdjacentTiles = true;
         this.chatterHandler = new HumanMobChatterHandler(humanMob, true) {
            @Override
            public boolean canProgressConversation(JobWorkerChatterHandler other) {
               return ChatWithOtherActiveJob.this.hasArrivedAtTarget;
            }
         };
      }

      @Override
      public void onMadeCurrent() {
         super.onMadeCurrent();
         this.chatterHandler.startConversation(ChatWithOtherLevelJob.this.targetHandler);
      }

      @Override
      public int getCompleteRange() {
         return 48;
      }

      @Override
      public void tick(boolean isCurrent, boolean isMovingTo) {
         if (isCurrent && isMovingTo) {
            this.chatterHandler.serverTick();
         }
      }

      @Override
      public boolean isJobValid(boolean isCurrent) {
         return this.chatterHandler.isInConversation();
      }

      @Override
      public ActiveJobResult performTarget() {
         this.hasArrivedAtTarget = true;
         this.chatterHandler.serverTick();
         if (this.chatterHandler.isInConversation()) {
            this.worker.regenRecreation(50.0F, 0.5F, ChatWithOtherLevelJob.this.getRecreationExhaustionIdentifier());
            return ActiveJobResult.PERFORMING;
         } else {
            this.chatterHandler.invalidate();
            return ActiveJobResult.FINISHED;
         }
      }

      @Override
      public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
         super.onCancelled(becauseOfInvalid, isCurrent, isMovingTo);
         this.chatterHandler.invalidate();
      }
   }
}
