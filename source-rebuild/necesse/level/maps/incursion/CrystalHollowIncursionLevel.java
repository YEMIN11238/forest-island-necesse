package necesse.level.maps.incursion;

import java.util.Arrays;
import java.util.List;
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
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.GenerationTools;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.presets.AncientAmethystRuinPreset;
import necesse.level.maps.presets.AncientEmeraldRuinPreset;
import necesse.level.maps.presets.AncientRubyRuinPreset;
import necesse.level.maps.presets.AncientSapphireRuinPreset;
import necesse.level.maps.presets.AncientTopazRuinPreset;
import necesse.level.maps.presets.Preset;

public class CrystalHollowIncursionLevel extends IncursionLevel {
   public CrystalHollowIncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public CrystalHollowIncursionLevel(LevelIdentifier identifier, BiomeMissionIncursionData incursion, WorldEntity worldEntity, AltarData altarData) {
      super(identifier, 150, 150, incursion, worldEntity);
      this.baseBiome = BiomeRegistry.CRYSTAL_HOLLOW;
      this.isCave = true;
      this.upgradeShardsRockID = "upgradeshardcrystalrock";
      this.alchemyShardsRockID = "alchemyshardcrystalrock";
      this.essenceRockID = "crystalessencecrystalrock";
      this.generateLevel(incursion, altarData);
   }

   public void generateLevel(BiomeMissionIncursionData incursionData, AltarData altarData) {
      CaveGeneration cg = new CaveGeneration(this, "crystaltile", "crystalrock");
      cg.random.setSeed(incursionData.getUniqueID());
      GameEvents.triggerEvent(new GenerateCaveLayoutEvent(this, cg), e -> cg.generateLevel(0.38F, 4, 3, 6));
      GameEvents.triggerEvent(new GeneratedCaveLayoutEvent(this, cg));
      GameEvents.triggerEvent(new GenerateCaveMiniBiomesEvent(this, cg), e -> {
         GenerationTools.generateRandomSmoothTileVeins(this, cg.random, 0.07F, 2, 7.0F, 20.0F, 3.0F, 8.0F, TileRegistry.getTileID("watertile"), 1.0F, true);
         this.liquidManager.calculateShores();
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("amethystclusterpure"), 0.001F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("sapphireclusterpure"), 0.001F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("emeraldclusterpure"), 0.001F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("rubyclusterpure"), 0.001F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("topazclusterpure"), 0.001F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("amethystclustersmall"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("sapphireclustersmall"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("emeraldclustersmall"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("rubyclustersmall"), 0.005F);
         cg.generateRandomSingleRocks(ObjectRegistry.getObjectID("topazclustersmall"), 0.005F);
      });
      GameEvents.triggerEvent(new GeneratedCaveMiniBiomesEvent(this, cg));
      PresetGeneration presets = new PresetGeneration(this);
      GameEvents.triggerEvent(
         new GenerateCaveStructuresEvent(this, cg, presets),
         e -> {
            cg.generateRandomCrates(0.03F, ObjectRegistry.getObjectID("crate"));
            List<Preset> ruins = Arrays.asList(
               new AncientAmethystRuinPreset(),
               new AncientSapphireRuinPreset(),
               new AncientEmeraldRuinPreset(),
               new AncientRubyRuinPreset(),
               new AncientTopazRuinPreset()
            );

            for (int i = 0; i < 10; i++) {
               Preset ruin = cg.random.getOneOf(ruins);
               presets.findRandomValidPositionAndApply(cg.random, 5, ruin, 4, true, false);
            }
         }
      );
      GameEvents.triggerEvent(new GeneratedCaveStructuresEvent(this, cg, presets));
      PresetGeneration entranceAndPerkPresets = new PresetGeneration(this);
      boolean hasBiggerArenaPerk = altarData.hasPerk(IncursionPerksRegistry.BIGGER_ARENA);
      IncursionBiome.generateEntrance(
         this, entranceAndPerkPresets, cg.random, 30, cg.rockTile, "ancientruinfloor", "deadwoodfloor", "ancientruinwall", hasBiggerArenaPerk
      );
      this.generatePresetsBasedOnPerks(altarData, incursionData, entranceAndPerkPresets, cg.random, this.baseBiome);
      GenerationTools.checkValid(this);
      GameObject extractionObject = ObjectRegistry.getObject("pearlescentshard");
      if (incursionData instanceof BiomeExtractionIncursionData) {
         GenerationTools.generateGuaranteedRandomVeins(
            this,
            cg.random,
            25,
            1,
            1,
            (level, tileX, tileY) -> level.getTileID(tileX, tileY) == cg.rockTile && extractionObject.canPlace(level, tileX, tileY, 0, false) == null,
            (level, tileX, tileY) -> extractionObject.placeObject(level, tileX, tileY, 0, false)
         );
      }

      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID(this.upgradeShardsRockID));
      cg.generateGuaranteedOreVeins(75, 6, 12, ObjectRegistry.getObjectID(this.alchemyShardsRockID));
      this.generateOresBasedOnPerks(altarData, cg, this, this.baseBiome, cg.random);
      GameEvents.triggerEvent(new GeneratedCaveOresEvent(this, cg));
   }
}
