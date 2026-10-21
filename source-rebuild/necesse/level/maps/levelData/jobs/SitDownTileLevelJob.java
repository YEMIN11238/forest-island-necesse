package necesse.level.maps.levelData.jobs;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ObjectUserActive;
import necesse.entity.mobs.ObjectUserMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanMobChatterHandler;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.UseObjectActiveJob;
import necesse.level.gameObject.ChairObjectInterface;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;

public class SitDownTileLevelJob extends TileLevelJob {
   public int objectID;

   public SitDownTileLevelJob(int tileX, int tileY, GameObject object) {
      super(tileX, tileY);
      this.objectID = object.getID();
   }

   public SitDownTileLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public String getRecreationExhaustionIdentifier() {
      return SettlerRecreationExhaustionManager.RELAXING_IDENTIFIER;
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public boolean isValid() {
      return !super.isValid() ? false : this.getLevel().getObjectID(this.tileX, this.tileY) == this.objectID;
   }

   @Override
   public int groupDistanceToJobByTiles(EntityJobWorker worker) {
      return worker.getJobFollowingMob() != null ? 0 : 10;
   }

   @Override
   public boolean canDoWhileFollowing(Mob followingMob) {
      if (followingMob instanceof ObjectUserMob) {
         ObjectUserMob objectUser = (ObjectUserMob)followingMob;
         ObjectUserActive active = objectUser.getUsingObject();
         if (active != null && active.object instanceof ChairObjectInterface) {
            return followingMob.getLevel().getRoomID(this.tileX, this.tileY) == followingMob.getLevel().getRoomID(active.tileX, active.tileY);
         }
      }

      return false;
   }

   public static <T extends SitDownTileLevelJob> JobSequence getJobSequence(HumanMob humanMob, FoundJob<T> foundJob) {
      if (UseObjectActiveJob.canUse(humanMob, foundJob.job.tileX, foundJob.job.tileY) == null) {
         return null;
      }

      GameMessage targetName = ObjectRegistry.getObject(foundJob.job.objectID).getLocalization();
      GameMessage activityDescription = new LocalMessage("activities", "relaxing", "target", targetName);
      return new SingleJobSequence(foundJob.job.getActiveJob(humanMob, foundJob.priority), activityDescription)
         .withPerformedLevelJob(foundJob.job)
         .markRecreationJob();
   }

   public ActiveJob getActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority) {
      return new SitDownTileLevelJob.SitDownActiveJob(humanMob, priority, this.tileX, this.tileY);
   }

   public static JobTypeHandler.SubHandler<SitDownTileLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<SitDownTileLevelJob>setJobHandler(SitDownTileLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike() && (!humanMob.isSettler() || humanMob.getJobFollowingMob() != null || humanMob.isSettlerWithinSettlement())
            );
      } else {
         return null;
      }
   }

   private class SitDownActiveJob extends UseObjectActiveJob {
      protected HumanMob humanMob;
      protected Mob followingMob;
      protected int performingValidCheckTicker = 0;
      protected HumanMobChatterHandler chatterHandler;

      public SitDownActiveJob(HumanMob humanMob, JobTypeHandler.TypePriority priority, int tileX, int tileY) {
         super(humanMob, priority, tileX, tileY, GameRandom.globalRandom.getIntBetween(20, 30));
         this.humanMob = humanMob;
         this.followingMob = humanMob.getJobFollowingMob();
         this.chatterHandler = new HumanMobChatterHandler(humanMob, false);
      }

      @Override
      public boolean isValid(boolean isCurrent) {
         if (SitDownTileLevelJob.this.isRemoved()) {
            return false;
         } else if (!SitDownTileLevelJob.this.isValid()) {
            return false;
         } else if (!super.isValid(isCurrent)) {
            return false;
         } else if (this.followingMob != null && !isCurrent) {
            Mob newFollowingMob = this.humanMob.getJobFollowingMob();
            return newFollowingMob != this.followingMob ? false : SitDownTileLevelJob.this.canDoWhileFollowing(this.followingMob);
         } else {
            return true;
         }
      }

      @Override
      public ActiveJobResult perform() {
         ActiveJobResult out = super.perform();
         if (this.followingMob != null) {
            this.performingValidCheckTicker++;
            if (this.performingValidCheckTicker >= 40) {
               this.performingValidCheckTicker = 0;
               Mob newFollowingMob = this.humanMob.getJobFollowingMob();
               if (newFollowingMob != this.followingMob || !SitDownTileLevelJob.this.canDoWhileFollowing(this.followingMob)) {
                  if (this.chatterHandler != null) {
                     this.chatterHandler.invalidate();
                  }

                  return ActiveJobResult.FINISHED;
               }
            }
         }

         if (this.chatterHandler != null && !this.worker.isOnStrike()) {
            this.chatterHandler.serverTick();
            if (this.chatterHandler.isInConversation()) {
               out = ActiveJobResult.PERFORMING;
            } else if (out != ActiveJobResult.FINISHED) {
               JobWorkerChatterHandler foundChatter = this.chatterHandler.lookForOtherToChatWith(4, false, true, false);
               if (foundChatter != null) {
                  out = ActiveJobResult.PERFORMING;
               }
            }

            if (out == ActiveJobResult.FINISHED) {
               this.chatterHandler.invalidate();
            }
         }

         if (out == ActiveJobResult.PERFORMING) {
            this.worker.regenRecreation(50.0F, 0.5F, SitDownTileLevelJob.this.getRecreationExhaustionIdentifier());
         }

         return out;
      }

      @Override
      public boolean canStopUsing() {
         return this.chatterHandler == null || !this.chatterHandler.isInConversation();
      }

      @Override
      public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
         super.onCancelled(becauseOfInvalid, isCurrent, isMovingTo);
         this.chatterHandler.invalidate();
      }

      @Override
      public JobWorkerChatterHandler getChatterHandler() {
         return this.chatterHandler;
      }
   }
}
