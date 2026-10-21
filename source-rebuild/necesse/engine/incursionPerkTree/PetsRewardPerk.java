package necesse.engine.incursionPerkTree;

import necesse.engine.localization.Localization;
import necesse.engine.registries.UniqueIncursionRewardsRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.level.maps.incursion.IncursionData;

public class PetsRewardPerk extends IncursionPerk {
   public PetsRewardPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public TicketSystemList<LootItemInterface> onGenerateTabletRewards(
      TicketSystemList<LootItemInterface> ticketedRewards, GameRandom seededRandom, int tier, IncursionData incursionData
   ) {
      int tabletTierWeight = tier * 10;
      ticketedRewards.addObject(40 + tabletTierWeight, UniqueIncursionRewardsRegistry.getRandomPetReward());
      return ticketedRewards;
   }

   @Override
   public String getCustomTooltipLocalization() {
      String baseTooltipDescription = Localization.translate("incursion", this.getStringID() + "desc1");
      String exclusivePetsDescription = Localization.translate("incursion", this.getStringID() + "desc2");
      return baseTooltipDescription.concat("\n").concat("\n").concat(exclusivePetsDescription);
   }
}
