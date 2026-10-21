package necesse.level.maps.managers;

import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionBoundsExecutor;
import necesse.level.maps.regionSystem.layers.BiomeBlendingOptions;
import necesse.level.maps.regionSystem.layers.BiomeBlendingValue;

public class BiomeBlendingManager {
   public static int BLEND_RANGE = 4;
   public static int MAX_VALUE = 100;
   public final Level level;
   protected static Point[] crossPoints = new Point[]{new Point(0, 1), new Point(1, 0), new Point(0, -1), new Point(-1, 0)};

   public BiomeBlendingManager(Level level) {
      this.level = level;
   }

   public BiomeBlendingOptions.BiomeBlendingOption[] getBlendOptions(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region == null) {
         return new BiomeBlendingOptions.BiomeBlendingOption[0];
      }

      int regionTileX = tileX - region.tileXOffset;
      int regionTileY = tileY - region.tileYOffset;
      BiomeBlendingOptions blendingOptions = region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY);
      synchronized (this.level.entityManager.lock) {
         return blendingOptions == null ? null : blendingOptions.getBlendingOptions(region, tileX, tileY);
      }
   }

   public LinkedList<BiomeBlendingValue> getBlendValues(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region == null) {
         return new LinkedList<>();
      }

      int regionTileX = tileX - region.tileXOffset;
      int regionTileY = tileY - region.tileYOffset;
      BiomeBlendingOptions blendingOptions = region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY);
      synchronized (this.level.entityManager.lock) {
         return blendingOptions.getBlendingValues();
      }
   }

   public void updateBlends(int tileX, int tileY) {
      this.updateBlends(tileX - 1, tileY - 1, tileX + 1, tileY + 1);
   }

   public void updateBlends(int startTileX, int startTileY, int endTileX, int endTileY) {
      if (!this.level.isServer()) {
         startTileX = this.level.limitTileXToBounds(startTileX);
         startTileY = this.level.limitTileYToBounds(startTileY);
         endTileX = this.level.limitTileXToBounds(endTileX);
         endTileY = this.level.limitTileYToBounds(endTileY);
         RegionBoundsExecutor regions = new RegionBoundsExecutor(
            this.level.regionManager,
            this.level.limitTileXToBounds(startTileX) - BLEND_RANGE - 1,
            this.level.limitTileYToBounds(startTileY) - BLEND_RANGE - 1,
            this.level.limitTileXToBounds(endTileX) + BLEND_RANGE + 1,
            this.level.limitTileYToBounds(endTileY) + BLEND_RANGE + 1,
            false
         );
         PointHashMap<Integer> sourceRemoves = new PointHashMap<>();
         HashMap<Integer, BiomeBlendingManager.BiomeBlendingCompute> blendingComputes = new HashMap<>();

         for (int tileX = startTileX; tileX <= endTileX; tileX++) {
            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
               Region region = regions.getRegionByTile(tileX, tileY);
               if (region != null) {
                  int regionTileX = tileX - region.tileXOffset;
                  int regionTileY = tileY - region.tileYOffset;
                  int biomeID = region.biomeLayer.getBiomeIDByRegion(regionTileX, regionTileY);
                  BiomeBlendingOptions blendingOptions = region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY);
                  synchronized (this.level.entityManager.lock) {
                     LinkedList<Integer> biomeIDsToRemove = new LinkedList<>();

                     for (Entry<Integer, BiomeBlendingValue> entry : blendingOptions.getBlendingSources()) {
                        int sourceBiomeID = entry.getKey();
                        BiomeBlendingValue blending = entry.getValue();
                        if (sourceBiomeID != biomeID && (blending.sourceTileX != tileX || blending.sourceTileY != tileY)) {
                           if ((
                                 blending.sourceTileX < startTileX
                                    || blending.sourceTileY < startTileY
                                    || blending.sourceTileX > endTileX
                                    || blending.sourceTileY > endTileY
                              )
                              && entry.getValue().value > 0) {
                              BiomeBlendingManager.BiomeBlendingCompute sourceCompute = blendingComputes.compute(
                                 sourceBiomeID, (id, last) -> last == null ? new BiomeBlendingManager.BiomeBlendingCompute(this, regions, id) : last
                              );
                              sourceCompute.openTiles.add(new BiomeBlendingManager.SourcedPoint(blending.sourceTileX, blending.sourceTileY, tileX, tileY));
                           }
                        } else {
                           biomeIDsToRemove.add(sourceBiomeID);
                           sourceRemoves.put(tileX, tileY, sourceBiomeID);
                        }
                     }

                     for (int sourceBiomeID : biomeIDsToRemove) {
                        blendingOptions.removeBiome(sourceBiomeID);
                     }
                  }

                  BiomeBlendingManager.BiomeBlendingCompute compute = blendingComputes.compute(
                     biomeID, (id, last) -> last == null ? new BiomeBlendingManager.BiomeBlendingCompute(this, regions, id) : last
                  );
                  compute.openTiles.add(new BiomeBlendingManager.SourcedPoint(tileX, tileY, tileX, tileY));
                  blendingOptions.setBlendValue(biomeID, tileX, tileY, MAX_VALUE);
               }
            }
         }

         if (this.level.isTileXWithinBounds(startTileX - 1)) {
            startTileX--;

            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
               Region region = regions.getRegionByTile(startTileX, tileY);
               if (region != null) {
                  int regionTileX = startTileX - region.tileXOffset;
                  int regionTileY = tileY - region.tileYOffset;
                  region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY).markUpdateBlendOptions();
               }
            }
         }

         if (this.level.isTileXWithinBounds(endTileX + 1)) {
            endTileX++;

            for (int tileY = startTileY; tileY <= endTileY; tileY++) {
               Region region = regions.getRegionByTile(endTileX, tileY);
               if (region != null) {
                  int regionTileX = endTileX - region.tileXOffset;
                  int regionTileY = tileY - region.tileYOffset;
                  region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY).markUpdateBlendOptions();
               }
            }
         }

         if (this.level.isTileYWithinBounds(startTileY - 1)) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
               int tileY = startTileY - 1;
               Region region = regions.getRegionByTile(tileX, tileY);
               if (region != null) {
                  int regionTileX = tileX - region.tileXOffset;
                  int regionTileY = tileY - region.tileYOffset;
                  region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY).markUpdateBlendOptions();
               }
            }
         }

         if (this.level.isTileYWithinBounds(endTileY + 1)) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
               int tileY = endTileY + 1;
               Region region = regions.getRegionByTile(tileX, tileY);
               if (region != null) {
                  int regionTileX = tileX - region.tileXOffset;
                  int regionTileY = tileY - region.tileYOffset;
                  region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY).markUpdateBlendOptions();
               }
            }
         }

         PointHashSet nextChecks = new PointHashSet();

         for (Point tile : sourceRemoves.getKeys()) {
            this.removeSource(regions, sourceRemoves.get(tile.x, tile.y), tile.x, tile.y, nextChecks);
         }

         for (Point tile : nextChecks) {
            Region region = regions.getRegionByTile(tile.x, tile.y);
            if (region != null) {
               int regionTileX = tile.x - region.tileXOffset;
               int regionTileY = tile.y - region.tileYOffset;
               BiomeBlendingOptions blendingOptions = region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY);

               for (Entry<Integer, BiomeBlendingValue> entry : blendingOptions.getBlendingSources()) {
                  int biomeID = entry.getKey();
                  BiomeBlendingValue value = entry.getValue();
                  BiomeBlendingManager.BiomeBlendingCompute compute = blendingComputes.compute(
                     biomeID, (id, last) -> last == null ? new BiomeBlendingManager.BiomeBlendingCompute(this, regions, id) : last
                  );
                  if (value.value > 0) {
                     compute.openTiles.add(new BiomeBlendingManager.SourcedPoint(value.sourceTileX, value.sourceTileY, tile.x, tile.y));
                  }
               }
            }
         }

         for (BiomeBlendingManager.BiomeBlendingCompute compute : blendingComputes.values()) {
            compute.run();
         }
      }
   }

   protected void removeSource(RegionBoundsExecutor regions, int biomeID, int sourceTileX, int sourceTileY, PointHashSet nextChecks) {
      LinkedList<Point> open = new LinkedList<>();
      PointHashSet closed = new PointHashSet();
      open.add(new Point(sourceTileX, sourceTileY));
      closed.add(sourceTileX, sourceTileY);

      while (!open.isEmpty()) {
         Point current = open.removeFirst();

         for (Point delta : crossPoints) {
            int nextTileX = current.x + delta.x;
            int nextTileY = current.y + delta.y;
            if (regions.isInsideBounds(nextTileX, nextTileY) && !closed.contains(nextTileX, nextTileY) && this.level.isTileWithinBounds(nextTileX, nextTileY)) {
               Region region = regions.getRegionByTile(nextTileX, nextTileY);
               if (region != null) {
                  int regionTileX = nextTileX - region.tileXOffset;
                  int regionTileY = nextTileY - region.tileYOffset;
                  BiomeBlendingOptions blendingOptions = region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY);
                  synchronized (this.level.entityManager.lock) {
                     if (blendingOptions.removeSourceAndReturnBiomeID(biomeID, sourceTileX, sourceTileY) != -1) {
                        open.add(new Point(nextTileX, nextTileY));
                     } else {
                        nextChecks.add(nextTileX, nextTileY);
                     }
                  }

                  closed.add(nextTileX, nextTileY);
               }
            }
         }
      }
   }

   protected static class BiomeBlendingCompute {
      public final BiomeBlendingManager manager;
      public final RegionBoundsExecutor regions;
      public final int biomeID;
      public final int blendingPriority;
      public LinkedList<BiomeBlendingManager.SourcedPoint> openTiles = new LinkedList<>();

      public BiomeBlendingCompute(BiomeBlendingManager manager, RegionBoundsExecutor regions, int biomeID) {
         this.manager = manager;
         this.regions = regions;
         this.biomeID = biomeID;
         this.blendingPriority = BiomeRegistry.getBiome(biomeID).getBiomeBlendingPriority();
      }

      public void run() {
         while (!this.openTiles.isEmpty()) {
            BiomeBlendingManager.SourcedPoint current = this.openTiles.removeFirst();

            for (Point crossPoint : BiomeBlendingManager.crossPoints) {
               int nextTileX = current.currentTileX + crossPoint.x;
               int nextTileY = current.currentTileY + crossPoint.y;
               if (this.manager.level.isTileWithinBounds(nextTileX, nextTileY) && this.regions.isInsideBounds(nextTileX, nextTileY)) {
                  Region region = this.regions.getRegionByTile(nextTileX, nextTileY);
                  if (region != null) {
                     int regionTileX = nextTileX - region.tileXOffset;
                     int regionTileY = nextTileY - region.tileYOffset;
                     Biome biome = region.biomeLayer.getBiomeByRegion(regionTileX, regionTileY);
                     if (biome.getID() != this.biomeID && biome.getBiomeBlendingPriority() != this.blendingPriority) {
                        double nextRange = Math.max(GameMath.diagonalMoveDistance(nextTileX, nextTileY, current.sourceTileX, current.sourceTileY) - 1.0, 0.0);
                        boolean shouldOpen = true;
                        if (nextRange >= BiomeBlendingManager.BLEND_RANGE) {
                           nextRange = BiomeBlendingManager.BLEND_RANGE;
                           shouldOpen = false;
                        }

                        double nextRangeFloat = nextRange / BiomeBlendingManager.BLEND_RANGE;
                        int nextValue = (int)((1.0 - nextRangeFloat) * BiomeBlendingManager.MAX_VALUE);
                        BiomeBlendingOptions blendingOptions = region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY);
                        synchronized (this.manager.level.entityManager.lock) {
                           BiomeBlendingValue currentValue = blendingOptions.getBlendValue(this.biomeID);
                           if (currentValue != null && currentValue.value >= nextValue) {
                              continue;
                           }

                           blendingOptions.setBlendValue(this.biomeID, current.sourceTileX, current.sourceTileY, nextValue);
                        }

                        if (shouldOpen) {
                           this.openTiles.addLast(new BiomeBlendingManager.SourcedPoint(current.sourceTileX, current.sourceTileY, nextTileX, nextTileY));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected static class SourcedPoint {
      public int sourceTileX;
      public int sourceTileY;
      public int currentTileX;
      public int currentTileY;

      public SourcedPoint(int sourceTileX, int sourceTileY, int currentTileX, int currentTileY) {
         this.sourceTileX = sourceTileX;
         this.sourceTileY = sourceTileY;
         this.currentTileX = currentTileX;
         this.currentTileY = currentTileY;
      }
   }
}
