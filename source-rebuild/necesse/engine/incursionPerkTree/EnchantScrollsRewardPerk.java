package necesse.engine.incursionPerkTree;

import java.util.List;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.incursion.IncursionData;

public class EnchantScrollsRewardPerk extends IncursionPerk {
   public EnchantScrollsRewardPerk(Integer tier, int perkCost, int xPositionOnPerkTree, IncursionPerk... prerequisitePerkRequired) {
      super(tier, perkCost, xPositionOnPerkTree, false, prerequisitePerkRequired);
   }

   @Override
   public TicketSystemList<LootItemInterface> onGenerateTabletRewards(
      TicketSystemList<LootItemInterface> ticketedRewards, GameRandom seededRandom, int tier, IncursionData incursionData
   ) {
      int tabletTierWeight = tier * 10;
      ticketedRewards.addObject(75 + tabletTierWeight, new LootItemInterface() {
         @Override
         public void addPossibleLoot(LootList list, Object... extra) {
            list.add("enchantingscroll");
         }

         @Override
         public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
            if (LootTable.isExtraEquals(extra, 0, "displayOnly")) {
               InventoryItem enchantingScroll = new InventoryItem("enchantingscroll");
               enchantingScroll.getGndData().setInt("incursionDisplayMinAmount", 1);
               enchantingScroll.getGndData().setInt("incursionDisplayMaxAmount", 5);
               list.add(enchantingScroll);
            } else {
               int amount = random.getIntBetween(1, 5);

               for (int i = 0; i < amount; i++) {
                  InventoryItem enchantingScroll = ItemRegistry.getItem("enchantingscroll").getDefaultLootItem(random, 1);
                  list.add(enchantingScroll);
               }
            }
         }
      });
      return ticketedRewards;
   }
}
