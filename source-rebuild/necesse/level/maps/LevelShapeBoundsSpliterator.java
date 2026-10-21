package necesse.level.maps;

import necesse.engine.util.AreaSpliterator;

public abstract class LevelShapeBoundsSpliterator<T> extends AreaSpliterator<T> {
   public final Level level;

   public LevelShapeBoundsSpliterator(Level level, int minX, int minY, int maxX, int maxY, int extraRange) {
      this.level = level;
      int startX = Math.max(this.getMinX(), this.getPosX(minX) - extraRange);
      int startY = Math.max(this.getMinY(), this.getPosY(minY) - extraRange);
      int endX = Math.min(this.getMaxX(), this.getPosX(maxX) + extraRange);
      int endY = Math.min(this.getMaxY(), this.getPosY(maxY) + extraRange);
      this.reset(startX, startY, endX + 1, endY + 1);
   }

   protected abstract int getPosX(int var1);

   protected abstract int getPosY(int var1);

   protected int getMinX() {
      return 0;
   }

   protected int getMinY() {
      return 0;
   }

   protected abstract int getMaxX();

   protected abstract int getMaxY();
}
