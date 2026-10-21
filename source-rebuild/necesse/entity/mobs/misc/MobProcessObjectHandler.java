package necesse.entity.mobs.misc;

import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.level.gameObject.ProcessObjectHandler;

public class MobProcessObjectHandler {
   public final Mob mob;
   public long startTime;
   public boolean isTargetOnTile;
   public int tileX;
   public int tileY;
   public int processTime;
   public ProcessObjectHandler target;

   public MobProcessObjectHandler(Mob mob) {
      this.mob = mob;
   }

   public boolean isInProgress() {
      return this.startTime != 0L;
   }

   public long getTimeSinceStart() {
      return this.mob.getTime() - this.startTime;
   }

   public float getProgressPercent() {
      return this.processTime != 0 ? GameUtils.getAnimFloat(this.getTimeSinceStart(), this.processTime) : 0.0F;
   }

   public void tick() {
      if (this.startTime > 0L) {
         long timeSinceStart = this.getTimeSinceStart();
         if (timeSinceStart <= this.processTime) {
            this.tickInProgress();
         } else {
            this.startTime = 0L;
            this.processTime = 0;
            this.onCompleted();
         }
      }
   }

   public void tickInProgress() {
   }

   public void onCompleted() {
   }
}
