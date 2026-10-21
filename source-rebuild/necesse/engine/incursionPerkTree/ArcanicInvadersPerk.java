package necesse.engine.incursionPerkTree;

import java.util.Objects;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameRandom;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.presets.incursionPresets.ArcanicInvadersPreset1;
import necesse.level.maps.presets.incursionPresets.ArcanicInvadersPreset2;
import necesse.level.maps.presets.incursionPresets.ArcanicInvadersPreset3;

public class ArcanicInvadersPerk extends IncursionPerk {
   public ArcanicInvadersPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, true, prerequisitePerkRequired);
   }

   @Override
   public void onIncursionStructuresGenerated(AltarData altarData, IncursionData incursionData, PresetGeneration presets, GameRandom random, Biome biome) {
      if (!Objects.equals(incursionData.getStringID(), "trial")) {
         float chance = 0.4F;
         if (random.getChance(chance)) {
            Preset arcanicInvaderPreset = random.getOneOf(
               new ArcanicInvadersPreset1(random), new ArcanicInvadersPreset2(random), new ArcanicInvadersPreset3(random)
            );
            presets.findRandomValidPositionAndApply(random, 50, arcanicInvaderPreset, 5, true, false);
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
