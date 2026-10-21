package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.level.maps.incursion.AltarData;

public class CrystalHollowTabletCanDropPerk extends IncursionPerk {
   public CrystalHollowTabletCanDropPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate(
         "incursion", this.getStringID() + "desc", "tier", IncursionBiomeRegistry.getBiomeTier(IncursionBiomeRegistry.CRYSTAL_HOLLOW.getID())
      );
      String tabletDropTooltip = Localization.translate(
         "incursion",
         "incursionperkdropstablet",
         "tier",
         this.tier,
         "biome",
         Localization.translate("biome", IncursionBiomeRegistry.CRYSTAL_HOLLOW.getStringID())
      );
      return baseTooltip.concat("\n").concat("\n").concat(tabletDropTooltip);
   }

   @Override
   public boolean hasSpecificIncursionAtTierCompleted(AltarData altarData) {
      this.incursionAndTierRequirement = new IncursionAndTierRequirement(IncursionBiomeRegistry.SUN_ARENA, 6);
      return altarData.checkForSpecificIncursionCompletedAtSpecificTierOrAbove(IncursionBiomeRegistry.SUN_ARENA.getID(), 6);
   }

   @Override
   public int getTabletIDForTabletDropPerks() {
      return IncursionBiomeRegistry.CRYSTAL_HOLLOW.getID();
   }

   @Override
   public boolean locksAllOtherPerksOnTier() {
      return true;
   }
}
