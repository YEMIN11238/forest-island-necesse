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
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.PresetGeneration;

public class SlimeCaveIncursionLevel extends IncursionLevel {
   public SlimeCaveIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public SlimeCaveIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursion, WorldEntity worldEntity, AltarData altarData) {
      super(identifier, 150, 150, incursion, worldEntity);
      this.baseBiome = BiomeRegistry.SLIME_CAVE;
      this.isCave = true;
      this.upgradeShardsRockID = "upgradeshardslimerock";
      this.alchemyShardsRockID = "alchemyshardslimerock";
      this.essenceRockID = "slimeessenceslimerock";
      this.generateLevel(incursion, altarData);
   }

   public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
      CaveGeneration cg = new CaveGeneration(this, "slimerocktile", "slimerock");
      cg.random.setSeed(incursionData.getUniqueID());
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.38F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GameEvents.triggerEvent(
         new GenerateCaveMiniBiomesEvent(this, cg),
         e -> {
            GenerationTools.generateRandomSmoothObjectVeins(
               this, cg.random, 0.5F, 2, 4.0F, 8.0F, 2.0F, 3.0F, ObjectRegistry.getObjectID("groundslime"), 1.0F, true
            );
            GenerationTools.generateRandomSmoothTileVeins(
               this, cg.random, 0.12F, 2, 7.0F, 20.0F, 3.0F, 8.0F, TileRegistry.getTileID("liquidslimetile"), 1.0F, true
            );
            this.liquidManager.calculateShores();
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("slimecaverock"), 0.005F);
            cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("slimecaverocksmall"), 0.01F);
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
         cg.generateGuaranteedOreVeins(100, 8, 16, ObjectRegistry.getObjectID("slimeumslimerock"));
      }

      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID("upgradeshardslimerock"));
      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID("alchemyshardslimerock"));
      this.generateOresBasedOnPerks(altarData, cg, this, this.baseBiome, cg.random);
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
   }
}
