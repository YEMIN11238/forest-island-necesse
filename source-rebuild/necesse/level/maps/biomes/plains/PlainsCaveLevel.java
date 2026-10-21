package necesse.level.maps.biomes.plains;

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
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.PortalObjectEntity;
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
import necesse.level.maps.presets.ChieftainsArenaPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.RandomLootAreaPreset;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.modularPresets.runeboundTribePreset.RuneboundTribePreset;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class PlainsCaveLevel extends PlainsSurfaceLevel {
   public PlainsCaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public PlainsCaveLevel(int islandX, int islandY, int dimension, WorldEntity worldEntity, Biome biome) {
      super(new LevelIdentifier(islandX, islandY, dimension), 300, 300, worldEntity);
      this.baseBiome = biome;
      this.isCave = true;
      this.generateLevel();
   }

   public void generateLevel() {
      CaveGeneration cg = new CaveGeneration(this, "graniterocktile", "graniterock");
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel());
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.02F, 2, 2.0F, 10.0F, 2.0F, 4.0F, TileRegistry.getTileID("lavatile"), 1.0F, true);
      GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.02F, 2, 2.0F, 10.0F, 2.0F, 4.0F, TileRegistry.getTileID("watertile"), 1.0F, true);
      int riverCount = cg.random.getIntBetween(4, 6);

      for (int i = 0; i < riverCount; i++) {
         RiverGeneration.generateOneSmallRiver(cg.random, this, "cavewatergrass", r -> r.getOneOf(RiverGeneration.WOOD_BRIDGE, RiverGeneration.STONE_BRIDGE));
      }

      this.liquidManager.calculateShores();
      PresetGeneration presets = new PresetGeneration(this);
      ChieftainsArenaPreset arenaPreset = new ChieftainsArenaPreset();
      int angle = cg.random.nextInt(360);
      Float dir = GameMath.getAngleDir(angle);
      float offCenterDistance = cg.random.getFloatBetween(0.0F, 0.4F);
      Point centerPos = new Point(
         this.tileWidth / 2 + (int)(dir.x * (this.tileWidth / 2 * offCenterDistance)),
         this.tileHeight / 2 + (int)(dir.y * (this.tileHeight / 2 * offCenterDistance))
      );
      float centerRange = 15.5F;
      LinesGeneration lg = new LinesGeneration(centerPos.x, centerPos.y, centerRange);
      int armAngle = cg.random.nextInt(360);
      int arms = 3;
      int anglePerArm = 360 / arms;

      for (int i = 0; i < arms; i++) {
         armAngle += anglePerArm;
         lg.addMultiArm(
            cg.random,
            armAngle,
            15,
            cg.random.getIntBetween(this.tileWidth / 2, (int)(this.tileWidth / 1.5F)),
            5.0F,
            10.0F,
            7.0F,
            8.0F,
            armLG -> !this.isTileWithinBounds(armLG.x2, armLG.y2, 10)
         );
      }

      CellAutomaton ca = lg.doCellularAutomaton(cg.random);
      ca.cleanHardEdges();

      for (int x = centerPos.x - (int)Math.floor(centerRange); x <= centerPos.x + Math.ceil(centerRange); x++) {
         for (int y = centerPos.y - (int)Math.floor(centerRange); y <= centerPos.y + Math.ceil(centerRange); y++) {
            if (centerPos.distance(x, y) <= centerRange) {
               ca.setAlive(x, y);
            }
         }
      }

      int gravelID = TileRegistry.getTileID("graveltile");
      int graniteTileID = TileRegistry.getTileID("graniterocktile");
      ca.forEachTile(this, (level, tileX, tileY) -> {
         if (cg.random.getChance(0.25F)) {
            level.setTile(tileX, tileY, gravelID);
         } else {
            level.setTile(tileX, tileY, graniteTileID);
         }

         level.setObject(tileX, tileY, 0);
      });

      for (int x = centerPos.x - (int)Math.floor(centerRange); x <= centerPos.x + Math.ceil(centerRange); x++) {
         for (int y = centerPos.y - (int)Math.floor(centerRange); y <= centerPos.y + Math.ceil(centerRange); y++) {
            if (centerPos.distance(x, y) <= centerRange && cg.random.getChance(0.05F)) {
               GameObject breakObject = ObjectRegistry.getObject(cg.random.getOneOf("crate", "vase"));
               if (breakObject.canPlace(this, x, y, 0, false) == null) {
                  breakObject.placeObject(this, x, y, 0, false);
               }
            }
         }
      }

      presets.addOccupiedSpace(centerPos.x - arenaPreset.width / 2, centerPos.y - arenaPreset.height / 2, arenaPreset.width, arenaPreset.height);
      final int crateObjectID = ObjectRegistry.getObjectID("crate");
      GameEvents.triggerEvent(new GenerateCaveMiniBiomesEvent(this, cg), e -> {
         GenerationTools.generateRandomPoints(this, cg.random, 0.01F, 15, p -> {
            LinesGeneration lgx = new LinesGeneration(p.x, p.y);
            ArrayList<LinesGeneration> tntArms = new ArrayList<>();
            int armAnglex = cg.random.nextInt(4) * 90;
            int armsx = cg.random.getIntBetween(3, 10);

            for (int i = 0; i < armsx; i++) {
               lgx = lgx.addArm(cg.random.getIntOffset(armAnglex, 20), cg.random.getIntBetween(5, 25), 3.0F);
               tntArms.add(lgx);
               int angleChange = cg.random.getOneOfWeighted(Integer.class, 15, 0, 5, 90, 5, -90);
               armAnglex += angleChange;
            }

            CellAutomaton cax = lgx.doCellularAutomaton(cg.random);
            cax.forEachTile(this, (level, tileX, tileY) -> {
               if (level.isSolidTile(tileX, tileY)) {
                  level.setObject(tileX, tileY, 0);
               }

               if (cg.random.getChance(0.05)) {
                  level.setObject(tileX, tileY, crateObjectID);
               }
            });
            int tntCount = cg.random.getOneOfWeighted(Integer.class, 100, 0, 200, 1, 50, 2);

            for (int i = 0; i < tntCount && !tntArms.isEmpty(); i++) {
               int index = cg.random.nextInt(tntArms.size());
               LinesGeneration next = tntArms.remove(index);
               int wireLength = cg.random.getIntBetween(10, 14) * cg.random.getOneOf(1, -1);
               float lineLength = (float)new Point(next.x1, next.y1).distance(next.x2, next.y2);
               Float dirx = GameMath.normalize(next.x1 - next.x2, next.y1 - next.y2);
               float linePointLength = cg.random.getFloatBetween(0.0F, lineLength);
               Float linePoint = new Float(next.x2 + dirx.x * linePointLength, next.y2 + dirx.y * linePointLength);
               Float leverPoint = GameMath.getPerpendicularPoint(linePoint, 2.0F * Math.signum(wireLength), dirx);
               Float tntPoint = GameMath.getPerpendicularPoint(linePoint, wireLength, dirx);
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
         GenerationTools.generateRandomSmoothVeinsL(this, cg.random, 0.002F, 4, 4.0F, 7.0F, 4.0F, 6.0F, lgx -> {
            CellAutomaton cax = lgx.doCellularAutomaton(cg.random);
            ArrayList<Point> validChestTiles = new ArrayList<>();
            cax.streamAliveOrdered().forEachOrdered(tile -> {
               cg.addIllegalCrateTile(tile.x, tile.y);
               this.setTile(tile.x, tile.y, TileRegistry.mudID);
               this.setObject(tile.x, tile.y, 0);
               validChestTiles.add(new Point(tile));
               GameTile gravelTile = TileRegistry.getTile(TileRegistry.gravelID);
               if (cg.random.getChance(0.3F) && gravelTile.canPlace(this, tile.x, tile.y, false) == null) {
                  gravelTile.placeTile(this, tile.x, tile.y, false);
               }
            });
            Point chestTile = cg.random.getOneOf(validChestTiles);
            if (chestTile != null) {
               this.setObject(chestTile.x, chestTile.y, ObjectRegistry.getObjectID("barrel"));
               LootTablePresets.bearBarrel.applyToLevel(cg.random, 1.0F, this, chestTile.x, chestTile.y);
               Mob grizzlyBear = MobRegistry.getMob("grizzlybear", this);
               grizzlyBear.canDespawn = false;
               Point spawnPos = PortalObjectEntity.getTeleportDestinationAroundObject(this, grizzlyBear, chestTile.x, chestTile.y, true);
               if (spawnPos == null) {
                  spawnPos = new Point(chestTile.x * 32 + 16, chestTile.y * 32 + 16);
               }

               grizzlyBear.onSpawned(spawnPos.x, spawnPos.y);
               this.entityManager.addMob(grizzlyBear, spawnPos.x, spawnPos.y);
            }
         });
         this.liquidManager.calculateShores();
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("smallrunestone"), 5.0E-4F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("granitecaverock"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("granitecaverocksmall"), 0.01F);
      });
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveOresEvent(this, cg), e -> {
         cg.generateOreVeins(0.45F, 3, 6, ObjectRegistry.getObjectID("copperoregraniterock"));
         cg.generateOreVeins(0.35F, 3, 6, ObjectRegistry.getObjectID("ironoregraniterock"));
         cg.generateOreVeins(0.1F, 3, 6, ObjectRegistry.getObjectID("goldoregraniterock"));
      });
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
      GameEvents.triggerEvent(
         new GenerateCaveStructuresEvent(this, cg, presets),
         e -> {
            this.preGeneratedStructures(cg, presets);
            AtomicInteger chestRoomRotation = new AtomicInteger();
            int chestRoomAmount = cg.random.getIntBetween(16, 20);

            for (int i = 0; i < chestRoomAmount; i++) {
               Preset chestRoom = new RandomCaveChestRoom(
                  cg.random, LootTablePresets.plainsCaveChest, chestRoomRotation, ChestRoomSet.dryad, ChestRoomSet.granite
               );
               chestRoom.replaceTile(TileRegistry.stoneFloorID, cg.random.getOneOf(TileRegistry.stoneFloorID, TileRegistry.stoneBrickFloorID));
               presets.findRandomValidPositionAndApply(cg.random, 5, chestRoom, 10, true, true);
            }

            int lootAreaAmount = cg.random.getIntBetween(5, 10);

            for (int i = 0; i < lootAreaAmount; i++) {
               TicketSystemList<String> mobs = new TicketSystemList<>();
               mobs.addObject(100000, "trenchcoatgoblinstacked", true);
               mobs.addObject(100, "goblin");
               Preset lootArea = new RandomLootAreaPreset(cg.random, 15, "stonecolumn", mobs);
               presets.findRandomValidPositionAndApply(cg.random, 5, lootArea, 10, true, true);
            }

            AtomicInteger caveRuinsRotation = new AtomicInteger();
            int caveRuinsCount = cg.random.getIntBetween(25, 35);

            for (int i = 0; i < caveRuinsCount; i++) {
               WallSet wallSet = cg.random.getOneOf(WallSet.granite, WallSet.dryad);
               FurnitureSet furnitureSet = cg.random.getOneOf(FurnitureSet.oak, FurnitureSet.spruce);
               String floorStringID = cg.random.getOneOf("dryadfloor", "dryadfloor", "graniterock", "granitefloor");
               Preset room = cg.random
                  .getOneOf(CaveRuins.caveRuinGetters)
                  .get(cg.random, wallSet, furnitureSet, floorStringID, LootTablePresets.plainsCaveRuinsChest, caveRuinsRotation);
               presets.findRandomValidPositionAndApply(cg.random, 5, room, 10, true, true);
            }

            RuneboundTribePreset.generateRuneboundTribeOnLevel(this, cg.random, presets);
            arenaPreset.applyToLevel(this, centerPos.x - arenaPreset.width / 2, centerPos.y - arenaPreset.height / 2);
            this.postGeneratedStructures(cg, presets);
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      GenerationTools.checkValid(this);
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
