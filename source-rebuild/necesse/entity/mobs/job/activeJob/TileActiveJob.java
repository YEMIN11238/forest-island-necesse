package necesse.entity.mobs.job.activeJob;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Float;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.levelData.jobs.JobMoveToTile;
import necesse.level.maps.multiTile.MultiTile;

public abstract class TileActiveJob extends ActiveJob {
   public final int tileX;
   public final int tileY;

   public TileActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, int tileX, int tileY) {
      super(worker, priority);
      this.tileX = tileX;
      this.tileY = tileY;
   }

   @Override
   public boolean isAt(JobMoveToTile moveToTile) {
      Mob mob = this.worker.getMobWorker();
      if (moveToTile.acceptAdjacentTiles) {
         MultiTile multiTile = this.getLevel()
            .getObject(moveToTile.tileX, moveToTile.tileY)
            .getMultiTile(this.getLevel(), 0, moveToTile.tileX, moveToTile.tileY);
         if (multiTile.getAdjacentTileRectangle(moveToTile.tileX, moveToTile.tileY).contains(mob.getTileX(), mob.getTileY()) && !mob.hasCurrentMovement()) {
            return true;
         }
      }

      double dist = new Point(mob.getX(), mob.getY()).distance(moveToTile.tileX * 32 + 16, moveToTile.tileY * 32 + 16);
      return dist <= this.getCompleteRange() ? this.hasLOS() : false;
   }

   public int getCompleteRange() {
      return 48;
   }

   public boolean hasLOS() {
      Mob mob = this.worker.getMobWorker();
      Line2D line = new Float(mob.x, mob.y, this.tileX * 32 + 16, this.tileY * 32 + 16);
      return !mob.getLevel().collides(line, new CollisionFilter().mobCollision().addFilter(pos -> {
         Rectangle rect = pos.object().getMultiTile().getTileRectangle(pos.tileX, pos.tileY);
         return !rect.contains(pos.tileX, pos.tileY);
      }));
   }

   @Override
   public String toString() {
      return super.toString() + "{" + this.tileX + ", " + this.tileY + "}";
   }
}
