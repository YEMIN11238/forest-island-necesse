package necesse.level.maps.regionSystem;

import java.awt.Point;
import java.util.LinkedList;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import necesse.engine.util.GameMath;

public abstract class RegionBoundsExecutorAbstract<T> {
   public final int startTileX;
   public final int startTileY;
   public final int endTileX;
   public final int endTileY;
   protected final int regionSizeBits;
   public final int regionStartX;
   public final int regionStartY;
   public final int regionEndX;
   public final int regionEndY;
   private LinkedList<RegionBoundsExecutorAbstract.RegionBounds<T>> regionsList;
   private RegionBoundsExecutorAbstract.RegionBounds<T>[][] regionsArray;

   public RegionBoundsExecutorAbstract(int startTileX, int startTileY, int endTileX, int endTileY, int regionSizeBits) {
      this.startTileX = startTileX;
      this.startTileY = startTileY;
      this.endTileX = endTileX;
      this.endTileY = endTileY;
      this.regionStartX = GameMath.divideByPowerOf2RoundedDown(startTileX, regionSizeBits);
      this.regionStartY = GameMath.divideByPowerOf2RoundedDown(startTileY, regionSizeBits);
      this.regionEndX = GameMath.divideByPowerOf2RoundedDown(endTileX, regionSizeBits);
      this.regionEndY = GameMath.divideByPowerOf2RoundedDown(endTileY, regionSizeBits);
      this.regionSizeBits = regionSizeBits;
   }

   protected abstract RegionBoundsExecutorAbstract.RegionBounds<T> getRegionBounds(int var1, int var2);

   protected abstract int getRegionTileXOffset(T var1);

   protected abstract int getRegionTileYOffset(T var1);

   private void calculateListIfNecessary() {
      if (this.regionsList == null) {
         this.regionsList = new LinkedList<>();
         if (this.regionsArray != null) {
            for (RegionBoundsExecutorAbstract.RegionBounds<T>[] innerArray : this.regionsArray) {
               for (RegionBoundsExecutorAbstract.RegionBounds<T> bounds : innerArray) {
                  if (bounds != null) {
                     this.regionsList.add(bounds);
                  }
               }
            }
         } else {
            for (int x = this.regionStartX; x <= this.regionEndX; x++) {
               for (int y = this.regionStartY; y <= this.regionEndY; y++) {
                  RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.getRegionBounds(x, y);
                  if (bounds != null) {
                     this.regionsList.add(bounds);
                  }
               }
            }
         }
      }
   }

   private void calculateArrayIfNecessary() {
      if (this.regionsArray == null) {
         this.regionsArray = new RegionBoundsExecutorAbstract.RegionBounds[this.regionEndX - this.regionStartX + 1][this.regionEndY - this.regionStartY + 1];
         if (this.regionsList != null) {
            for (RegionBoundsExecutorAbstract.RegionBounds<T> bounds : this.regionsList) {
               this.regionsArray[bounds.regionX - this.regionStartX][bounds.regionY - this.regionStartY] = bounds;
            }
         } else {
            for (int x = this.regionStartX; x <= this.regionEndX; x++) {
               for (int y = this.regionStartY; y <= this.regionEndY; y++) {
                  RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.getRegionBounds(x, y);
                  if (bounds != null) {
                     this.regionsArray[x - this.regionStartX][y - this.regionStartY] = bounds;
                  }
               }
            }
         }
      }
   }

   public void runBounds(RegionBoundsExecutorAbstract.BoundsExecutor<T> executor) {
      this.calculateListIfNecessary();

      for (RegionBoundsExecutorAbstract.RegionBounds<T> bounds : this.regionsList) {
         executor.run(bounds.region, bounds.regionStartTileX, bounds.regionStartTileY, bounds.regionEndTileX, bounds.regionEndTileY);
      }
   }

   public void runDimensions(RegionBoundsExecutorAbstract.DimensionsExecutor<T> executor) {
      this.calculateListIfNecessary();

      for (RegionBoundsExecutorAbstract.RegionBounds<T> bounds : this.regionsList) {
         executor.run(
            bounds.region,
            bounds.regionStartTileX,
            bounds.regionStartTileY,
            bounds.regionEndTileX - bounds.regionStartTileX + 1,
            bounds.regionEndTileY - bounds.regionStartTileY + 1
         );
      }
   }

