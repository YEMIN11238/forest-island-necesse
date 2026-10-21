package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
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
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.presets.FishianHousePreset1;
import necesse.level.maps.presets.FishianHousePreset2;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetUtils;

public class FishianMiniBiomeWorldPreset extends WorldPreset {
   public int size = 75;

   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER) && presetsRegion.hasAnyOfBiome(BiomeRegistry.SWAMP.getID());
   }

   @Override
   public void addToRegion(
      GameRandom random, final LevelPresetsRegion presetsRegion, final BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer
   ) {
      int total = getTotalBiomePoints(random, presetsRegion, BiomeRegistry.SWAMP, 0.006F);

      for (int i = 0; i < total; i++) {
         Dimension dimension = new Dimension(this.size, this.size);
         Point placeTile = findRandomBiomePresetTile(
            random,
            presetsRegion,
            generatorStack,
            BiomeRegistry.SWAMP,
            50,
            dimension,
            new String[]{"minibiomes", "loot"},
            new WorldPreset.ValidTilePredicate() {
               @Override
               public boolean isValidPosition(int tileX, int tileY) {
                  return FishianMiniBiomeWorldPreset.this.runCornerCheck(
                     tileX, tileY, FishianMiniBiomeWorldPreset.this.size, FishianMiniBiomeWorldPreset.this.size, new WorldPreset.ValidTilePredicate() {
                        @Override
                        public boolean isValidPosition(int tileX, int tileY) {
                           return generatorStack.getLazyBiomeID(tileX, tileY) == BiomeRegistry.SWAMP.getID();
                        }
                     }
                  );
               }
            }
         );
         if (placeTile != null) {
            LinesGenerationWorldPreset lg = new LinesGenerationWorldPreset(placeTile.x + this.size / 2, placeTile.y + this.size / 2)
               .addRandomArms(random, 16, this.size / 2.0F - 10.0F, this.size / 2.0F, 6.0F, 8.0F);
            if (lg.isWithinPresetRegionBounds(presetsRegion)) {
               RegionTileWorldPresetGenerator tileGenerator = new RegionTileWorldPresetGenerator();
               final PointHashSet validTiles = Performance.record(performanceTimer, "getDiamondPoints", lg::getDiamondPoints);

               for (Point tile : validTiles) {
                  tileGenerator.addTile(tile.x, tile.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                     @Override
                     public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                        level.setTile(tileX, tileY, TileRegistry.puddleCobble);
                        level.setObject(tileX, tileY, 0);
                     }
                  });
               }

               Rectangle bounds = lg.getOccupiedTileRectangle();
               ArrayList<Float> voronoiPoints = new ArrayList<>();
               int voronoiPadding = -20;
               int usableWidth = bounds.width - voronoiPadding * 2;
               int usableHeight = bounds.height - voronoiPadding * 2;
               int resolution = 20;
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
               final PointHashSet floorTiles = new PointHashSet();
               PointHashSet houseTiles = new PointHashSet();

               for (TriangleLine line : voronoiLines) {
                  LinesGeneration.pathTiles(new java.awt.geom.Line2D.Float(line.p1, line.p2), true, (from, next) -> {
                     if (validTiles.contains(next.x, next.y)) {
                        tileGenerator.addTile(next.x, next.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                           @Override
                           public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                              if (random.getChance(0.8F)) {
                                 level.setTile(tileX, tileY, TileRegistry.deepSwampStoneFloorID);
                              }

                              level.setObject(tileX, tileY, 0);
                           }
                        });
                        floorTiles.add(next.x, next.y);
                     }
                  });
               }

               AtomicInteger lootRotation = new AtomicInteger();
               ArrayList<BiFunction<GameRandom, AtomicInteger, Preset>> housePresets = new ArrayList<>();
               housePresets.add(FishianHousePreset1::new);
               housePresets.add(FishianHousePreset2::new);
               LinkedList<Runnable> housePresetRunners = new LinkedList<>();

               while (!voronoiPoints.isEmpty()) {
                  int pointsIndex = random.nextInt(voronoiPoints.size());
                  Float centerTile = voronoiPoints.remove(pointsIndex);
                  int centerTileX = (int)centerTile.x;
                  int centerTileY = (int)centerTile.y;
                  if (!housePresets.isEmpty()) {
                     Dimension houseSize = new Dimension(10, 10);
                     int houseTileX = centerTileX - houseSize.width / 2;
                     int houseTileY = centerTileY - houseSize.height / 2;
                     boolean canPlaceHouse = this.runGridCheck(
                        houseTileX, houseTileY, houseSize.width, houseSize.height, 2, new WorldPreset.ValidTilePredicate() {
                           @Override
                           public boolean isValidPosition(int tileX, int tileY) {
                              return !floorTiles.contains(tileX, tileY) && validTiles.contains(tileX, tileY);
                           }
                        }
                     );
                     if (canPlaceHouse) {
                        BiFunction<GameRandom, AtomicInteger, Preset> presetGetter = housePresets.remove(0);
                        housePresetRunners.add(
                           () -> presetsRegion.addPreset(
                              this, houseTileX, houseTileY, houseSize, (String)null, new LevelPresetsRegion.WorldPresetPlaceFunction() {
                                 @Override
                                 public void place(GameRandom random, Level level, PerformanceTimerManager timer) {
                                    Preset preset = presetGetter.apply(random, lootRotation);
                                    preset = PresetUtils.randomizeRotationAndMirror(preset, random);
                                    int placeOffsetX = houseSize.width / 2 - preset.width / 2;
                                    int placeOffsetY = houseSize.height / 2 - preset.height / 2;
                                    preset.applyToLevel(level, houseTileX + placeOffsetX, houseTileY + placeOffsetY);
                                 }
                              }
                           )
                        );
                     }
                  } else if (!floorTiles.contains(centerTileX, centerTileY) && !houseTiles.contains(centerTileX, centerTileY) && !random.getChance(0.2F)) {
                     LinesGeneration lakeLG = new LinesGeneration(centerTileX, centerTileY);
                     int lakeArms = random.getIntBetween(5, 7);
                     int angle = random.nextInt(360);
                     int anglePerArm = 360 / lakeArms;

                     for (int j = 0; j < lakeArms; j++) {
                        angle += random.getIntOffset(anglePerArm, anglePerArm / 2);
                        Float lakeDir = GameMath.getAngleDir(angle);
                        AtomicReference<Point> armEndPointRef = new AtomicReference<>(new Point(centerTileX, centerTileY));
                        LinesGeneration.pathTilesBreak(
                           new java.awt.geom.Line2D.Float(
                              centerTile.x, centerTile.y, centerTile.x + lakeDir.x * resolution, centerTile.y + lakeDir.y * resolution
                           ),
                           true,
                           (from, next) -> {
                              int tileXx = next.x;
                              int tileYx = next.y;
                              if (floorTiles.contains(tileXx, tileYx)) {
                                 return false;
                              }

                              if (houseTiles.contains(tileXx, tileYx)) {
                                 return false;
                              }

                              armEndPointRef.set(new Point(tileXx, tileYx));
                              return true;
                           }
                        );
                        Point armEndPoint = armEndPointRef.get();
                        float width = random.getFloatBetween(2.0F, 3.0F);
                        lakeLG.addLine((int)centerTile.x, (int)centerTile.y, armEndPoint.x, armEndPoint.y, width);
                     }

                     GameObject cattail = ObjectRegistry.getObject("cattail");
                     GameObject reeds = ObjectRegistry.getObject("reeds");
                     CellAutomaton lakeCA = lakeLG.doCellularAutomaton(random);
                     lakeCA.streamAliveOrdered().forEach(tilex -> {
                        if (validTiles.contains(tilex.x, tilex.y)) {
                           if (!floorTiles.contains(tilex.x, tilex.y)) {
                              if (!houseTiles.contains(tilex.x, tilex.y)) {
                                 tileGenerator.addTile(tilex.x, tilex.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                                    @Override
                                    public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                                       level.setTile(tileX, tileY, TileRegistry.waterID);
                                       level.setObject(tileX, tileY, 0);
                                       if (random.getChance(0.1F) && reeds.canPlace(level, 0, tileX, tileY, 0, false, false) == null) {
                                          reeds.placeObject(level, 0, tileX, tileY, 0, false);
                                       }

                                       if (random.getChance(0.1F) && cattail.canPlace(level, 0, tileX, tileY, 0, false, false) == null) {
                                          cattail.placeObject(level, 0, tileX, tileY, 0, false);
                                       }
                                    }
                                 });
                              }
                           }
                        }
                     });
                  }
               }

               for (Point floorTile : floorTiles) {
                  for (int x = -1; x <= 1; x++) {
                     int tileX = floorTile.x + x;

                     for (int y = -1; y <= 1; y++) {
                        int tileY = floorTile.y + y;
                        if (!floorTiles.contains(tileX, tileY) && !houseTiles.contains(tileX, tileY)) {
                           tileGenerator.addTile(tileX, tileY, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                              @Override
                              public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                                 level.setTile(tileX, tileY, TileRegistry.puddleCobble);
                              }
                           });
                        }
                     }
                  }
               }

               final TicketSystemList<GameObject> randomObjects = new TicketSystemList<>();
               randomObjects.addObject(20, ObjectRegistry.getObject("seashell"));
               randomObjects.addObject(20, ObjectRegistry.getObject("seasnail"));
               randomObjects.addObject(20, ObjectRegistry.getObject("seastar"));
               randomObjects.addObject(25, ObjectRegistry.getObject("bamboodebris"));
               randomObjects.addObject(25, ObjectRegistry.getObject("bambootree"));
               randomObjects.addObject(100, ObjectRegistry.getObject("glowcoral"));

               for (Point tile : validTiles) {
                  if (!floorTiles.contains(tile.x, tile.y) && random.getChance(0.2F)) {
                     tileGenerator.addTile(tile.x, tile.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                           GameObject obj = randomObjects.getRandomObject(random);
                           if (obj.canPlace(level, 0, tileX, tileY, 0, false, true) == null) {
                              obj.placeObject(level, 0, tileX, tileY, 0, false);
                           }
                        }
                     });
                  }
               }

               tileGenerator.forEachRegion(
                  new RegionTileWorldPresetGenerator.ForEachFunction() {
                     @Override
                     public void handle(int regionX, int regionY, LevelPresetsRegion.WorldPresetPlaceFunction placeFunction) {
                        int tileX = GameMath.getTileCoordByRegion(regionX);
                        int tileY = GameMath.getTileCoordByRegion(regionY);
                        presetsRegion.addPreset(
                           FishianMiniBiomeWorldPreset.this, tileX, tileY, new Dimension(16, 16), new String[]{"minibiomes", "loot"}, placeFunction
                        );
                     }
                  }
               );
               housePresetRunners.forEach(Runnable::run);
            }
         }
      }
   }
}
