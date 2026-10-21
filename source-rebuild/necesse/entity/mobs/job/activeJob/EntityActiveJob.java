package necesse.entity.mobs.job.activeJob;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.MovedRectangle;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.levelData.jobs.JobMoveToTile;

public abstract class EntityActiveJob<T extends Entity> extends ActiveJob {
   public final T target;
   public boolean acceptAdjacentTiles;
   public boolean forcedAtTarget;

   public EntityActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, T target) {
      super(worker, priority);
      this.target = target;
   }

   @Override
   public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
      Mob mob = this.worker.getMobWorker();
      int directMoveToDistance = this.getDirectMoveToDistance();
      return directMoveToDistance > 0 && new Point(mob.getX(), mob.getY()).distance(this.target.x, this.target.y) <= directMoveToDistance && this.hasMoveLOS()
         ? new JobMoveToTile(this.getDirectMoveTo(this.target))
         : new JobMoveToTile(this.target.getTileX(), this.target.getTileY(), this.acceptAdjacentTiles);
   }

   @Override
   public boolean isAt(JobMoveToTile moveToTile) {
      return this.isAtTarget();
   }

   public boolean isAtTarget() {
      return this.getDistanceToTarget() <= this.getCompleteRange() && this.hasLOS();
   }

   public int getCompleteRange() {
      return (int)(GameMath.diagonalDistance * 2.0 * 32.0);
   }

   public double getDistanceToTarget() {
      Mob mob = this.worker.getMobWorker();
      return new Point(mob.getX(), mob.getY()).distance(this.target.x, this.target.y);
   }

   public boolean hasLOS() {
      Mob mob = this.worker.getMobWorker();
      Line2D line = new Float(mob.x, mob.y, this.target.x, this.target.y);
      return !mob.getLevel().collides(line, this.modifyChasingCollisionFilter(mob, mob.getLevelCollisionFilter()));
   }

   public boolean hasMoveLOS() {
      Mob mob = this.worker.getMobWorker();
      return !mob.getLevel()
         .collides(new MovedRectangle(mob, this.target.getX(), this.target.getY()), this.modifyChasingCollisionFilter(mob, mob.getLevelCollisionFilter()));
   }

   protected CollisionFilter modifyChasingCollisionFilter(Mob mob, CollisionFilter collisionFilter) {
      return this.target instanceof Mob
         ? mob.modifyChasingCollisionFilter(collisionFilter, (Mob)this.target)
         : mob.modifyChasingCollisionFilter(collisionFilter, null);
   }

   public int getDirectMoveToDistance() {
      return 160;
   }

   public abstract MobMovement getDirectMoveTo(T var1);

   @Override
   public boolean isValid(boolean isCurrent) {
      if (this.target != null && (!this.isInvalidIfTargetRemoved(isCurrent) || !this.target.removed()) && this.target.isSamePlace(this.worker.getMobWorker())) {
         return !this.worker.getJobRestrictZone().containsTile(this.target.getTileX(), this.target.getTileY()) ? false : this.isJobValid(isCurrent);
      } else {
         return false;
      }
   }

   public abstract boolean isJobValid(boolean var1);

   public boolean isInvalidIfTargetRemoved(boolean isCurrent) {
      return true;
   }

   @Override
   public void moveToPathAlreadyAtTarget(JobMoveToTile moveToTile) {
      this.forcedAtTarget = true;
   }

   @Override
   public ActiveJobResult perform() {
      return !this.forcedAtTarget && !this.isAtTarget() ? ActiveJobResult.MOVE_TO : this.performTarget();
   }

   public abstract ActiveJobResult performTarget();

   @Override
   public String toString() {
      return super.toString() + "{" + this.target.getUniqueID() + ":" + this.target + "}";
   }
}
