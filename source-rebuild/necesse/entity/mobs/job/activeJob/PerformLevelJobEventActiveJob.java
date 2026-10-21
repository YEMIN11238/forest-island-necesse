package necesse.entity.mobs.job.activeJob;

import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public class PerformLevelJobEventActiveJob extends SimplePerformActiveJob {
   public AbstractLevelJob levelJob;

   public PerformLevelJobEventActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, AbstractLevelJob levelJob) {
      super(worker, priority);
      this.levelJob = levelJob;
   }

   public PerformLevelJobEventActiveJob(EntityJobWorker worker, FoundJob<?> foundJob) {
      this(worker, foundJob.priority, foundJob.job);
   }

   @Override
   public ActiveJobResult perform() {
      this.worker.onPerformedJob(this.levelJob, this.priority);
      return ActiveJobResult.FINISHED;
   }
}
