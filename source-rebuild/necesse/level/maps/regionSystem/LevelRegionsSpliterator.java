package necesse.level.maps.regionSystem;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.level.maps.Level;
import necesse.level.maps.LevelShapeBoundsSpliterator;

public class LevelRegionsSpliterator extends LevelShapeBoundsSpliterator<Point> {
   public LevelRegionsSpliterator(Level level, int minX, int minY, int maxX, int maxY, int extraRange) {
      super(level, minX, minY, maxX, maxY, extraRange);
   }

   @Override
   protected int getPosX(int x) {
      return this.level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(x));
   }

   @Override
   protected int getPosY(int y) {
      return this.level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(y));
   }

   @Override
   protected int getMinX() {
      return this.level.tileWidth > 0 ? 0 : Integer.MIN_VALUE;
   }

   @Override
   protected int getMinY() {
      return this.level.tileHeight > 0 ? 0 : Integer.MIN_VALUE;
   }

   @Override
   protected int getMaxX() {
      return this.level.tileWidth > 0 ? this.level.regionManager.getRegionCoordByTile(this.level.tileWidth - 1) : Integer.MAX_VALUE;
   }

   @Override
   protected int getMaxY() {
      return this.level.tileHeight > 0 ? this.level.regionManager.getRegionCoordByTile(this.level.tileHeight - 1) : Integer.MAX_VALUE;
   }

   protected Point getPos(int x, int y) {
      return new Point(x, y);
   }
}
