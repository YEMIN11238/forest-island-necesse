package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.IncursionBiomeRegistry;

public class SlimeTabletCanDropPerk extends IncursionPerk {
   public SlimeTabletCanDropPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltip = Localization.translate(
         "incursion", this.getStringID() + "desc", "tier", IncursionBiomeRegistry.getBiomeTier(IncursionBiomeRegistry.SLIME_CAVE.getID())
      );
      String tabletDropTooltip = Localization.translate(
         "incursion", "incursionperkdropstablet", "tier", this.tier, "biome", Localization.translate("biome", IncursionBiomeRegistry.SLIME_CAVE.getStringID())
      );
      return baseTooltip.concat("\n").concat("\n").concat(tabletDropTooltip);
   }

   @Override
   public int getTabletIDForTabletDropPerks() {
      return IncursionBiomeRegistry.SLIME_CAVE.getID();
   }

   @Override
   public boolean locksAllOtherPerksOnTier() {
      return true;
   }
}
