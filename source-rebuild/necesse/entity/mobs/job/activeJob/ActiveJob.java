package necesse.entity.mobs.job.activeJob;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.JobMoveToTile;

public abstract class ActiveJob {
   public final EntityJobWorker worker;
   public final JobTypeHandler.TypePriority priority;

   public ActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority) {
      this.worker = worker;
      this.priority = priority;
   }

   public void onMadeCurrent() {
   }

   public abstract JobMoveToTile getMoveToTile(JobMoveToTile var1);

   public abstract boolean isAt(JobMoveToTile var1);

   public void moveToPathAlreadyAtTarget(JobMoveToTile moveToTile) {
   }

   public abstract void tick(boolean var1, boolean var2);

   public abstract boolean isValid(boolean var1);

   public boolean shouldClearSequence() {
      return true;
   }

   public ActiveJobHitResult onHit(MobWasHitEvent event, boolean isMovingTo) {
      return ActiveJobHitResult.CLEAR_SEQUENCE;
   }

   public ActiveJobTargetFoundResult onTargetFound(Mob target, boolean isCurrent, boolean isMovingTo) {
      return this.worker.getJobFollowingMob() != null ? ActiveJobTargetFoundResult.FAIL : ActiveJobTargetFoundResult.CONTINUE;
   }

   public ActiveJobTargetFoundResult onFollowOutOfRange(Mob followingMob, boolean isCurrent, boolean isMovingTo) {
      return ActiveJobTargetFoundResult.FAIL;
   }

   public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
   }

   public void tickWhileBeingInteractedWith() {
   }

   public abstract ActiveJobResult perform();

   public Level getLevel() {
      return this.worker.getMobWorker().getLevel();
   }

   public JobWorkerChatterHandler getChatterHandler() {
      return null;
   }
}
