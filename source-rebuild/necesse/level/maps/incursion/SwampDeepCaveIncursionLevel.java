package necesse.level.maps.incursion;

import necesse.engine.GameEvents;
import necesse.engine.events.worldGeneration.GenerateCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GenerateCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GenerateCaveStructuresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveLayoutEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveMiniBiomesEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveOresEvent;
import necesse.engine.events.worldGeneration.GeneratedCaveStructuresEvent;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.PresetGeneration;

public class SwampDeepCaveIncursionLevel extends IncursionLevel {
   public SwampDeepCaveIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public SwampDeepCaveIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursion, WorldEntity worldEntity, AltarData altarData) {
      super(identifier, 150, 150, incursion, worldEntity);
      this.baseBiome = BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION;
      this.isCave = true;
      this.upgradeShardsRockID = "upgradesharddeepswamprock";
      this.alchemyShardsRockID = "alchemysharddeepswamprock";
      this.essenceRockID = "bioessencedeepswamprock";
      this.generateLevel(incursion, altarData);
   }

   public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
      CaveGeneration cg = new CaveGeneration(this, "deepswamprocktile", "deepswamprock");
      cg.random.setSeed(incursionData.getUniqueID());
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.38F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GameEvents.triggerEvent(
         new GenerateCaveMiniBiomesEvent(this, cg),
         e -> {
            GenerationTools.generateRandomSmoothVeinsC(
               this, cg.random, 0.02F, 4, 15.0F, 25.0F, 3.0F, 5.0F, ca -> ca.forEachTile(this, (level, tileX, tileY) -> {
                  level.setTile(tileX, tileY, TileRegistry.spiderNestID);
                  if (cg.random.getChance(0.95F)) {
                     level.setObject(tileX, tileY, ObjectRegistry.cobWebID);
                  } else {
                     level.setObject(tileX, tileY, 0);
                  }
               })
            );
            GameTile swampRockTile = TileRegistry.getTile(TileRegistry.deepSwampRockID);
            GameObject tallGrass = ObjectRegistry.getObject(ObjectRegistry.getObjectID("deepswamptallgrass"));
            GenerationTools.generateRandomSmoothVeinsC(
               this, cg.random, 0.03F, 5, 4.0F, 10.0F, 3.0F, 5.0F, cells -> cells.forEachTile(this, (level, tileX, tileY) -> {
                  swampRockTile.placeTile(level, tileX, tileY, false);
                  this.setObject(tileX, tileY, 0);
                  if (cg.random.getChance(0.85F) && tallGrass.canPlace(level, tileX, tileY, 0, false) == null) {
                     tallGrass.placeObject(level, tileX, tileY, 0, false);
                  }
               })
            );
            GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.04F, 2, 2.0F, 10.0F, 2.0F, 4.0F, TileRegistry.getTileID("lavatile"), 1.0F, true);
            this.liquidManager.calculateShores();
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepswampcaverock"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("deepswampcaverocksmall"), 0.01F);
            GameObject grassObject = ObjectRegistry.getObject(ObjectRegistry.getObjectID("deepswampgrass"));
            GenerationTools.iterateLevel(
               this,
               (x, y) -> this.getTileID(x, y) == TileRegistry.deepSwampRockID && this.getObjectID(x, y) == 0 && cg.random.getChance(0.6F),
               (x, y) -> grassObject.placeObject(this, x, y, 0, false)
            );
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      PresetGeneration presets = new PresetGeneration(this);
      GameEvents.triggerEvent(new GenerateCaveStructuresEvent(this, cg, presets), e -> cg.generateRandomCrates(0.03F, ObjectRegistry.getObjectID("swampcrate")));
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      PresetGeneration entranceAndPerkPresets = new PresetGeneration(this);
      boolean hasBiggerArenaPerk = altarData.hasPerk(IncursionPerksRegistry.BIGGER_ARENA);
      IncursionBiome.generateEntrance(
         this,
         entranceAndPerkPresets,
         cg.random,
         30,
         cg.rockTile,
         "deepswampstonebrickfloor",
         "deepswampstonefloor",
         "deepswampstonecolumn",
         hasBiggerArenaPerk
      );
      this.generatePresetsBasedOnPerks(altarData, incursionData, entranceAndPerkPresets, cg.random, this.baseBiome);
      GenerationTools.checkValid(this);
      if (incursionData instanceof BiomeExtractionIncursionData) {
         cg.generateGuaranteedOreVeins(100, 8, 16, ObjectRegistry.getObjectID("myceliumoredeepswamprock"));
      }

      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID(this.upgradeShardsRockID));
      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID(this.alchemyShardsRockID));
      this.generateOresBasedOnPerks(altarData, cg, this, this.baseBiome, cg.random);
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
   }
}
