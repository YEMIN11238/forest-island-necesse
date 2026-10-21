package necesse.entity.mobs.job;

import necesse.level.maps.levelData.jobs.AbstractLevelJob;

@FunctionalInterface
public interface JobTypeHandlerGenerator<T extends AbstractLevelJob> {
   JobTypeHandler.SubHandler<T> registerHandler(EntityJobWorker var1, JobTypeHandler var2);

   default JobTypeHandlerGenerator<T> or(JobTypeHandlerGenerator<T> other) {
      return (worker, handler) -> {
         JobTypeHandler.SubHandler<T> prevHandler = this.registerHandler(worker, handler);
         return prevHandler != null ? prevHandler : other.registerHandler(worker, handler);
      };
   }
}
