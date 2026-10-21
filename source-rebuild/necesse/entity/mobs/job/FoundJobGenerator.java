package necesse.entity.mobs.job;

import java.util.function.Predicate;
import necesse.entity.Entity;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public abstract class FoundJobGenerator<T extends AbstractLevelJob> {
   public final T levelJob;

   public FoundJobGenerator(T levelJob) {
      this.levelJob = levelJob;
   }

   public boolean isAvailable(Entity worker) {
      return this.levelJob.reservable.isAvailable(worker);
   }

   public boolean isWithinRestrictZone(ZoneTester restrictZone) {
      return this.levelJob.isWithinRestrictZone(restrictZone);
   }

   public boolean testPredicate(Predicate<AbstractLevelJob> predicate) {
      return predicate == null || predicate.test(this.levelJob);
   }

   public abstract FoundJob<T> generateJob(EntityJobWorker var1);
}
