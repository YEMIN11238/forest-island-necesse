package necesse.level.maps;

import necesse.engine.util.GameMath;

public class LevelTilesSpliterator extends LevelShapeBoundsSpliterator<TilePosition> {
   public LevelTilesSpliterator(Level level, int minX, int minY, int maxX, int maxY, int extraRange) {
      super(level, minX, minY, maxX, maxY, extraRange);
   }

   @Override
   protected int getPosX(int x) {
      return GameMath.getTileCoordinate(x);
   }

   @Override
   protected int getPosY(int y) {
      return GameMath.getTileCoordinate(y);
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
      return this.level.tileWidth > 0 ? this.level.tileWidth - 1 : Integer.MAX_VALUE;
   }

   @Override
   protected int getMaxY() {
      return this.level.tileHeight > 0 ? this.level.tileHeight - 1 : Integer.MAX_VALUE;
   }

   protected TilePosition getPos(int x, int y) {
      return new TilePosition(this.level, x, y);
   }
}
