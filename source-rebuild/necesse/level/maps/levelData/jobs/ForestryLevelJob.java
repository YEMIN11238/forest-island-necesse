package necesse.level.maps.levelData.jobs;

import java.util.function.Predicate;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.save.LoadData;
import necesse.entity.ObjectDamageResult;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.activeJob.MineObjectActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.level.gameObject.ForestryJobObject;
import necesse.level.gameObject.ForestrySaplingObject;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.LevelObject;
import necesse.level.maps.levelData.settlementData.zones.SettlementForestryZone;

public class ForestryLevelJob extends MineObjectLevelJob {
   public SettlementForestryZone zone;
   public boolean destroySapling;

   public ForestryLevelJob(int tileX, int tileY, SettlementForestryZone zone, boolean destroySapling) {
      super(tileX, tileY);
      this.zone = zone;
      this.destroySapling = destroySapling;
   }

   public ForestryLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public boolean isValid() {
      return !this.zone.isRemoved() && (this.destroySapling || this.zone.isChoppingAllowed()) ? super.isValid() : false;
   }

   @Override
   public boolean isValidObject(LevelObject object) {
      if (!this.zone.containsTile(this.tileX, this.tileY)) {
         return false;
      } else {
         return this.destroySapling && object.object instanceof ForestrySaplingObject && !this.zone.replantChoppedDownTrees()
            ? true
            : object.object instanceof ForestryJobObject;
      }
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   public static <T extends ForestryLevelJob> JobSequence getJobSequence(EntityJobWorker worker, final boolean useItem, final FoundJob<T> foundJob) {
      GameObject object = foundJob.job.getObject().object;
      GameMessage activityDescription = new LocalMessage("activities", "chopping", "target", object.getLocalization());
      final GameLinkedListJobSequence sequence = new GameLinkedListJobSequence(activityDescription, false);
      sequence.add(
         new MineObjectActiveJob(
            worker,
            foundJob.priority,
            foundJob.job.tileX,
            foundJob.job.tileY,
            lo -> !foundJob.job.isRemoved() && foundJob.job.isValidObject(lo),
            foundJob.job.reservable,
            "ironaxe",
            20,
            500,
            0
         ) {
            @Override
            public void onObjectDestroyed(ObjectDamageResult result) {
               this.addItemPickupJobs(foundJob.priority, result, sequence);
               PlantSaplingLevelJob newJob = foundJob.job.zone.getNewPlantJob(this.tileX, this.tileY, result.levelObject.object);
               if (newJob != null) {
                  int jobTypeID = LevelJobRegistry.getJobTypeID(PlantSaplingLevelJob.class);
                  newJob.reservable.reserve(this.worker.getMobWorker());
                  sequence.addLast(newJob.getActiveJob(this.worker, this.worker.getJobTypeHandler().getPriority(jobTypeID), useItem));
               }

               foundJob.job.remove();
            }
         }
      );
      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      return sequence;
   }

   public static JobTypeHandler.SubHandler<ForestryLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<ForestryLevelJob>setJobHandler(ForestryLevelJob.class, foundJob -> getJobSequence(humanMob, humanMob.isSettler(), foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike()
                  && !humanMob.hasCompletedMission()
                  && (!humanMob.isSettler() || humanMob.isSettlerWithinSettlement())
                  && !humanMob.isInventoryFull(true)
            );
      } else {
         return null;
      }
   }
}
