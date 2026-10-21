package necesse.entity.mobs.job;

import necesse.engine.util.ComputedValue;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public abstract class AbstractSubHandlerFoundJobGenerator<T extends AbstractLevelJob> extends FoundJobGenerator<T> {
   public final PreSequenceComputes preSequenceComputes;

   public AbstractSubHandlerFoundJobGenerator(T levelJob, PreSequenceComputes preSequenceComputes) {
      super(levelJob);
      this.preSequenceComputes = preSequenceComputes;
   }

   @Override
   public FoundJob<T> generateJob(EntityJobWorker worker) {
      JobTypeHandler.SubHandler<T> subHandler = this.getSubHandler(worker);
      if (subHandler == null) {
         throw new IllegalStateException(
            "Job with stringID \"" + this.levelJob.getStringID() + "\" does not have a handler registered in worker: " + worker.getClass().getSimpleName()
         );
      }

      if (!subHandler.canPerform(worker)) {
         return null;
      }

      ComputedValue<Object> preSequenceCompute = this.preSequenceComputes.getCompute(this.levelJob, () -> subHandler.getPreSequenceCompute(worker));
      return new HandlerFoundJob<>(worker, this.levelJob, subHandler, subHandler.priority, preSequenceCompute);
   }

   public abstract JobTypeHandler.SubHandler<T> getSubHandler(EntityJobWorker var1);
}