   public void runCoordinates(RegionBoundsExecutorAbstract.CoordinateExecutor<T> executor) {
      this.calculateListIfNecessary();

      for (RegionBoundsExecutorAbstract.RegionBounds<T> bounds : this.regionsList) {
         for (int x = bounds.regionStartTileX; x <= bounds.regionEndTileX; x++) {
            for (int y = bounds.regionStartTileY; y <= bounds.regionEndTileY; y++) {
               executor.run(bounds.region, x, y);
            }
         }
      }
   }

   public Stream<RegionBoundsExecutorAbstract.RegionTilePosition<T>> streamCoordinates() {
      this.calculateListIfNecessary();
      return this.regionsList
         .stream()
         .flatMap(
            bounds -> IntStream.range(bounds.regionStartTileX, bounds.regionEndTileX + 1)
               .boxed()
               .flatMap(
                  regionTileX -> IntStream.range(bounds.regionStartTileY, bounds.regionEndTileY + 1)
                     .mapToObj(regionTileY -> new RegionBoundsExecutorAbstract.RegionTilePosition<>(bounds.region, regionTileX, regionTileY))
               )
         );
   }

   public void runOnBounds(int startTileX, int startTileY, int endTileX, int endTileY, RegionBoundsExecutorAbstract.CoordinateExecutor<T> executor) {
      this.calculateArrayIfNecessary();

      for (int tileX = startTileX; tileX <= endTileX; tileX++) {
         for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            int boundsRegionX = GameMath.divideByPowerOf2RoundedDown(tileX, this.regionSizeBits) - this.regionStartX;
            int boundsRegionY = GameMath.divideByPowerOf2RoundedDown(tileY, this.regionSizeBits) - this.regionStartY;
            RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.regionsArray[boundsRegionX][boundsRegionY];
            if (bounds != null) {
               executor.run(bounds.region, tileX - this.getRegionTileXOffset(bounds.region), tileY - this.getRegionTileYOffset(bounds.region));
            }
         }
      }
   }

   public void runOnTiles(Iterable<Point> tiles, boolean acceptOutOfTileBounds, RegionBoundsExecutorAbstract.CoordinateExecutor<T> executor) {
      this.calculateArrayIfNecessary();

      for (Point tile : tiles) {
         int boundsRegionX = GameMath.divideByPowerOf2RoundedDown(tile.x, this.regionSizeBits) - this.regionStartX;
         int boundsRegionY = GameMath.divideByPowerOf2RoundedDown(tile.y, this.regionSizeBits) - this.regionStartY;
         if (boundsRegionX >= 0 && boundsRegionX < this.regionsArray.length && boundsRegionY >= 0 && boundsRegionY < this.regionsArray[boundsRegionX].length) {
            RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.regionsArray[boundsRegionX][boundsRegionY];
            if (bounds != null) {
               executor.run(bounds.region, tile.x - this.getRegionTileXOffset(bounds.region), tile.y - this.getRegionTileYOffset(bounds.region));
            }
         } else if (!acceptOutOfTileBounds) {
            throw new IllegalArgumentException("Tile coordinates out of bounds " + boundsRegionX + "x" + boundsRegionY);
         }
      }
   }

   public void runOnTiles(Iterable<Point> tiles, RegionBoundsExecutorAbstract.CoordinateExecutor<T> executor) {
      this.runOnTiles(tiles, false, executor);
   }

   public boolean runOnTile(int tileX, int tileY, RegionBoundsExecutorAbstract.CoordinateExecutor<T> executor) {
      this.calculateArrayIfNecessary();
      int boundsRegionX = GameMath.divideByPowerOf2RoundedDown(tileX, this.regionSizeBits) - this.regionStartX;
      int boundsRegionY = GameMath.divideByPowerOf2RoundedDown(tileY, this.regionSizeBits) - this.regionStartY;
      if (boundsRegionX >= 0 && boundsRegionX < this.regionsArray.length && boundsRegionY >= 0 && boundsRegionY < this.regionsArray[boundsRegionX].length) {
         RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.regionsArray[boundsRegionX][boundsRegionY];
         if (bounds == null) {
            return false;
         }

         executor.run(bounds.region, tileX - this.getRegionTileXOffset(bounds.region), tileY - this.getRegionTileYOffset(bounds.region));
         return true;
      } else {
         throw new IllegalArgumentException("Tile coordinates out of bounds " + boundsRegionX + "x" + boundsRegionY);
      }
   }

   public <R> R getOnTile(int tileX, int tileY, RegionBoundsExecutorAbstract.CoordinateGetter<T, R> getter, R defaultValue) {
      this.calculateArrayIfNecessary();
      int boundsRegionX = GameMath.divideByPowerOf2RoundedDown(tileX, this.regionSizeBits) - this.regionStartX;
      int boundsRegionY = GameMath.divideByPowerOf2RoundedDown(tileY, this.regionSizeBits) - this.regionStartY;
      if (boundsRegionX >= 0 && boundsRegionX < this.regionsArray.length && boundsRegionY >= 0 && boundsRegionY < this.regionsArray[boundsRegionX].length) {
         RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.regionsArray[boundsRegionX][boundsRegionY];
         return bounds == null
            ? defaultValue
            : getter.get(bounds.region, tileX - this.getRegionTileXOffset(bounds.region), tileY - this.getRegionTileYOffset(bounds.region));
      } else {
         throw new IllegalArgumentException("Tile coordinates out of bounds: " + boundsRegionX + "x" + boundsRegionY);
      }
   }

   public boolean isTileLoaded(int tileX, int tileY) {
      this.calculateArrayIfNecessary();
      int boundsRegionX = GameMath.divideByPowerOf2RoundedDown(tileX, this.regionSizeBits) - this.regionStartX;
      int boundsRegionY = GameMath.divideByPowerOf2RoundedDown(tileY, this.regionSizeBits) - this.regionStartY;
      if (boundsRegionX >= 0 && boundsRegionX < this.regionsArray.length && boundsRegionY >= 0 && boundsRegionY < this.regionsArray[boundsRegionX].length) {
         RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.regionsArray[boundsRegionX][boundsRegionY];
         return bounds != null;
      } else {
         throw new IllegalArgumentException("Tile coordinates out of bounds " + boundsRegionX + "x" + boundsRegionY);
      }
   }

   public boolean isInsideBounds(int tileX, int tileY) {
      return tileX >= this.startTileX && tileX <= this.endTileX && tileY >= this.startTileY && tileY <= this.endTileY;
   }

   public T getRegionByTile(int tileX, int tileY) {
      return this.getRegion(GameMath.divideByPowerOf2RoundedDown(tileX, this.regionSizeBits), GameMath.divideByPowerOf2RoundedDown(tileY, this.regionSizeBits));
   }

   public T getRegion(int regionTileX, int regionTileY) {
      this.calculateArrayIfNecessary();
      int boundsRegionX = regionTileX - this.regionStartX;
      int boundsRegionY = regionTileY - this.regionStartY;
      if (boundsRegionX >= 0 && boundsRegionX < this.regionsArray.length && boundsRegionY >= 0 && boundsRegionY < this.regionsArray[boundsRegionX].length) {
         RegionBoundsExecutorAbstract.RegionBounds<T> bounds = this.regionsArray[boundsRegionX][boundsRegionY];
         return bounds == null ? null : bounds.region;
      } else {
         throw new IllegalArgumentException("Tile coordinates out of bounds " + boundsRegionX + "x" + boundsRegionY);
      }
   }

   public void calculateAllRegions() {
      this.calculateArrayIfNecessary();
   }

   @FunctionalInterface
   public interface BoundsExecutor<T> {
      void run(T var1, int var2, int var3, int var4, int var5);
   }

   @FunctionalInterface
   public interface CoordinateExecutor<T> {
      void run(T var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface CoordinateGetter<T, R> {
      R get(T var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface DimensionsExecutor<T> {
      void run(T var1, int var2, int var3, int var4, int var5);
   }

   protected static class RegionBounds<T> {
      public final T region;
      public final int regionX;
      public final int regionY;
      public final int regionStartTileX;
      public final int regionStartTileY;
      public final int regionEndTileX;
      public final int regionEndTileY;

      public RegionBounds(T region, int regionX, int regionY, int regionStartTileX, int regionStartTileY, int regionEndTileX, int regionEndTileY) {
         this.region = region;
         this.regionX = regionX;
         this.regionY = regionY;
         this.regionStartTileX = regionStartTileX;
         this.regionStartTileY = regionStartTileY;
         this.regionEndTileX = regionEndTileX;
         this.regionEndTileY = regionEndTileY;
      }
   }

   public static class RegionTilePosition<T> {
      public final T region;
      public final int regionTileX;
      public final int regionTileY;

      public RegionTilePosition(T region, int regionTileX, int regionTileY) {
         this.region = region;
         this.regionTileX = regionTileX;
         this.regionTileY = regionTileY;
      }
   }
}
