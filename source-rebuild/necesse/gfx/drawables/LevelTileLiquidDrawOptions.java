package necesse.gfx.drawables;

import java.util.HashMap;
import necesse.engine.util.GameMath;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

public class LevelTileLiquidDrawOptions {
   private final Level level;
   private final HashMap<Long, LevelTileLiquidRegionDrawOptions> regionMap = new HashMap<>();
   private long lastKey = Long.MIN_VALUE;
   private LevelTileLiquidRegionDrawOptions lastDrawOptions;

   public LevelTileLiquidDrawOptions(Level level) {
      this.level = level;
   }

   public LevelTileLiquidRegionDrawOptions getByTile(Level level, int tileX, int tileY) {
      int regionX = level.regionManager.getRegionCoordByTile(tileX);
      int regionY = level.regionManager.getRegionCoordByTile(tileY);
      long key = GameMath.getUniqueLongKey(regionX, regionY);
      synchronized (level.entityManager.lock) {
         if (this.lastKey == key) {
            return this.lastDrawOptions;
         }

         LevelTileLiquidRegionDrawOptions drawOptions = this.regionMap.get(key);
         if (drawOptions == null) {
            Region region = level.regionManager.getRegion(regionX, regionY, false);
            if (region != null) {
               drawOptions = new LevelTileLiquidRegionDrawOptions(region);
               this.regionMap.put(key, drawOptions);
            }
         }

         this.lastKey = key;
         this.lastDrawOptions = drawOptions;
         return drawOptions;
      }
   }

   public void draw() {
      synchronized (this.level.entityManager.lock) {
         for (LevelTileLiquidRegionDrawOptions drawOptions : this.regionMap.values()) {
            drawOptions.draw();
         }
      }
   }
}
