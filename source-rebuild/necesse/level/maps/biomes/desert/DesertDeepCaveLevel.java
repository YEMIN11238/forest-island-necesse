package necesse.level.maps.biomes.desert;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
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
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.CellAutomaton;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.LinesGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.furniturePresets.BedDresserPreset;
import necesse.level.maps.presets.furniturePresets.BenchPreset;
import necesse.level.maps.presets.furniturePresets.BookshelfClockPreset;
import necesse.level.maps.presets.furniturePresets.BookshelvesPreset;
import necesse.level.maps.presets.furniturePresets.CabinetsPreset;
import necesse.level.maps.presets.furniturePresets.DeskBookshelfPreset;
import necesse.level.maps.presets.furniturePresets.DinnerTablePreset;
import necesse.level.maps.presets.furniturePresets.DisplayStandClockPreset;
import necesse.level.maps.presets.furniturePresets.ModularDinnerTablePreset;
import necesse.level.maps.presets.furniturePresets.ModularTablesPreset;
import necesse.level.maps.presets.furniturePresets.SingleChestPreset;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class DesertDeepCaveLevel extends DesertCaveLevel {
   public DesertDeepCaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public DesertDeepCaveLevel(int islandX, int islandY, int dimension, WorldEntity worldEntity, Biome biome) {
      super(new LevelIdentifier(islandX, islandY, dimension), 300, 300, worldEntity);
      this.baseBiome = biome;
      this.isCave = true;
      this.generateLevel();
   }

   @Override
   public void generateLevel() {
      CaveGeneration cg = new CaveGeneration(this, "deepsandstonetile", "deepsandstonerock");
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.44F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveMiniBiomesEvent(this, cg), e -> {
         GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.05F, 2, 7.0F, 20.0F, 3.0F, 8.0F, TileRegistry.getTileID("lavatile"), 1.0F, true);
         this.liquidManager.calculateShores();
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepsandcaverock"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepsandcaverocksmall"), 0.01F);
      });
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveOresEvent(this, cg), e -> {
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("copperoredeepsandstonerock"));
         cg.generateOreVeins(0.25F, 3, 6, ObjectRegistry.getObjectID("ironoredeepsandstonerock"));
         cg.generateOreVeins(0.15F, 3, 6, ObjectRegistry.getObjectID("goldoredeepsandstonerock"));
         cg.generateOreVeins(0.17F, 3, 6, ObjectRegistry.getObjectID("ancientfossiloredeepsnowrock"));
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("lifequartzdeepsandstonerock"));
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
                  cg.random, LootTablePresets.deepDesertCaveChest, chestRoomRotation, ChestRoomSet.deepSandstone, ChestRoomSet.obsidian
               );
               chestRoom.replaceTile(TileRegistry.deepStoneFloorID, cg.random.getOneOf(TileRegistry.deepStoneFloorID, TileRegistry.deepStoneBrickFloorID));
               presets.findRandomValidPositionAndApply(cg.random, 5, chestRoom, 10, true, true);
            }

            AtomicInteger caveRuinsRotation = new AtomicInteger();
            int caveRuinsCount = cg.random.getIntBetween(25, 35);

            for (int i = 0; i < caveRuinsCount; i++) {
               WallSet wallSet = cg.random.getOneOf(WallSet.deepSandstone, WallSet.obsidian);
               FurnitureSet furnitureSet = cg.random.getOneOf(FurnitureSet.palm, FurnitureSet.spruce);
               String floorStringID = cg.random.getOneOf("deepstonefloor", "deepstonebrickfloor");
               Preset room = cg.random
                  .getOneOf(CaveRuins.caveRuinGetters)
                  .get(cg.random, wallSet, furnitureSet, floorStringID, LootTablePresets.desertDeepCaveRuinsChest, caveRuinsRotation);
               presets.findRandomValidPositionAndApply(cg.random, 5, room, 10, true, true);
            }

            int angle = cg.random.nextInt(360);
            Float dir = GameMath.getAngleDir(angle);
            float offCenterDistance = cg.random.getFloatBetween(0.0F, 0.4F);
            Point pos = new Point(
               this.tileWidth / 2 + (int)(dir.x * (this.tileWidth / 2.0F * offCenterDistance)),
               this.tileHeight / 2 + (int)(dir.y * (this.tileHeight / 2.0F * offCenterDistance))
            );
            float centerRange = 15.5F;
            LinesGeneration lg = new LinesGeneration(pos.x, pos.y, centerRange);
            int armAngle = cg.random.nextInt(360);
            int arms = 8;
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
                  armLG -> armLG.x2 < 10 || armLG.x2 > this.tileWidth - 10 || armLG.y2 < 10 || armLG.y2 > this.tileHeight - 10
               );
            }

            CellAutomaton ca = lg.doCellularAutomaton(cg.random);
            ca.cleanHardEdges();

            for (int x = pos.x - (int)Math.floor(centerRange); x <= pos.x + Math.ceil(centerRange); x++) {
               for (int y = pos.y - (int)Math.floor(centerRange); y <= pos.y + Math.ceil(centerRange); y++) {
                  if (pos.distance(x, y) <= centerRange) {
                     ca.setAlive(x, y);
                  }
               }
            }

            int sandBrickID = TileRegistry.getTileID("sandbrick");
            int woodFloorID = TileRegistry.getTileID("woodfloor");
            ca.forEachTile(this, (level, tileX, tileY) -> {
               if (cg.random.getChance(0.75F)) {
                  level.setTile(tileX, tileY, sandBrickID);
               } else {
                  level.setTile(tileX, tileY, woodFloorID);
               }

               level.setObject(tileX, tileY, 0);
            });
            ca.placeEdgeWalls(this, ObjectRegistry.getObjectID("deepsandstonewall"), true);
            ca.forEachTile(this, (level, tileX, tileY) -> {
               if (cg.random.getChance(0.02F)) {
                  GameObject breakObject = ObjectRegistry.getObject(cg.random.getOneOf("crate", "vase"));
                  if (breakObject.canPlace(level, tileX, tileY, 0, false) == null) {
                     breakObject.placeObject(level, tileX, tileY, 0, false);
                  }
               }
            });

            for (int x = pos.x - (int)Math.floor(centerRange); x <= pos.x + Math.ceil(centerRange); x++) {
               for (int y = pos.y - (int)Math.floor(centerRange); y <= pos.y + Math.ceil(centerRange); y++) {
                  if (pos.distance(x, y) <= centerRange && cg.random.getChance(0.05F)) {
                     GameObject breakObject = ObjectRegistry.getObject(cg.random.getOneOf("crate", "vase"));
                     if (breakObject.canPlace(this, x, y, 0, false) == null) {
                        breakObject.placeObject(this, x, y, 0, false);
                     }
                  }
               }
            }

            LootTable templeChestLootTable = new LootTable();

            for (int i = 0; i < 5; i++) {
               templeChestLootTable.items.add(LootTablePresets.desertDeepCrate);
            }

            TicketSystemList<Preset> templeFurniture = new TicketSystemList<>();
            templeFurniture.addObject(100, new BedDresserPreset(FurnitureSet.palm, 2));
            templeFurniture.addObject(100, new BenchPreset(FurnitureSet.palm, 2));
            templeFurniture.addObject(100, new BookshelfClockPreset(FurnitureSet.palm, 2));
            templeFurniture.addObject(100, new BookshelvesPreset(FurnitureSet.palm, 2, 3));
            templeFurniture.addObject(100, new CabinetsPreset(FurnitureSet.palm, 2, 3));
            templeFurniture.addObject(100, new DeskBookshelfPreset(FurnitureSet.palm, 2));
            templeFurniture.addObject(100, new DinnerTablePreset(FurnitureSet.palm, 2));
            templeFurniture.addObject(100, new DisplayStandClockPreset(FurnitureSet.palm, 2, cg.random, null));
            templeFurniture.addObject(100, new ModularDinnerTablePreset(FurnitureSet.palm, 2, 1));
            templeFurniture.addObject(100, new ModularTablesPreset(FurnitureSet.palm, 2, 2, true));
            templeFurniture.addObject(100, new SingleChestPreset(FurnitureSet.palm, 2, cg.random, templeChestLootTable));
            ca.placeFurniturePresets(templeFurniture, 0.4F, this, cg.random);
            int columnsCount = cg.random.getIntBetween(6, 8);
            int anglePerColumn = 360 / columnsCount;
            int columnAngle = cg.random.nextInt(360);
            float columnDistance = centerRange - centerRange / 3.0F;
            int columnID = ObjectRegistry.getObjectID("deepsandstonecolumn");

            for (int i = 0; i < columnsCount; i++) {
               columnAngle += cg.random.getIntOffset(anglePerColumn, anglePerColumn / 5);
               Float columnDir = GameMath.getAngleDir(columnAngle);
               this.setObject(pos.x + (int)(columnDir.x * columnDistance), pos.y + (int)(columnDir.y * columnDistance), columnID);
            }

            this.setObject(pos.x, pos.y, ObjectRegistry.getObjectID("templepedestal"));
            cg.generateRandomCrates(0.03F, ObjectRegistry.getObjectID("crate"), ObjectRegistry.getObjectID("vase"));
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
