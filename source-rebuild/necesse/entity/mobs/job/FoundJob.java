package necesse.entity.mobs.job;

import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public abstract class FoundJob<T extends AbstractLevelJob> {
   public final EntityJobWorker worker;
   public final T job;
   public final JobTypeHandler.TypePriority priority;
   public final ComputedValue<Object> preSequenceCompute;
   private double workerDistance = -1.0;
   private boolean canMoveToComputed;
   private boolean canMoveTo;
   private boolean sequenceComputed;
   private JobSequence sequence;

   public FoundJob(EntityJobWorker worker, T job, JobTypeHandler.TypePriority priority, ComputedValue<Object> preSequenceCompute) {
      this.worker = worker;
      this.job = job;
      this.priority = priority;
      this.preSequenceCompute = preSequenceCompute;
   }

   public double getDistanceFromWorker() {
      if (this.workerDistance < 0.0) {
         Mob mob = this.worker.getMobWorker();
         Mob followingMob = this.worker.getJobFollowingMob();
         if (followingMob != null) {
            mob = followingMob;
         }

         this.workerDistance = GameMath.diagonalMoveDistance(mob.getX(), mob.getY(), this.job.getTileX() * 32 + 16, this.job.getTileY() * 32 + 16);
         int groupByTiles = this.job.groupDistanceToJobByTiles(this.worker);
         if (groupByTiles > 0) {
            int tileDistance = groupByTiles * 32;
            this.workerDistance = Math.ceil(this.workerDistance / tileDistance) * tileDistance;
         }
      }

      return this.workerDistance;
   }

   public boolean canMoveTo() {
      if (!this.canMoveToComputed) {
         this.canMoveTo = this.job.estimateCanMoveTo(this.worker);
         this.canMoveToComputed = true;
      }

      return this.canMoveTo;
   }

   public boolean isDisabled() {
      return this.priority == null ? false : this.priority.disabledBySettler || this.priority.disabledByPlayer;
   }

   public abstract void startCooldown(long var1);

   protected abstract JobSequence getNewSequence();

   public JobSequence getSequence() {
      if (!this.sequenceComputed) {
         Mob mob = this.worker.getMobWorker();
         Performance.record(mob.getLevel().tickManager(), "getSequence", () -> Performance.record(mob.getLevel().tickManager(), this.job.getStringID(), () -> {
            this.sequence = this.getNewSequence();
            this.sequenceComputed = true;
         }));
      }

      return this.sequence;
   }
}
