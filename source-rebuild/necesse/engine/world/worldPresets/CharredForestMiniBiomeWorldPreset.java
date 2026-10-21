package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.TicketSystemList;
import necesse.engine.util.voronoi.DelaunayTriangulator;
import necesse.engine.util.voronoi.TriangleLine;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.entity.mobs.Mob;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.regionSystem.Region;

public class CharredForestMiniBiomeWorldPreset extends WorldPreset {
   public int size = 100;
   public int maxExpansionCount = 10;
   public int expansionResolution = 10;
   public HashSet<Integer> validBiomeIDs = new HashSet<>(Arrays.asList(BiomeRegistry.FOREST.getID(), BiomeRegistry.PLAINS.getID()));

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.SURFACE_IDENTIFIER) && this.validBiomeIDs.stream().anyMatch(presetsRegion::hasAnyOfBiome);
   }

   private int attemptExpand(
      LevelPresetsRegion presetsRegion,
      String[] occupiedBoards,
      Point placeTile,
      Dimension size,
      int resolution,
      int maxExpansion,
      WorldPreset.ValidTilePredicate validTileCheck,
      int validChecksOffset,
      int validCheckResolution
   ) {
      int expandedCount = 0;
      boolean validLeft = true;
      boolean validRight = true;
      boolean validUp = true;
      boolean validDown = true;

      while (maxExpansion > 0) {
         boolean expanded = false;
         int nextTileY = placeTile.y;
         float tilesPerValidCheck = (float)(size.height + validChecksOffset * 2) / validCheckResolution;
         int totalValidChecks = Math.max((int)Math.ceil(tilesPerValidCheck), 1);
         int nextTileXLeft = placeTile.x - resolution - validChecksOffset;
         if (validLeft && !presetsRegion.isRectangleOccupied(occupiedBoards, nextTileXLeft, nextTileY, resolution, size.height)) {
            for (int j = 0; j < totalValidChecks; j++) {
               int currentTileY = Math.min(placeTile.y - validChecksOffset + j * validCheckResolution, nextTileY + size.height - 1 + validChecksOffset);
               if (!validTileCheck.isValidPosition(nextTileXLeft, currentTileY)) {
                  validLeft = false;
                  break;
               }
            }

            if (validLeft) {
               placeTile.x -= resolution;
               size.width += resolution;
               expanded = true;
               maxExpansion--;
               expandedCount++;
               if (maxExpansion <= 0) {
                  break;
               }
            }
         }

         int nextTileXRight = placeTile.x + size.width + validChecksOffset;
         if (validRight && !presetsRegion.isRectangleOccupied(occupiedBoards, nextTileXRight, nextTileY, resolution, size.height)) {
            for (int j = 0; j < totalValidChecks; j++) {
               int currentTileY = Math.min(placeTile.y - validChecksOffset + j * validCheckResolution, nextTileY + size.height - 1 + validChecksOffset);
               if (!validTileCheck.isValidPosition(nextTileXRight, currentTileY)) {
                  validRight = false;
                  break;
               }
            }

            if (validRight) {
               size.width += resolution;
               expanded = true;
               maxExpansion--;
               expandedCount++;
               if (maxExpansion <= 0) {
                  break;
               }
            }
         }

         nextTileY = placeTile.x;
         tilesPerValidCheck = (float)(size.width + validChecksOffset * 2) / validCheckResolution;
         totalValidChecks = Math.max((int)Math.ceil(tilesPerValidCheck), 1);
         nextTileXLeft = placeTile.y - resolution - validChecksOffset;
         if (validUp && !presetsRegion.isRectangleOccupied(occupiedBoards, nextTileY, nextTileXLeft, size.width, resolution)) {
            for (int j = 0; j < totalValidChecks; j++) {
               int currentTileX = Math.min(placeTile.x - validChecksOffset + j * validCheckResolution, nextTileY + size.width - 1 + validChecksOffset);
               if (!validTileCheck.isValidPosition(currentTileX, nextTileXLeft)) {
                  validUp = false;
                  break;
               }
            }

            if (validUp) {
               placeTile.y -= resolution;
               size.height += resolution;
               expanded = true;
               maxExpansion--;
               expandedCount++;
               if (maxExpansion <= 0) {
                  break;
               }
            }
         }

         nextTileXRight = placeTile.y + size.height + validChecksOffset;
         if (validDown && !presetsRegion.isRectangleOccupied(occupiedBoards, nextTileY, nextTileXRight, size.width, resolution)) {
            for (int j = 0; j < totalValidChecks; j++) {
               int currentTileX = Math.min(placeTile.x - validChecksOffset + j * validCheckResolution, nextTileY + size.width - 1 + validChecksOffset);
               if (!validTileCheck.isValidPosition(currentTileX, nextTileXRight)) {
                  validDown = false;
                  break;
               }
            }

            if (validDown) {
               size.height += resolution;
               expanded = true;
               maxExpansion--;
               expandedCount++;
               if (maxExpansion <= 0) {
                  break;
               }
            }
         }

         if (!expanded) {
            break;
         }
      }

      return expandedCount;
   }

   @Override
   public void addToRegion(
      GameRandom random, final LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, this.validBiomeIDs, 5.0E-4F);

      for (int i = 0; i < total; i++) {
         final Dimension dimension = new Dimension(this.size, this.size);
         final WorldPreset.ValidTilePredicate validTileCheck = new WorldPreset.ValidTilePredicate() {
            @Override
            public boolean isValidPosition(int tileX, int tileY) {
               return generatorStack.isSurfaceOcean(tileX, tileY)
                  ? false
                  : CharredForestMiniBiomeWorldPreset.this.validBiomeIDs.contains(generatorStack.getLazyBiomeID(tileX, tileY));
            }
         };
         String[] occupiedBoards = new String[]{"minibiomes", "loot", "villages"};
         Point placeTile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            this.validBiomeIDs,
            1000,
            dimension,
            occupiedBoards,
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return CharredForestMiniBiomeWorldPreset.this.runGridCheck(
                     tileX - 10, tileY - 10, dimension.width + 20, dimension.height + 20, 20, validTileCheck
                  );
               }
            }
         );
         if (placeTile != null) {
            Performance.record(
               performanceTimer,
               "attempExpand",
               () -> this.attemptExpand(
                  presetsRegion, occupiedBoards, placeTile, dimension, this.expansionResolution, this.maxExpansionCount, validTileCheck, 10, 20
               )
            );
         }

         if (placeTile != null) {
            int minSize = GameMath.min(dimension.width, dimension.height);
            LinesGenerationWorldPreset lg = new LinesGenerationWorldPreset(placeTile.x + dimension.width / 2, placeTile.y + dimension.height / 2)
               .addRandomArms(random, 13, minSize / 2.0F - 10.0F, minSize / 2.0F, 15.0F, 18.0F);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               final LevelPresetsRegion.TiledPlaceableWorldPreset spawnTestPreset = presetsRegion.addPreset(
                  this, placeTile.x, placeTile.y, dimension, (String[])null, new LevelPresetsRegion.WorldPresetPlaceFunction() {
                     @Override
                     public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                     }
                  }
               );
               RegionTileWorldPresetGenerator tileGenerator = new RegionTileWorldPresetGenerator();
               PointHashSet validTiles = Performance.record(performanceTimer, "getDiagonalDistancePoints", lg::getDiagonalDistancePoints);

               for (Point region : lg.getRegionPointsSet()) {
                  tileGenerator.onRegionGenerated(region.x, region.y, new RegionTileWorldPresetGenerator.RegionGeneratedFunction() {
                     @Override
                     public void onRegionGenerated(GameRandom random, Level level, Region regionx, PerformanceTimerManager timer) {
                        for (Mob mob : level.entityManager.mobs.getInRegion(regionx.regionX, regionx.regionY)) {
                           int tileID = level.getTileID(mob.getTileX(), mob.getTileY());
                           if (tileID == TileRegistry.ashID || tileID == TileRegistry.gravelID) {
                              mob.remove();
                           }
                        }
                     }
                  });
               }

               for (Point tile : validTiles) {
                  tileGenerator.addTile(tile.x, tile.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                     @Override
                     public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                        if (level.getTile(tileX, tileY).isLiquid) {
                           if (random.getChance(0.1F)) {
                              level.setTile(tileX, tileY, TileRegistry.ashID);
                           } else {
                              level.setTile(tileX, tileY, TileRegistry.gravelID);
                           }
                        } else {
                           level.setTile(tileX, tileY, TileRegistry.ashID);
                        }

                        level.setObject(tileX, tileY, 0);
                     }
                  });
               }

               Rectangle bounds = lg.getOccupiedTileRectangle();
               ArrayList<Float> voronoiPoints = new ArrayList<>();
               int voronoiPadding = -20;
               int usableWidth = bounds.width - voronoiPadding * 2;
               int usableHeight = bounds.height - voronoiPadding * 2;
               int resolution = 30;
               int resWidth = usableWidth / resolution;
               int resHeight = usableHeight / resolution;
               int xOffset = bounds.x + voronoiPadding + usableWidth % resolution / 2;
               int yOffset = bounds.y + voronoiPadding + usableHeight % resolution / 2;

               for (int x = 0; x < resWidth; x++) {
                  int minX = x * resolution + xOffset;
                  int maxX = minX + resolution;

                  for (int y = 0; y < resHeight; y++) {
                     int minY = y * resolution + yOffset;
                     int maxY = minY + resolution;
                     Float point = new Float(random.getIntBetween(minX, maxX), random.getIntBetween(minY, maxY));
                     voronoiPoints.add(point);
                  }
               }

               ArrayList<TriangleLine> voronoiLines = new ArrayList<>();
               DelaunayTriangulator.compute(voronoiPoints, false, voronoiLines);

               for (TriangleLine line : voronoiLines) {
                  LinesGeneration.pathTiles(new java.awt.geom.Line2D.Float(line.p1, line.p2), true, (from, next) -> {
                     if (validTiles.contains(next.x, next.y)) {
                        tileGenerator.addTile(next.x, next.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                           @Override
                           public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                              if (!level.getTile(tileX, tileY).isLiquid && random.getChance(0.8F)) {
                                 level.setTile(tileX, tileY, TileRegistry.gravelID);
                              }
                           }
                        });
                     }
                  });
               }

               final TicketSystemList<GameObject> randomObjects = new TicketSystemList<>();
               randomObjects.addObject(20, ObjectRegistry.getObject("burnedtree"));
               randomObjects.addObject(30, ObjectRegistry.getObject("burnedbush"));
               randomObjects.addObject(5, ObjectRegistry.getObject("burnedtreestump"));
               randomObjects.addObject(2, ObjectRegistry.getObject("burnedlogbench"));
               randomObjects.addObject(10, ObjectRegistry.getObject("burnedleafpile"));
               randomObjects.addObject(10, ObjectRegistry.getObject("charredrock"));
               randomObjects.addObject(20, ObjectRegistry.getObject("charredrocksmall"));

               for (Point tile : validTiles) {
                  if (random.getChance(0.15F)) {
                     tileGenerator.addTile(tile.x, tile.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                           int tileID = level.getTileID(tileX, tileY);
                           if (tileID == TileRegistry.ashID) {
                              GameObject obj = randomObjects.getRandomObject(random);
                              int rotation = random.nextInt(4);
                              if (obj.canPlace(level, 0, tileX, tileY, rotation, false, true) == null) {
                                 obj.placeObject(level, 0, tileX, tileY, rotation, false);
                              }
                           }
                        }
                     });
                  }
               }

               tileGenerator.forEachRegion(
                  new RegionTileWorldPresetGenerator.ForEachFunction() {
                     @Override
                     public void handle(int regionX, int regionY, final LevelPresetsRegion.WorldPresetPlaceFunction placeFunction) {
                        int tileX = GameMath.getTileCoordByRegion(regionX);
                        int tileY = GameMath.getTileCoordByRegion(regionY);
                        presetsRegion.addPreset(
                           CharredForestMiniBiomeWorldPreset.this,
                           tileX,
                           tileY,
                           new Dimension(16, 16),
                           new String[]{"minibiomes", "loot"},
                           new LevelPresetsRegion.WorldPresetPlaceFunction() {
                              @Override
                              public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                                 if (!spawnTestPreset.presetBlockedBySpawn) {
                                    placeFunction.place(random, level, timer);
                                 }
                              }
                           }
                        );
                     }
                  }
               );
            }
         }
      }
   }
}
