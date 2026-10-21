package necesse.engine.incursionPerkTree;

import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.presets.incursionPresets.GraveyardMegaUpgradeShardsClusterPreset;

public class MegaUpgradeShardVeinPerk extends IncursionPerk {
   public MegaUpgradeShardVeinPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onGenerateOres(CaveGeneration cg, IncursionLevel incursionLevel, Biome biome, GameRandom random) {
      super.onGenerateOres(cg, incursionLevel, biome, random);
      if (cg != null && random.getChance(0.25F)) {
         cg.generateGuaranteedOreVeins(65, 65, 65, ObjectRegistry.getObjectID(incursionLevel.upgradeShardsRockID));
      }
   }

   @Override
   public void onIncursionStructuresGenerated(AltarData altarData, IncursionData incursionData, PresetGeneration presets, GameRandom random, Biome biome) {
      if (biome == BiomeRegistry.GRAVEYARD && random.getChance(0.25F)) {
         presets.findRandomValidPositionAndApply(random, 50, new GraveyardMegaUpgradeShardsClusterPreset(), 5, true, true);
      }
   }
}
