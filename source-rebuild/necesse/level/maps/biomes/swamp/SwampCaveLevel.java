package necesse.level.maps.biomes.swamp;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
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
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.generationModules.RiverGeneration;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.RandomLootAreaPreset;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;
import necesse.level.maps.presets.worldStructures.evilWitchCastle.EvilWitchCastlePreset;

public class SwampCaveLevel extends SwampSurfaceLevel {
   public SwampCaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public SwampCaveLevel(int islandX, int islandY, int dimension, WorldEntity worldEntity, Biome biome) {
      super(new LevelIdentifier(islandX, islandY, dimension), 300, 300, worldEntity);
      this.baseBiome = biome;
      this.isCave = true;
      this.generateLevel();
   }

   public void generateLevel() {
      CaveGeneration cg = new CaveGeneration(this, "swamprocktile", "swamprock");
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel());
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      int crate = ObjectRegistry.getObjectID("swampcrate");
      int trackObject = ObjectRegistry.getObjectID("minecarttrack");
      LinkedList<Mob> minecartsGenerated = new LinkedList<>();
      GameEvents.triggerEvent(
         new GenerateCaveMiniBiomesEvent(this, cg),
         e -> {
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.02F, 2, 2.0F, 10.0F, 2.0F, 4.0F, TileRegistry.getTileID("lavatile"), 1.0F, true);
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.02F, 2, 2.0F, 10.0F, 2.0F, 4.0F, TileRegistry.getTileID("watertile"), 1.0F, true);
            int riverCount = cg.random.getIntBetween(4, 6);

            for (int i = 0; i < riverCount; i++) {
               RiverGeneration.generateOneSmallRiver(
                  cg.random, this, "cavewatergrass", r -> r.getOneOf(RiverGeneration.SWAMP_BRIDGE, RiverGeneration.WOOD_BRIDGE)
               );
            }

