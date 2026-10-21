package necesse.engine.incursionPerkTree;

import java.util.Objects;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameRandom;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.incursionPresets.ChefInvaderPreset1;
import necesse.level.maps.presets.incursionPresets.ChefInvaderPreset2;
import necesse.level.maps.presets.incursionPresets.ChefInvaderPreset3;

public class ChefInvadersPerk extends IncursionPerk {
   public ChefInvadersPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionStructuresGenerated(AltarData altarData, IncursionData incursionData, PresetGeneration presets, GameRandom random, Biome biome) {
      if (!Objects.equals(incursionData.getStringID(), "trial")) {
         float chance = 0.4F;
         if (random.getChance(chance)) {
            Preset chefInvaderPreset = random.getOneOf(
               new ChefInvaderPreset1(random, biome), new ChefInvaderPreset2(random, biome), new ChefInvaderPreset3(random, biome)
            );
            presets.findRandomValidPositionAndApply(random, 50, chefInvaderPreset, 5, true, false);
         }
      }
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate("incursion", this.getStringID() + "desc");
      String invadersDropLootTooltip = Localization.translate("incursion", "invadersdroplootdesc");
      return baseTooltip.concat("\n").concat("\n").concat(invadersDropLootTooltip);
   }
}
