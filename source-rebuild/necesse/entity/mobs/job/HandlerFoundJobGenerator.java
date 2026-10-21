package necesse.entity.mobs.job;

import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public class HandlerFoundJobGenerator<T extends AbstractLevelJob> extends AbstractSubHandlerFoundJobGenerator<T> {
   public final JobTypeHandler handler;

   public HandlerFoundJobGenerator(T levelJob, JobTypeHandler handler, PreSequenceComputes preSequenceComputes) {
      super(levelJob, preSequenceComputes);
      this.handler = handler;
   }

   @Override
   public JobTypeHandler.SubHandler<T> getSubHandler(EntityJobWorker worker) {
      return (JobTypeHandler.SubHandler<T>)this.handler.getJobHandler(this.levelJob.getID());
   }
}
