package necesse.level.maps.levelData.settlementData.zones;

import java.awt.Point;
import java.util.SortedSet;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.util.PointSortedSet;

public abstract class SettlementTileTickZone extends SettlementWorkZone {
   protected Point lastProcessingTile;
   protected float iterations;

   @Override
   public void tickSecond() {
      Performance.record(this.manager.data.getLevel().tickManager(), "forestryJobs", () -> {
         SortedSet<Point> subSet;
         if (this.lastProcessingTile == null) {
            subSet = new PointSortedSet(this.zoning.getTiles().getUnderlyingSet());
         } else {
            subSet = this.zoning.getTiles().tailSet(this.lastProcessingTile, false);
         }

         this.iterations = this.iterations + this.size() / 10.0F;

         for (Point tile : subSet) {
            if (this.iterations < 1.0F) {
               break;
            }

            this.iterations--;
            this.lastProcessingTile = tile;
            this.handleTile(tile);
         }

         if (this.iterations > 1.0F) {
            for (Point tile : this.zoning.getTiles()) {
               if (this.iterations < 1.0F) {
                  break;
               }

               this.iterations--;
               this.lastProcessingTile = tile;
               this.handleTile(tile);
            }
         }
      });
   }

   protected abstract void handleTile(Point var1);
}
