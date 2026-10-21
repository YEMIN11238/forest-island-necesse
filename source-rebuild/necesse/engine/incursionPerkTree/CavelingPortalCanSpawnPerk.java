package necesse.engine.incursionPerkTree;

import java.util.Objects;
import necesse.engine.util.GameRandom;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.presets.IncursionCavelingPortalPreset;
import necesse.level.maps.presets.Preset;

public class CavelingPortalCanSpawnPerk extends IncursionPerk {
   public CavelingPortalCanSpawnPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionStructuresGenerated(AltarData altarData, IncursionData incursionData, PresetGeneration presets, GameRandom random, Biome biome) {
      if (!Objects.equals(incursionData.getStringID(), "trial")) {
         float chance = 0.4F;
         if (random.getChance(chance)) {
            Preset incursionPortalPreset = random.getOneOf(new IncursionCavelingPortalPreset());
            presets.findRandomValidPositionAndApply(random, 50, incursionPortalPreset, 5, false, false);
         }
      }
   }
}
