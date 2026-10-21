package necesse.entity.mobs.job.activeJob;

import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ObjectUserActive;
import necesse.entity.mobs.ObjectUserMob;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectUsersObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.JobMoveToTile;

public class UseObjectActiveJob extends TileActiveJob {
   protected int maxTicksRemaining;
   protected ObjectUserActive active;

   public UseObjectActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, int tileX, int tileY, int seconds) {
      super(worker, priority, tileX, tileY);
      this.maxTicksRemaining = seconds * 20;
   }

   @Override
   public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
      return new JobMoveToTile(this.tileX, this.tileY, true);
   }

   @Override
   public boolean isAt(JobMoveToTile moveToTile) {
      Mob mob = this.worker.getMobWorker();
      return TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), moveToTile.tileX, moveToTile.tileY, mob.getTileX(), mob.getTileY());
   }

   @Override
   public void tick(boolean isCurrent, boolean isMovingTo) {
   }

   @Override
   public boolean isValid(boolean isCurrent) {
      return this.active == null ? canUse(this.worker, this.tileX, this.tileY) != null : true;
   }

   public static OEUsers canUse(EntityJobWorker worker, int tileX, int tileY) {
      OEUsers oeUsers = getOEUsers(worker.getLevel(), tileX, tileY);
      if (oeUsers == null) {
         return null;
      } else {
         return oeUsers.getCanUseError(worker.getMobWorker()) == null ? oeUsers : null;
      }
   }

   public static OEUsers getOEUsers(Level level, int tileX, int tileY) {
      GameObject object = level.getObject(tileX, tileY);
      return object instanceof ObjectUsersObject ? ((ObjectUsersObject)object).getOEUsersObject(level, tileX, tileY) : null;
   }

   @Override
   public void tickWhileBeingInteractedWith() {
      super.tickWhileBeingInteractedWith();
      if (this.active != null) {
         this.active.keepUsing();
      }
   }

   @Override
   public ActiveJobResult perform() {
      if (this.active == null) {
         OEUsers oeUsers = canUse(this.worker, this.tileX, this.tileY);
         if (oeUsers == null) {
            return ActiveJobResult.FAILED;
         }

         Mob mob = this.worker.getMobWorker();
         oeUsers.startUser(mob);
         this.active = ((ObjectUserMob)mob).getUsingObject();
      } else {
         this.active.keepUsing();
      }

      this.maxTicksRemaining--;
      if (this.maxTicksRemaining <= 0 && this.canStopUsing()) {
         if (this.active != null) {
            this.active.stopUsing();
            this.active = null;
         }

         return ActiveJobResult.FINISHED;
      } else {
         return ActiveJobResult.PERFORMING;
      }
   }

   public boolean canStopUsing() {
      return true;
   }

   @Override
   public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
      super.onCancelled(becauseOfInvalid, isCurrent, isMovingTo);
      if (this.active != null) {
         this.active.stopUsing();
         this.active = null;
      }
   }
}
