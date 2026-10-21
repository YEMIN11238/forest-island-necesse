package necesse.entity.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.levelEvent.LevelEvent;
import necesse.level.maps.Level;

public class LevelEventRegionList<T extends LevelEvent> extends RegionTrackerList<T> {
   public LevelEventRegionList(Level level) {
      super(level);
   }

   @Override
   public Stream<T> streamInRegionsShape(int minX, int minY, int maxX, int maxY, int extraRegionRange) {
      return (Stream<T>)super.streamInRegionsShape(minX, minY, maxX, maxY, extraRegionRange).distinct();
   }

   @Override
   public GameAreaStream<T> streamAreaTileRange(int x, int y, int tileRange) {
      return (GameAreaStream<T>)super.streamAreaTileRange(x, y, tileRange).distinct();
   }

   @Override
   public ArrayList<T> getInRegionRange(int regionX, int regionY, int regionRange) {
      HashSet<T> set = new HashSet<>();

      for (int x = regionX - regionRange; x <= regionX + regionRange; x++) {
         if (this.level.regionManager.isRegionXWithinBounds(x)) {
            for (int y = regionY - regionRange; y <= regionY + regionRange; y++) {
               if (this.level.regionManager.isRegionYWithinBounds(y)) {
                  set.addAll(this.getInRegion(x, y));
               }
            }
         }
      }

      return new ArrayList<>(set);
   }
}
