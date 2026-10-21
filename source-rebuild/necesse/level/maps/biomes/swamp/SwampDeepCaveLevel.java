package necesse.level.maps.biomes.swamp;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.AreaFinder;
import necesse.engine.GameEvents;
import necesse.engine.events.worldGeneration.GenerateCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GenerateCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GenerateCaveOresEvent;
import necesse.engine.events.worldGeneration.GenerateCaveStructuresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveOresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveStructuresEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.util.voronoi.DelaunayTriangulator;
import necesse.engine.util.voronoi.TriangleLine;
import necesse.engine.world.WorldEntity;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.presets.AgedChampionArenaPreset;
import necesse.level.maps.presets.FishianHousePreset1;
import necesse.level.maps.presets.FishianHousePreset2;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.PresetRotateException;
import necesse.level.maps.presets.PresetRotation;
import necesse.level.maps.presets.PresetUtils;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.RandomLootAreaPreset;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class SwampDeepCaveLevel extends SwampCaveLevel {
   public SwampDeepCaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public SwampDeepCaveLevel(int islandX, int islandY, int dimension, WorldEntity worldEntity, Biome biome) {
      super(new LevelIdentifier(islandX, islandY, dimension), 300, 300, worldEntity);
      this.baseBiome = biome;
      this.isCave = true;
      this.generateLevel();
   }

   @Override
   public void generateLevel() {
      CaveGeneration cg = new CaveGeneration(this, "deepswamprocktile", "deepswamprock");
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel());
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      int crate = ObjectRegistry.getObjectID("swampcrate");
      PresetGeneration presets = new PresetGeneration(this);
      GameEvents.triggerEvent(
         new GenerateCaveMiniBiomesEvent(this, cg),
         e -> {
            GenerationTools.generateRandomSmoothVeinsC(this, cg.random, 0.02F, 4, 15.0F, 25.0F, 3.0F, 5.0F, cax -> {
               cax.forEachTile(this, (level, tileX, tileYx) -> {
                  level.setTile(tileX, tileYx, TileRegistry.spiderNestID);
                  if (cg.random.getChance(0.95F)) {
                     level.setObject(tileX, tileYx, ObjectRegistry.cobWebID);
                  } else {
                     level.setObject(tileX, tileYx, 0);
                  }
               });
               cax.spawnMobs(this, cg.random, "smallswampcavespider", 4, 8, 1, 8);
            });
            GameObject crystalClusterSmall = ObjectRegistry.getObject("emeraldclustersmall");
            GenerationTools.generateRandomSmoothVeinsL(
               this,
               cg.random,
               0.005F,
               4,
               3.0F,
               5.0F,
               4.0F,
               6.0F,
               lgx -> {
                  CellAutomaton cax = lgx.doCellularAutomaton(cg.random);
                  cax.streamAliveOrdered().forEachOrdered(tilex -> {
                     cg.addIllegalCrateTile(tilex.x, tilex.y);
                     this.setTile(tilex.x, tilex.y, TileRegistry.getTileID("emeraldgravel"));
                     this.setObject(tilex.x, tilex.y, 0);
                  });
                  cax.streamAliveOrdered()
                     .forEachOrdered(
                        tilex -> {
                           if (this.getObjectID(tilex.x, tilex.y) == 0
                              && this.getObjectID(tilex.x - 1, tilex.y) == 0
                              && this.getObjectID(tilex.x + 1, tilex.y) == 0
                              && this.getObjectID(tilex.x, tilex.y - 1) == 0
                              && this.getObjectID(tilex.x, tilex.y + 1) == 0
                              && cg.random.getChance(0.1F)) {
                              int rotation = cg.random.nextInt(4);
                              Point[] clearPoints = new Point[]{new Point(-1, -1), new Point(1, -1)};
                              if (this.getRelativeAnd(
                                 tilex.x,
                                 tilex.y,
                                 PresetUtils.getRotatedPoints(0, 0, rotation, clearPoints),
                                 (tileX, tileYx) -> cax.isAlive(tileX, tileYx) && this.getObjectID(tileX, tileYx) == 0
                              )) {
                                 ObjectRegistry.getObject(ObjectRegistry.getObjectID("emeraldcluster")).placeObject(this, tilex.x, tilex.y, rotation, false);
                              }
                           }

                           if (cg.random.getChance(0.3F) && crystalClusterSmall.canPlace(this, tilex.x, tilex.y, 0, false) == null) {
                              crystalClusterSmall.placeObject(this, tilex.x, tilex.y, 0, false);
                           }
                        }
                     );
               }
            );
            GameTile swampRockTile = TileRegistry.getTile(TileRegistry.deepSwampRockID);
            GameObject tallGrass = ObjectRegistry.getObject(ObjectRegistry.getObjectID("deepswamptallgrass"));
            GenerationTools.generateRandomSmoothVeinsC(
               this, cg.random, 0.03F, 5, 4.0F, 10.0F, 3.0F, 5.0F, cells -> cells.forEachTile(this, (level, tileX, tileYx) -> {
                  swampRockTile.placeTile(level, tileX, tileYx, false);
                  this.setObject(tileX, tileYx, 0);
                  if (cg.random.getChance(0.85F) && tallGrass.canPlace(level, tileX, tileYx, 0, false) == null) {
                     tallGrass.placeObject(level, tileX, tileYx, 0, false);
                  }
               })
            );
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.04F, 2, 2.0F, 10.0F, 2.0F, 4.0F, TileRegistry.getTileID("lavatile"), 1.0F, true);
            int fishianSize = 75;
            int cobbleID = TileRegistry.puddleCobble;
            int edgePadding = 20;
            int centerTileX = cg.random.getIntBetween(edgePadding, this.tileWidth - edgePadding - fishianSize);
            int centerTileY = cg.random.getIntBetween(edgePadding, this.tileHeight - edgePadding - fishianSize);
            HashSet<Point> validTiles = new HashSet<>();
            Rectangle bounds = new Rectangle(centerTileX, centerTileY, centerTileX, centerTileY);
            LinesGeneration lg = new LinesGeneration(centerTileX, centerTileY).addRandomArms(cg.random, 16, fishianSize / 2 - 10, fishianSize / 2, 6.0F, 8.0F);
            CellAutomaton ca = lg.doCellularAutomaton(cg.random);
            ca.forEachTile(this, (level, tileX, tileYx) -> {
               if (bounds.x > tileX) {
                  bounds.x = tileX;
               }

               if (bounds.y > tileYx) {
                  bounds.y = tileYx;
               }

               if (bounds.width < tileX) {
                  bounds.width = tileX;
               }

               if (bounds.height < tileYx) {
                  bounds.height = tileYx;
               }

               validTiles.add(new Point(tileX, tileYx));
               this.setTile(tileX, tileYx, cobbleID);
               this.setObject(tileX, tileYx, 0);
            });
            bounds.width = bounds.width - bounds.x;
            bounds.height = bounds.height - bounds.y;
            presets.addOccupiedSpace(bounds);
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
                  Float point = new Float(cg.random.getIntBetween(minX, maxX), cg.random.getIntBetween(minY, maxY));
                  voronoiPoints.add(point);
               }
            }

            ArrayList<TriangleLine> voronoiLines = new ArrayList<>();
            DelaunayTriangulator.compute(voronoiPoints, false, voronoiLines);
            HashSet<Point> floorTiles = new HashSet<>();
            HashSet<Point> houseTiles = new HashSet<>();
            int stoneFloor = TileRegistry.deepSwampStoneFloorID;

            for (TriangleLine line : voronoiLines) {
               LinesGeneration.pathTiles(new java.awt.geom.Line2D.Float(line.p1, line.p2), true, (from, next) -> {
                  int tileX = next.x;
                  int tileYx = next.y;
                  if (this.isTileWithinBounds(tileX, tileYx)) {
                     if (validTiles.contains(new Point(tileX, tileYx))) {
                        if (cg.random.getChance(0.8F)) {
                           this.setTile(tileX, tileYx, stoneFloor);
                        }

                        this.setObject(tileX, tileYx, 0);
                        floorTiles.add(new Point(tileX, tileYx));
                     }
                  }
               });
            }

            AtomicInteger lootRotation = new AtomicInteger();
            ArrayList<Preset> housePresets = new ArrayList<>();
            housePresets.add(new FishianHousePreset1(cg.random, lootRotation));
            housePresets.add(new FishianHousePreset2(cg.random, lootRotation));

            for (Preset housePreset : housePresets) {
               housePreset.addCanApplyRectEachPredicate(
                  0,
                  0,
                  housePreset.width,
                  housePreset.height,
                  0,
                  (level, levelX, levelY, dir) -> !floorTiles.contains(new Point(levelX, levelY)) && validTiles.contains(new Point(levelX, levelY))
               );
               housePreset.addCustomApplyRectEach(-1, -1, housePreset.width + 2, housePreset.height + 2, 0, (level, levelX, levelY, dir, blackboard) -> {
                  houseTiles.add(new Point(levelX, levelY));
                  return null;
               });
            }

            while (!voronoiPoints.isEmpty()) {
               int pointsIndex = cg.random.nextInt(voronoiPoints.size());
               Float centerTile = voronoiPoints.remove(pointsIndex);
               if (!housePresets.isEmpty()
                  && bounds.contains(centerTile.x, centerTile.y)
                  && validTiles.contains(new Point((int)centerTile.x, (int)centerTile.y))) {
                  int presetIndex = cg.random.nextInt(housePresets.size());
                  Preset preset = housePresets.get(presetIndex);
                  int placeDir = cg.random.nextInt(4);

                  try {
                     preset = preset.rotate(PresetRotation.toRotationAngle(placeDir));
                  } catch (PresetRotateException var47) {
                  }

                  final Preset finalPreset = preset;
                  AreaFinder posFinder = new AreaFinder((int)centerTile.x, (int)centerTile.y, 5, true) {
                     @Override
                     public boolean checkPoint(int x, int y) {
                        return finalPreset.canApplyToLevelCentered(SwampDeepCaveLevel.this, x, y);
                     }
                  };
                  posFinder.runFinder();
                  if (posFinder.hasFound()) {
                     housePresets.remove(presetIndex);
                     Point tile = posFinder.getFirstFind();
                     GameBlackboard blackboard = new GameBlackboard();
                     finalPreset.applyToLevelCentered(this, tile.x, tile.y, blackboard);
                     Point doorTile = blackboard.get(Point.class, "doorTile");
                     if (doorTile != null) {
                        int angle = 0;
                        switch ((placeDir + 2) % 4) {
                           case 0:
                              angle = cg.random.getIntOffset(270, 45);
                              break;
                           case 1:
                              angle = cg.random.getIntOffset(0, 45);
                              break;
                           case 2:
                              angle = cg.random.getIntOffset(90, 45);
                              break;
                           case 3:
                              angle = cg.random.getIntOffset(180, 45);
                        }

                        Float dir = GameMath.getAngleDir(angle);
                        LinesGeneration.pathTilesBreak(
                           new java.awt.geom.Line2D.Float(doorTile.x, doorTile.y, doorTile.x + dir.x * 30.0F, doorTile.y + dir.y * 30.0F),
                           true,
                           (from, next) -> {
                              int tileX = next.x;
                              int tileYx = next.y;
                              if (!this.isTileWithinBounds(tileX, tileYx)) {
                                 return true;
                              }

                              if (floorTiles.contains(new Point(tileX, tileYx))) {
                                 return false;
                              }

                              this.setTile(tileX, tileYx, stoneFloor);
                              this.setObject(tileX, tileYx, 0);
                              floorTiles.add(new Point(tileX, tileYx));
                              return true;
                           }
                        );
                        continue;
                     }
                  }
               }

               if (!floorTiles.contains(new Point((int)centerTile.x, (int)centerTile.y))
                  && !houseTiles.contains(new Point((int)centerTile.x, (int)centerTile.y))) {
                  LinesGeneration lakeLG = new LinesGeneration((int)centerTile.x, (int)centerTile.y);
                  int lakeArms = cg.random.getIntBetween(5, 7);
                  int angle = cg.random.nextInt(360);
                  int anglePerArm = 360 / lakeArms;

                  for (int i = 0; i < lakeArms; i++) {
                     angle += cg.random.getIntOffset(anglePerArm, anglePerArm / 2);
                     Float lakeDir = GameMath.getAngleDir(angle);
                     AtomicReference<Point> armEndPointRef = new AtomicReference<>(new Point((int)centerTile.x, (int)centerTile.y));
                     LinesGeneration.pathTilesBreak(
                        new java.awt.geom.Line2D.Float(centerTile.x, centerTile.y, centerTile.x + lakeDir.x * resolution, centerTile.y + lakeDir.y * resolution),
                        true,
                        (from, next) -> {
                           int tileX = next.x;
                           int tileYx = next.y;
                           if (!this.isTileWithinBounds(tileX, tileYx)) {
                              return false;
                           }

                           if (floorTiles.contains(new Point(tileX, tileYx))) {
                              return false;
                           }

                           if (houseTiles.contains(new Point(tileX, tileYx))) {
                              return false;
                           }

                           armEndPointRef.set(new Point(tileX, tileYx));
                           return true;
                        }
                     );
                     Point armEndPoint = armEndPointRef.get();
                     float width = cg.random.getFloatBetween(2.0F, 3.0F);
                     lakeLG.addLine((int)centerTile.x, (int)centerTile.y, armEndPoint.x, armEndPoint.y, width);
                  }

                  GameObject cattail = ObjectRegistry.getObject("cattail");
                  GameObject reeds = ObjectRegistry.getObject("reeds");
                  CellAutomaton lakeCA = lakeLG.doCellularAutomaton(cg.random);
                  lakeCA.forEachTile(this, (level, tileX, tileYx) -> {
                     if (validTiles.contains(new Point(tileX, tileYx))) {
                        if (!floorTiles.contains(new Point(tileX, tileYx))) {
                           if (!houseTiles.contains(new Point(tileX, tileYx))) {
                              this.setTile(tileX, tileYx, TileRegistry.waterID);
                              this.setObject(tileX, tileYx, 0);
                              if (cg.random.getChance(0.1F) && reeds.canPlace(this, 0, tileX, tileYx, 0, false, false) == null) {
                                 reeds.placeObject(this, 0, tileX, tileYx, 0, false);
                              }

                              if (cg.random.getChance(0.1F) && cattail.canPlace(this, 0, tileX, tileYx, 0, false, false) == null) {
                                 cattail.placeObject(this, 0, tileX, tileYx, 0, false);
                              }
                           }
                        }
                     }
                  });

                  for (Point floorTile : floorTiles) {
                     for (int x = -1; x <= 1; x++) {
                        int tileX = floorTile.x + x;

                        for (int y = -1; y <= 1; y++) {
                           int tileY = floorTile.y + y;
                           Point tile = new Point(tileX, tileY);
                           if (!floorTiles.contains(tile) && !houseTiles.contains(tile)) {
                              this.setTile(tileX, tileY, cobbleID);
                           }
                        }
                     }
                  }

                  TicketSystemList<GameObject> randomObjects = new TicketSystemList<>();
                  randomObjects.addObject(20, ObjectRegistry.getObject("seashell"));
                  randomObjects.addObject(20, ObjectRegistry.getObject("seasnail"));
                  randomObjects.addObject(20, ObjectRegistry.getObject("seastar"));
                  randomObjects.addObject(25, ObjectRegistry.getObject("bamboodebris"));
                  randomObjects.addObject(25, ObjectRegistry.getObject("bambootree"));
                  randomObjects.addObject(100, ObjectRegistry.getObject("glowcoral"));

                  for (Point tile : validTiles) {
                     if (!floorTiles.contains(tile) && cg.random.getChance(0.01F)) {
                        GameObject obj = randomObjects.getRandomObject(cg.random);
                        if (obj.canPlace(this, 0, tile.x, tile.y, 0, false, true) == null) {
                           obj.placeObject(this, 0, tile.x, tile.y, 0, false);
                        }
                     }
                  }
               }
            }

            this.liquidManager.calculateShores();
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepswampcaverock"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepswampcaverocksmall"), 0.01F);
            GameObject grassObject = ObjectRegistry.getObject(ObjectRegistry.getObjectID("deepswampgrass"));
            GenerationTools.iterateLevel(
               this,
               (x, yx) -> this.getTileID(x, yx) == TileRegistry.deepSwampRockID && this.getObjectID(x, yx) == 0 && cg.random.getChance(0.6F),
               (x, yx) -> grassObject.placeObject(this, x, yx, 0, false)
            );
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveOresEvent(this, cg), e -> {
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("copperoredeepswamprock"));
         cg.generateOreVeins(0.1F, 3, 6, ObjectRegistry.getObjectID("ironoredeepswamprock"));
         cg.generateOreVeins(0.15F, 3, 6, ObjectRegistry.getObjectID("goldoredeepswamprock"));
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("tungstenoredeepswamprock"));
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("lifequartzdeepswamprock"));
         cg.generateOreVeins(0.17F, 3, 6, ObjectRegistry.getObjectID("myceliumoredeepswamprock"));
      });
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
      GameEvents.triggerEvent(
         new GenerateCaveStructuresEvent(this, cg, presets),
         e -> {
            this.preGeneratedStructures(cg, presets);
            AtomicInteger chestRoomRotation = new AtomicInteger();
            int chestRoomAmount = cg.random.getIntBetween(13, 18);

            for (int i = 0; i < chestRoomAmount; i++) {
               Preset chestRoom = new RandomCaveChestRoom(
                  cg.random, LootTablePresets.deepSwampCaveChest, chestRoomRotation, ChestRoomSet.deepSwampStone, ChestRoomSet.deepStone
               );
               chestRoom.replaceTile(TileRegistry.deepStoneFloorID, cg.random.getOneOf(TileRegistry.deepStoneFloorID, TileRegistry.deepStoneBrickFloorID));
               chestRoom.replaceTile(
                  TileRegistry.deepSwampStoneFloorID, cg.random.getOneOf(TileRegistry.deepSwampStoneFloorID, TileRegistry.deepSwampStoneBrickFloorID)
               );
               presets.findRandomValidPositionAndApply(cg.random, 5, chestRoom, 10, true, true);
            }

            int lootAreaAmount = cg.random.getIntBetween(5, 10);

            for (int i = 0; i < lootAreaAmount; i++) {
               Preset lootArea = new RandomLootAreaPreset(cg.random, 15, "deepswampstonecolumn", "swampdweller");
               presets.findRandomValidPositionAndApply(cg.random, 5, lootArea, 10, true, true);
            }

            AtomicInteger caveRuinsRotation = new AtomicInteger();
            int caveRuinsCount = cg.random.getIntBetween(25, 35);

            for (int i = 0; i < caveRuinsCount; i++) {
               WallSet wallSet = cg.random.getOneOf(WallSet.deepSwampStone, WallSet.deepStone);
               FurnitureSet furnitureSet = cg.random.getOneOf(FurnitureSet.oak, FurnitureSet.spruce);
               String floorStringID = cg.random.getOneOf("deepswampstonefloor", "deepswampstonebrickfloor");
               Preset room = cg.random
                  .getOneOf(CaveRuins.caveRuinGetters)
                  .get(cg.random, wallSet, furnitureSet, floorStringID, LootTablePresets.swampDeepCaveRuinsChest, caveRuinsRotation);
               presets.findRandomValidPositionAndApply(cg.random, 5, room, 10, true, true);
            }

            presets.findRandomValidPositionAndApply(cg.random, 200, new AgedChampionArenaPreset(), 2, true, true);
            cg.generateRandomCrates(0.03F, crate);
            this.postGeneratedStructures(cg, presets);
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      GenerationTools.checkValid(this);
   }

   @Override
   public boolean isDeepCaveLevel() {
      return true;
   }

   @Override
   public GameMessage getLocationMessage(int tileX, int tileY) {
      return new LocalMessage("biome", "deepcave", "biome", this.getBiome(tileX, tileY).getLocalization());
   }
}
