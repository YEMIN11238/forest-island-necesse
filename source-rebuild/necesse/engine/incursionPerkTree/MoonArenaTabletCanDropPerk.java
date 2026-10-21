package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.level.maps.incursion.AltarData;

public class MoonArenaTabletCanDropPerk extends IncursionPerk {
   public MoonArenaTabletCanDropPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate(
         "incursion", this.getStringID() + "desc", "tier", IncursionBiomeRegistry.getBiomeTier(IncursionBiomeRegistry.MOON_ARENA.getID())
      );
      String tabletDropTooltip = Localization.translate(
         "incursion", "incursionperkdropstablet", "tier", this.tier, "biome", Localization.translate("biome", IncursionBiomeRegistry.MOON_ARENA.getStringID())
      );
      return baseTooltip.concat("\n").concat("\n").concat(tabletDropTooltip);
   }

   @Override
   public int getTabletIDForTabletDropPerks() {
      return IncursionBiomeRegistry.MOON_ARENA.getID();
   }

   @Override
   public boolean hasSpecificIncursionAtTierCompleted(AltarData altarData) {
      this.incursionAndTierRequirement = new IncursionAndTierRequirement(IncursionBiomeRegistry.SPIDER_CASTLE, 4);
      return altarData.checkForSpecificIncursionCompletedAtSpecificTierOrAbove(IncursionBiomeRegistry.SPIDER_CASTLE.getID(), 4);
   }

   @Override
   public boolean locksAllOtherPerksOnTier() {
      return true;
   }
}
