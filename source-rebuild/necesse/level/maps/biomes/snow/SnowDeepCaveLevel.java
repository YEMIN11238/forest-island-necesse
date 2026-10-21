package necesse.level.maps.biomes.snow;

import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.GameEvents;
import necesse.engine.events.worldGeneration.GenerateCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GenerateCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GenerateCaveOresEvent;
import necesse.engine.events.worldGeneration.GenerateCaveStructuresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveOresEvent;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.presets.MusicPlayerPreset2;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.RandomCaveChestRoom;
import necesse.level.maps.presets.caveRooms.CaveRuins;
import necesse.level.maps.presets.set.ChestRoomSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.WallSet;

public class SnowDeepCaveLevel extends SnowCaveLevel {
   public SnowDeepCaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public SnowDeepCaveLevel(int islandX, int islandY, int dimension, WorldEntity worldEntity, Biome biome) {
      super(new LevelIdentifier(islandX, islandY, dimension), 300, 300, worldEntity);
      this.baseBiome = biome;
      this.isCave = true;
      this.generateLevel();
   }

   @Override
   public void generateLevel() {
      CaveGeneration cg = new CaveGeneration(this, "deepsnowrocktile", "deepsnowrock");
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.44F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveMiniBiomesEvent(this, cg), e -> {
         GameTile deepIceTile = TileRegistry.getTile("deepicetile");
         GenerationTools.generateRandomSmoothVeins(this, cg.random, 0.06F, 2, 7.0F, 20.0F, 3.0F, 8.0F, (level, tileX, tileY) -> {
            deepIceTile.placeTile(level, tileX, tileY, false);
            level.setObject(tileX, tileY, 0);
         });
         this.liquidManager.calculateShores();
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepsnowcaverock"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepsnowcaverocksmall"), 0.01F);
         int icicleTrigger = ObjectRegistry.getObjectID("fallingicicletrigger");
         GenerationTools.generateRandomPoints(this, cg.random, 0.5F, veinPos -> {
            if (this.getObjectID(veinPos.x, veinPos.y) == 0) {
               GenerationTools.placeRandomVein(this, cg.random, veinPos.x, veinPos.y, 7, 20, -1, -1, 0.0F, icicleTrigger, -1, 0.4F, false, false);
            }
         });
      });
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveOresEvent(this, cg), e -> {
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("copperoredeepsnowrock"));
         cg.generateOreVeins(0.25F, 3, 6, ObjectRegistry.getObjectID("ironoredeepsnowrock"));
         cg.generateOreVeins(0.15F, 3, 6, ObjectRegistry.getObjectID("goldoredeepsnowrock"));
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("tungstenoredeepsnowrock"));
         cg.generateOreVeins(0.05F, 3, 6, ObjectRegistry.getObjectID("lifequartzdeepsnowrock"));
         cg.generateOreVeins(0.17F, 3, 6, ObjectRegistry.getObjectID("glacialoredeepsnowrock"));
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
                  cg.random, LootTablePresets.deepSnowCaveChest, chestRoomRotation, ChestRoomSet.deepStone, ChestRoomSet.deepSnowStone
               );
               chestRoom.replaceTile(TileRegistry.deepStoneFloorID, cg.random.getOneOf(TileRegistry.deepStoneFloorID, TileRegistry.deepStoneBrickFloorID));
               chestRoom.replaceTile(
                  TileRegistry.deepSnowStoneFloorID, cg.random.getOneOf(TileRegistry.deepSnowStoneFloorID, TileRegistry.deepSnowStoneBrickFloorID)
               );
               presets.findRandomValidPositionAndApply(cg.random, 5, chestRoom, 10, true, true);
            }

            AtomicInteger caveRuinsRotation = new AtomicInteger();
            int caveRuinsCount = cg.random.getIntBetween(25, 35);

            for (int i = 0; i < caveRuinsCount; i++) {
               WallSet wallSet = cg.random.getOneOf(WallSet.deepStone, WallSet.deepSnowStone);
               FurnitureSet furnitureSet = cg.random.getOneOf(FurnitureSet.pine, FurnitureSet.spruce);
               String floorStringID = cg.random.getOneOf("deepsnowstonefloor", "deepsnowstonebrickfloor");
               Preset room = cg.random
                  .getOneOf(CaveRuins.caveRuinGetters)
                  .get(cg.random, wallSet, furnitureSet, floorStringID, LootTablePresets.snowDeepCaveRuinsChest, caveRuinsRotation);
               presets.findRandomValidPositionAndApply(cg.random, 5, room, 10, true, true);
            }

            presets.findRandomValidPositionAndApply(cg.random, 50, new MusicPlayerPreset2(cg.random), 10, true, false);
            cg.generateRandomCrates(0.03F, ObjectRegistry.getObjectID("snowcrate"));
            this.postGeneratedStructures(cg, presets);
         }
      );
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
