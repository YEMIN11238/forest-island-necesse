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
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.SeedObject;
import necesse.level.maps.LevelObject;

public class HarvestCropLevelJob extends MineObjectLevelJob {
   public HarvestCropLevelJob(int tileX, int tileY) {
      super(tileX, tileY);
   }

   public HarvestCropLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public boolean isValidObject(LevelObject object) {
      return object.object.isSeed && ((SeedObject)object.object).isLastStage();
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   public static <T extends HarvestCropLevelJob> JobSequence getJobSequence(EntityJobWorker worker, final boolean useItem, final FoundJob<T> foundJob) {
      GameObject object = foundJob.job.getObject().object;
      GameMessage activityDescription = new LocalMessage("activities", "harvesting", "target", object.getLocalization());
      final GameLinkedListJobSequence sequence = new GameLinkedListJobSequence(activityDescription, false);
      sequence.add(
         new MineObjectActiveJob(
            worker, foundJob.priority, foundJob.job.tileX, foundJob.job.tileY, foundJob.job::isValidObject, foundJob.job.reservable, "sickle", 100, 500, 0
         ) {
            @Override
            public void onObjectDestroyed(ObjectDamageResult result) {
               this.addItemPickupJobs(foundJob.priority, result, sequence);
               if (result.levelObject.object.isSeed) {
                  int firstStageID = ((SeedObject)result.levelObject.object).stageIDs[0];
                  if (firstStageID != -1) {
                     PlantCropLevelJob plantJob = new PlantCropLevelJob(this.tileX, this.tileY, firstStageID);
                     plantJob.reservable.reserve(this.worker.getMobWorker());
                     if (this.getLevel().jobsLayer.addJob(plantJob) == plantJob) {
                        int jobTypeID = LevelJobRegistry.getJobTypeID(PlantCropLevelJob.class);
                        sequence.addLast(plantJob.getActiveJob(this.worker, this.worker.getJobTypeHandler().getPriority(jobTypeID), useItem));
                     }
                  }
               }

               foundJob.job.remove();
            }
         }
      );
      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      return sequence;
   }

   public static JobTypeHandler.SubHandler<HarvestCropLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<HarvestCropLevelJob>setJobHandler(HarvestCropLevelJob.class, foundJob -> getJobSequence(humanMob, humanMob.isSettler(), foundJob))
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
