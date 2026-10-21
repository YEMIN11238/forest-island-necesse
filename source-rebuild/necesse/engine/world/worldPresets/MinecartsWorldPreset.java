package necesse.engine.world.worldPresets;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Line2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.PointHashSet;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.entity.mobs.Mob;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.LinesGeneration;

public class MinecartsWorldPreset extends WorldPreset {
   @Override
   public boolean shouldAddToRegion(LevelPresetsRegion presetsRegion) {
      return presetsRegion.identifier.equals(LevelIdentifier.CAVE_IDENTIFIER);
   }

   @Override
   public void addToRegion(GameRandom random, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, PerformanceTimerManager performanceTimer) {
      int total = getTotalPoints(random, presetsRegion, 0.02F);

      for (int i = 0; i < total; i++) {
         Point startTile = findRandomPresetTile(random, presetsRegion, 10, new Dimension(4, 4), (String)null, null);
         if (startTile != null) {
            ArrayList<LinesGeneration> tntArms = new ArrayList<>();
            LinesGeneration lg = Performance.record(performanceTimer, "linesGeneration", () -> {
               LinesGeneration linesGeneration = new LinesGeneration(startTile.x + 2, startTile.y + 2);
               int armAngle = random.nextInt(4) * 90;
               int arms = random.getIntBetween(6, 15);

               for (int j = 0; j < arms; j++) {
                  float width = random.getFloatBetween(1.0F, 3.0F);
                  LinesGeneration nextArm = linesGeneration.addArm(random.getIntOffset(armAngle, 20), random.getIntBetween(5, 25), width);
                  if (!this.isTileWithinBounds(nextArm.x2, nextArm.y2, presetsRegion, (int)Math.ceil(width) + 1)) {
                     linesGeneration.removeLastLine();
                     break;
                  }

                  linesGeneration = nextArm;
                  tntArms.add(linesGeneration);
                  int angleChange = random.getOneOfWeighted(Integer.class, 30, 0, 5, 90, 5, -90);
                  armAngle += angleChange;
               }

               return linesGeneration;
            });
            if (!lg.getRoot().isEmpty()) {
               PointHashSet tiles = Performance.record(performanceTimer, "getPoints", lg::getDiamondPoints);
               ArrayList<Float> tntTileLines = new ArrayList<>();
               Performance.record(
                  performanceTimer,
                  "tntLines",
                  () -> {
                     int tntCount = random.getOneOfWeighted(Integer.class, 100, 0, 200, 1, 50, 2);

                     for (int j = 0; j < tntCount && !tntArms.isEmpty(); j++) {
                        int index = random.nextInt(tntArms.size());
                        LinesGeneration next = tntArms.remove(index);
                        int wireLength = random.getIntBetween(10, 14) * random.getOneOf(1, -1);
                        float lineLength = (float)new Point(next.x1, next.y1).distance(next.x2, next.y2);
                        java.awt.geom.Point2D.Float dir = GameMath.normalize(next.x1 - next.x2, next.y1 - next.y2);
                        float linePointLength = random.getFloatBetween(0.0F, lineLength);
                        java.awt.geom.Point2D.Float lineTile = new java.awt.geom.Point2D.Float(
                           next.x2 + dir.x * linePointLength, next.y2 + dir.y * linePointLength
                        );
                        java.awt.geom.Point2D.Float leverTile = GameMath.getPerpendicularPoint(lineTile, 2.0F * Math.signum(wireLength), dir);
                        java.awt.geom.Point2D.Float tntTile = GameMath.getPerpendicularPoint(lineTile, wireLength, dir);
                        int tntTileX = tntTile.x < 0.0F ? (int)Math.floor(tntTile.x) : (int)tntTile.x;
                        int tntTileY = tntTile.y < 0.0F ? (int)Math.floor(tntTile.y) : (int)tntTile.y;
                        if (this.isTileWithinBounds(tntTileX, tntTileY, presetsRegion, 2)) {
                           Float wireTileLine = new Float(leverTile, tntTile);
                           tntTileLines.add(wireTileLine);
                        }
                     }
                  }
               );
               RegionTileWorldPresetGenerator tileGenerator = new RegionTileWorldPresetGenerator();
               GameObject crateObject = ObjectRegistry.getObject("crate");
               int trackObject = ObjectRegistry.getObjectID("minecarttrack");
               Performance.record(performanceTimer, "clearAndPlaceCrates", () -> {
                  for (Point tile : tiles) {
                     tileGenerator.addTile(tile.x, tile.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                           if (level.isSolidTile(tileX, tileY)) {
                              level.setObject(tileX, tileY, 0);
                           }

                           if (random.getChance(0.05) && crateObject.canPlace(level, tileX, tileY, 0, false) == null) {
                              crateObject.placeObject(level, tileX, tileY, 0, false);
                           }
                        }
                     });
                  }
               });
               ArrayList<Point> trackTiles = new ArrayList<>();
               Performance.record(performanceTimer, "placeTracks", () -> lg.getRoot().recursiveLines(lg2 -> {
                  GameLinkedList<Point> minecartTiles = new GameLinkedList<>();
                  LinesGeneration.pathTiles(lg2.getTileLine(), true, (from, next) -> minecartTiles.add(next));

                  for (GameLinkedList<Point>.Element el : minecartTiles.elements()) {
                     Point current = (Point)el.object;
                     if (this.isTileWithinBounds(current.x, current.y, presetsRegion, 2)) {
                        trackTiles.add(current);
                        GameLinkedList<Point>.Element nextEl = el.next();
                        if (nextEl != null) {
                           final Point next = (Point)nextEl.object;
                           tileGenerator.addTile(current.x, current.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                              @Override
                              public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                                 if (next.x < tileX) {
                                    level.setObject(tileX, tileY, trackObject, 3);
                                 } else if (next.x > tileX) {
                                    level.setObject(tileX, tileY, trackObject, 1);
                                 } else if (next.y < tileY) {
                                    level.setObject(tileX, tileY, trackObject, 0);
                                 } else if (next.y > tileY) {
                                    level.setObject(tileX, tileY, trackObject, 2);
                                 }
                              }
                           });
                        } else {
                           GameLinkedList<Point>.Element prevEl = el.prev();
                           if (prevEl != null) {
                              final Point prev = (Point)prevEl.object;
                              tileGenerator.addTile(current.x, current.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                                 @Override
                                 public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                                    if (prev.x < tileX) {
                                       level.setObject(tileX, tileY, trackObject, 1);
                                    } else if (prev.x > tileX) {
                                       level.setObject(tileX, tileY, trackObject, 3);
                                    } else if (prev.y < tileY) {
                                       level.setObject(tileX, tileY, trackObject, 2);
                                    } else if (prev.y > tileY) {
                                       level.setObject(tileX, tileY, trackObject, 0);
                                    }
                                 }
                              });
                           } else {
                              tileGenerator.addTile(current.x, current.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                                 @Override
                                 public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                                    level.setObject(tileX, tileY, trackObject, 0);
                                 }
                              });
                           }
                        }
                     }
                  }

                  return true;
               }));
               Performance.record(performanceTimer, "placeMinecart", () -> {
                  int minecartCount = random.getOneOfWeighted(Integer.class, 100, 0, 200, 1, 50, 2);

                  for (int j = 0; j < minecartCount && !trackTiles.isEmpty(); j++) {
                     int index = random.nextInt(trackTiles.size());
                     Point next = trackTiles.remove(index);
                     tileGenerator.addTile(next.x, next.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                           Mob minecart = MobRegistry.getMob("minecart", level);
                           level.entityManager.addMob(minecart, tileX * 32 + 16, tileY * 32 + 16);
                        }
                     });
                  }
               });
               Performance.record(performanceTimer, "placeTNT", () -> {
                  for (Float tntLine : tntTileLines) {
                     LinkedList<Point> tntTiles = new LinkedList<>();
                     LinesGeneration.pathTiles(tntLine, true, (fromTile, nextTile) -> tntTiles.add(nextTile));

                     for (Point tile : tntTiles) {
                        tileGenerator.addTile(tile.x, tile.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                           @Override
                           public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                              level.wireManager.setWire(tileX, tileY, 0, true);
                              if (level.getObject(tileX, tileY).isSolid) {
                                 level.setObject(tileX, tileY, 0);
                              }
                           }
                        });
                     }

                     Point first = tntTiles.getFirst();
                     tileGenerator.addTile(first.x, first.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                           level.setObject(tileX, tileY, ObjectRegistry.getObjectID("rocklever"));
                        }
                     });
                     Point last = tntTiles.getLast();
                     tileGenerator.addTile(last.x, last.y, new RegionTileWorldPresetGenerator.TilePlaceFunction() {
                        @Override
                        public void place(GameRandom random, Level level, int tileX, int tileY, PerformanceTimerManager timer) {
                           level.setObject(tileX, tileY, ObjectRegistry.getObjectID("tnt"));
                        }
                     });
                  }
               });
               tileGenerator.addToRegion(this, presetsRegion);
            }
         }
      }
   }
}
