package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.level.maps.incursion.AltarData;

public class SpiderCastleTabletCanDropPerk extends IncursionPerk {
   public SpiderCastleTabletCanDropPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate(
         "incursion", this.getStringID() + "desc", "tier", IncursionBiomeRegistry.getBiomeTier(IncursionBiomeRegistry.SPIDER_CASTLE.getID())
      );
      String tabletDropTooltip = Localization.translate(
         "incursion",
         "incursionperkdropstablet",
         "tier",
         this.tier,
         "biome",
         Localization.translate("biome", IncursionBiomeRegistry.SPIDER_CASTLE.getStringID())
      );
      return baseTooltip.concat("\n").concat("\n").concat(tabletDropTooltip);
   }

   @Override
   public boolean hasSpecificIncursionAtTierCompleted(AltarData altarData) {
      this.incursionAndTierRequirement = new IncursionAndTierRequirement(IncursionBiomeRegistry.GRAVEYARD, 3);
      return altarData.checkForSpecificIncursionCompletedAtSpecificTierOrAbove(IncursionBiomeRegistry.GRAVEYARD.getID(), 3);
   }

   @Override
   public int getTabletIDForTabletDropPerks() {
      return IncursionBiomeRegistry.SPIDER_CASTLE.getID();
   }

   @Override
   public boolean locksAllOtherPerksOnTier() {
      return true;
   }
}