            this.liquidManager.calculateShores();
            GameObject cattail = ObjectRegistry.getObject("cattail");
            GameObject reeds = ObjectRegistry.getObject("reeds");
            GenerationTools.generateRandomVeins(this, cg.random, 0.5F, 12, 20, (level, tileX, tileY) -> {
               if (cg.random.getChance(0.2F) && reeds.canPlace(level, tileX, tileY, 0, false) == null) {
                  reeds.placeObject(level, tileX, tileY, 0, false);
               }

               if (cg.random.getChance(0.2F) && cattail.canPlace(level, tileX, tileY, 0, false) == null) {
                  cattail.placeObject(level, tileX, tileY, 0, false);
               }
            });
            GameObject swampSpore = ObjectRegistry.getObject("swampspore");
            GenerationTools.generateRandomPoints(this, cg.random, 0.02F, 10, initialPos -> {
               LinesGeneration lg = new LinesGeneration(initialPos.x, initialPos.y).addRandomArms(cg.random, 4, 8.0F, 12.0F, 8.0F, 12.0F);
               CellAutomaton ca = lg.doCellularAutomaton(cg.random);
               ca.forEachTile(this, (level, tileX, tileY) -> level.setObject(tileX, tileY, 0));

               for (LinesGeneration line : lg.getLines()) {
                  swampSpore.placeObject(this, line.x2, line.y2, 0, false);
               }
            });
            GenerationTools.generateRandomSmoothVeinsC(this, cg.random, 0.02F, 5, 7.0F, 15.0F, 3.0F, 5.0F, ca -> {
               ca.forEachTile(this, (level, tileX, tileY) -> {
                  level.setTile(tileX, tileY, TileRegistry.spiderNestID);
                  if (cg.random.getChance(0.95F)) {
                     level.setObject(tileX, tileY, ObjectRegistry.cobWebID);
                  } else {
                     level.setObject(tileX, tileY, 0);
                  }
               });
               ca.spawnMobs(this, cg.random, "swampcavespider", 4, 8, 1, 4);
            });
            GameTile mudTile = TileRegistry.getTile(TileRegistry.mudID);
            GameObject wildMushroom = ObjectRegistry.getObject(ObjectRegistry.getObjectID("wildmushroom"));
            GameObject thorns = ObjectRegistry.getObject(ObjectRegistry.getObjectID("thorns"));
            GenerationTools.generateRandomSmoothVeinsC(this, cg.random, 0.1F, 5, 4.0F, 10.0F, 3.0F, 5.0F, cells -> {
               boolean addThorns = cg.random.getChance(0.3F);
               cells.forEachTile(this, (level, tileX, tileY) -> {
                  mudTile.placeTile(level, tileX, tileY, false);
                  if (this.getObjectID(tileX, tileY) == 0) {
                     if (addThorns) {
                        if (cg.random.getChance(0.85F) && thorns.canPlace(level, tileX, tileY, 0, false) == null) {
                           thorns.placeObject(level, tileX, tileY, 0, false);
                        }
                     } else if (cg.random.getChance(0.05F) && wildMushroom.canPlace(level, tileX, tileY, 0, false) == null) {
                        wildMushroom.placeObject(level, tileX, tileY, 0, false);
                     }
                  }
               });
            });
            GenerationTools.generateRandomPoints(this, cg.random, 0.01F, 15, p -> {
               LinesGeneration lg = new LinesGeneration(p.x, p.y);
               ArrayList<LinesGeneration> tntArms = new ArrayList<>();
               int armAngle = cg.random.nextInt(4) * 90;
               int arms = cg.random.getIntBetween(3, 10);

               for (int i = 0; i < arms; i++) {
                  lg = lg.addArm(cg.random.getIntOffset(armAngle, 20), cg.random.getIntBetween(5, 25), 3.0F);
                  tntArms.add(lg);
                  int angleChange = cg.random.getOneOfWeighted(Integer.class, 15, 0, 5, 90, 5, -90);
                  armAngle += angleChange;
               }

               CellAutomaton ca = lg.doCellularAutomaton(cg.random);
               ca.forEachTile(this, (level, tileX, tileY) -> {
                  if (level.isSolidTile(tileX, tileY)) {
                     level.setObject(tileX, tileY, 0);
                  }

                  if (cg.random.getChance(0.05)) {
                     level.setObject(tileX, tileY, crate);
                  }
               });
               ArrayList<Point> trackTiles = new ArrayList<>();
               lg.getRoot().recursiveLines(lg2 -> {
                  GameLinkedList<Point> tilesx = new GameLinkedList<>();
                  LinesGeneration.pathTiles(lg2.getTileLine(), true, (from, nextxx) -> tilesx.add(nextxx));

                  for (GameLinkedList<Point>.Element el : tilesx.elements()) {
                     Point current = (Point)el.object;
                     if (this.isTileWithinBounds(current.x, current.y, 2)) {
                        trackTiles.add(current);
                        GameLinkedList<Point>.Element nextEl = el.next();
                        if (nextEl != null) {
                           Point nextx = (Point)nextEl.object;
                           if (nextx.x < current.x) {
                              this.setObject(current.x, current.y, trackObject, 3);
                           } else if (nextx.x > current.x) {
                              this.setObject(current.x, current.y, trackObject, 1);
                           } else if (nextx.y < current.y) {
                              this.setObject(current.x, current.y, trackObject, 0);
                           } else if (nextx.y > current.y) {
                              this.setObject(current.x, current.y, trackObject, 2);
                           }
                        } else {
                           GameLinkedList<Point>.Element prevEl = el.prev();
                           if (prevEl != null) {
                              Point prev = (Point)prevEl.object;
                              if (prev.x < current.x) {
                                 this.setObject(current.x, current.y, trackObject, 1);
                              } else if (prev.x > current.x) {
                                 this.setObject(current.x, current.y, trackObject, 3);
                              } else if (prev.y < current.y) {
                                 this.setObject(current.x, current.y, trackObject, 2);
                              } else if (prev.y > current.y) {
                                 this.setObject(current.x, current.y, trackObject, 0);
                              }
                           } else {
                              this.setObject(current.x, current.y, trackObject, 0);
                           }
                        }
                     }
                  }

                  return true;
               });
               int minecartCount = cg.random.getOneOfWeighted(Integer.class, 100, 0, 200, 1, 50, 2);

               for (int i = 0; i < minecartCount && !trackTiles.isEmpty(); i++) {
                  int index = cg.random.nextInt(trackTiles.size());
                  Point next = trackTiles.remove(index);
                  Mob minecart = MobRegistry.getMob("minecart", this);
                  this.entityManager.addMob(minecart, next.x * 32 + 16, next.y * 32 + 16);
                  minecartsGenerated.add(minecart);
               }

               int tntCount = cg.random.getOneOfWeighted(Integer.class, 100, 0, 200, 1, 50, 2);

               for (int i = 0; i < tntCount && !tntArms.isEmpty(); i++) {
                  int index = cg.random.nextInt(tntArms.size());
                  LinesGeneration next = tntArms.remove(index);
                  int wireLength = cg.random.getIntBetween(10, 14) * cg.random.getOneOf(1, -1);
                  float lineLength = (float)new Point(next.x1, next.y1).distance(next.x2, next.y2);
                  Float dir = GameMath.normalize(next.x1 - next.x2, next.y1 - next.y2);
                  float linePointLength = cg.random.getFloatBetween(0.0F, lineLength);
                  Float linePoint = new Float(next.x2 + dir.x * linePointLength, next.y2 + dir.y * linePointLength);
                  Float leverPoint = GameMath.getPerpendicularPoint(linePoint, 2.0F * Math.signum(wireLength), dir);
                  Float tntPoint = GameMath.getPerpendicularPoint(linePoint, wireLength, dir);
                  java.awt.geom.Line2D.Float wireLine = new java.awt.geom.Line2D.Float(leverPoint, tntPoint);
                  LinkedList<Point> tiles = new LinkedList<>();
                  LinesGeneration.pathTiles(wireLine, true, (fromTile, nextTile) -> tiles.add(nextTile));

                  for (Point tile : tiles) {
                     this.wireManager.setWire(tile.x, tile.y, 0, true);
                     if (this.getObject(tile.x, tile.y).isSolid) {
                        this.setObject(tile.x, tile.y, 0);
                     }
                  }

                  Point first = tiles.getFirst();
                  Point last = tiles.getLast();
                  this.setObject(first.x, first.y, ObjectRegistry.getObjectID("rocklever"));
                  this.setObject(last.x, last.y, ObjectRegistry.getObjectID("tnt"));
               }
            });
            this.liquidManager.calculateShores();
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("swampcaverock"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("swampcaverocksmall"), 0.01F);
            GameObject grassObject = ObjectRegistry.getObject(ObjectRegistry.getObjectID("swampgrass"));
            GenerationTools.iterateLevel(
               this,
               (x, y) -> this.getTileID(x, y) == TileRegistry.swampRockID && this.getObjectID(x, y) == 0 && cg.random.getChance(0.6F),
               (x, y) -> grassObject.placeObject(this, x, y, 0, false)
            );
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveOresEvent(this, cg), e -> {
         cg.generateOreVeins(0.3F, 3, 6, ObjectRegistry.getObjectID("ivyoreswamp"));
         cg.generateOreVeins(0.3F, 3, 6, ObjectRegistry.getObjectID("copperoreswamp"));
         cg.generateOreVeins(0.25F, 3, 6, ObjectRegistry.getObjectID("ironoreswamp"));
         cg.generateOreVeins(0.15F, 3, 6, ObjectRegistry.getObjectID("goldoreswamp"));
      });
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
      PresetGeneration presets = new PresetGeneration(this);
      GameEvents.triggerEvent(
         new GenerateCaveStructuresEvent(this, cg, presets),
         e -> {
            this.preGeneratedStructures(cg, presets);
            AtomicInteger chestRoomRotation = new AtomicInteger();
            int chestRoomAmount = cg.random.getIntBetween(13, 18);

            for (int i = 0; i < chestRoomAmount; i++) {
               Preset chestRoom = new RandomCaveChestRoom(
                  cg.random, LootTablePresets.swampCaveChest, chestRoomRotation, ChestRoomSet.swampStone, ChestRoomSet.wood
               );
               chestRoom.replaceTile(TileRegistry.stoneFloorID, cg.random.getOneOf(TileRegistry.stoneFloorID, TileRegistry.stoneBrickFloorID));
               chestRoom.replaceTile(TileRegistry.swampStoneFloorID, cg.random.getOneOf(TileRegistry.swampStoneFloorID, TileRegistry.swampStoneBrickFloorID));
               presets.findRandomValidPositionAndApply(cg.random, 5, chestRoom, 10, true, true);
            }

            int lootAreaAmount = cg.random.getIntBetween(5, 10);

            for (int i = 0; i < lootAreaAmount; i++) {
               Preset lootArea = new RandomLootAreaPreset(cg.random, 15, "swampstonecolumn", "swampzombie");
               presets.findRandomValidPositionAndApply(cg.random, 5, lootArea, 10, true, true);
            }

            AtomicInteger caveRuinsRotation = new AtomicInteger();
            int caveRuinsCount = cg.random.getIntBetween(25, 35);

            for (int i = 0; i < caveRuinsCount; i++) {
               WallSet wallSet = cg.random.getOneOf(WallSet.swampStone, WallSet.wood);
               FurnitureSet furnitureSet = cg.random.getOneOf(FurnitureSet.oak, FurnitureSet.spruce);
               String floorStringID = cg.random.getOneOf("woodfloor", "woodfloor", "swampstonefloor", "swampstonebrickfloor");
               Preset room = cg.random
                  .getOneOf(CaveRuins.caveRuinGetters)
                  .get(cg.random, wallSet, furnitureSet, floorStringID, LootTablePresets.swampCaveRuinsChest, caveRuinsRotation);
               presets.findRandomValidPositionAndApply(cg.random, 5, room, 10, true, true);
            }

            cg.generateRandomCrates(0.03F, crate);
            presets.findRandomValidPositionAndApply(cg.random, 100, new EvilWitchCastlePreset(cg.random), 10, false, false);
            this.postGeneratedStructures(cg, presets);
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      GenerationTools.checkValid(this);

      for (Mob mob : minecartsGenerated) {
         if (this.getObjectID(mob.getTileX(), mob.getTileY()) != trackObject) {
            mob.remove();
         }
      }
   }

   protected void preGeneratedStructures(CaveGeneration cg, PresetGeneration presets) {
   }

   protected void postGeneratedStructures(CaveGeneration cg, PresetGeneration presets) {
   }

   @Override
   public boolean isBasicCaveLevel() {
      return true;
   }

   @Override
   public GameMessage getLocationMessage(int tileX, int tileY) {
      return new LocalMessage("biome", "cave", "biome", this.getBiome(tileX, tileY).getLocalization());
   }

   @Override
   public float getLiquidSaltWaterSinkRate() {
      return 4.0F;
   }

   @Override
   public float getLiquidFreshWaterSinkRate() {
      return 10.0F;
   }

   @Override
   public float getLiquidMobSinkRate() {
      return 10.0F;
   }
}
