package necesse.entity.mobs.job;

import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public class SubHandlerFoundJobGenerator<T extends AbstractLevelJob> extends AbstractSubHandlerFoundJobGenerator<T> {
   public final JobTypeHandler.SubHandler<T> subHandler;

   public SubHandlerFoundJobGenerator(T levelJob, JobTypeHandler.SubHandler<T> subHandler, PreSequenceComputes preSequenceComputes) {
      super(levelJob, preSequenceComputes);
      this.subHandler = subHandler;
   }

   @Override
   public JobTypeHandler.SubHandler<T> getSubHandler(EntityJobWorker worker) {
      return this.subHandler;
   }
}
