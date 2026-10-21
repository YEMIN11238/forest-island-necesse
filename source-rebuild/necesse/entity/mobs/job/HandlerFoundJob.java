package necesse.entity.mobs.job;

import necesse.engine.util.ComputedValue;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public class HandlerFoundJob<T extends AbstractLevelJob> extends FoundJob<T> {
   public final JobTypeHandler.SubHandler<T> handler;

   public HandlerFoundJob(
      EntityJobWorker worker, T job, JobTypeHandler.SubHandler<T> handler, JobTypeHandler.TypePriority priority, ComputedValue<Object> preSequenceCompute
   ) {
      super(worker, job, priority, preSequenceCompute);
      this.handler = handler;
   }

   @Override
   public void startCooldown(long currentTime) {
      this.handler.startCooldown(currentTime);
   }

   @Override
   protected JobSequence getNewSequence() {
      return this.handler.sequenceFunction.apply(this);
   }
}
