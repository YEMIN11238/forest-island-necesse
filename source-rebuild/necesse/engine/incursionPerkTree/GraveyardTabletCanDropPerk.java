package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.level.maps.incursion.AltarData;

public class GraveyardTabletCanDropPerk extends IncursionPerk {
   public GraveyardTabletCanDropPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate(
         "incursion", this.getStringID() + "desc", "tier", IncursionBiomeRegistry.getBiomeTier(IncursionBiomeRegistry.GRAVEYARD.getID())
      );
      String tabletDropTooltip = Localization.translate(
         "incursion", "incursionperkdropstablet", "tier", this.tier, "biome", Localization.translate("biome", IncursionBiomeRegistry.GRAVEYARD.getStringID())
      );
      return baseTooltip.concat("\n").concat("\n").concat(tabletDropTooltip);
   }

   @Override
   public int getTabletIDForTabletDropPerks() {
      return IncursionBiomeRegistry.GRAVEYARD.getID();
   }

   @Override
   public boolean hasSpecificIncursionAtTierCompleted(AltarData altarData) {
      this.incursionAndTierRequirement = new IncursionAndTierRequirement(IncursionBiomeRegistry.SLIME_CAVE, 2);
      return altarData.checkForSpecificIncursionCompletedAtSpecificTierOrAbove(IncursionBiomeRegistry.SLIME_CAVE.getID(), 2);
   }

   @Override
   public boolean locksAllOtherPerksOnTier() {
      return true;
   }
}
