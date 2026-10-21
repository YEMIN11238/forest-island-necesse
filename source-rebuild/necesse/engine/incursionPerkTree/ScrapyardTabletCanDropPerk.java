package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.level.maps.incursion.AltarData;

public class ScrapyardTabletCanDropPerk extends IncursionPerk {
   public ScrapyardTabletCanDropPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate(
         "incursion", this.getStringID() + "desc", "tier", IncursionBiomeRegistry.getBiomeTier(IncursionBiomeRegistry.SCRAPYARD.getID())
      );
      String tabletDropTooltip = Localization.translate(
         "incursion", "incursionperkdropstablet", "tier", this.tier, "biome", Localization.translate("biome", IncursionBiomeRegistry.SCRAPYARD.getStringID())
      );
      return baseTooltip.concat("\n").concat("\n").concat(tabletDropTooltip);
   }

   @Override
   public boolean hasSpecificIncursionAtTierCompleted(AltarData altarData) {
      this.incursionAndTierRequirement = new IncursionAndTierRequirement(IncursionBiomeRegistry.CRYSTAL_HOLLOW, 7);
      return altarData.checkForSpecificIncursionCompletedAtSpecificTierOrAbove(IncursionBiomeRegistry.CRYSTAL_HOLLOW.getID(), 7);
   }

   @Override
   public int getTabletIDForTabletDropPerks() {
      return IncursionBiomeRegistry.SCRAPYARD.getID();
   }

   @Override
   public boolean locksAllOtherPerksOnTier() {
      return true;
   }
}
