package necesse.engine.journal;

import java.util.ArrayList;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.presets.DeepCaveChestLootTable;

public class ObtainDeepDesertTrinketJournalChallenge extends ItemObtainedJournalChallenge {
   public ObtainDeepDesertTrinketJournalChallenge() {
      super();
   }

   @Override
   public void onChallengeRegistryClosed() {
      super.onChallengeRegistryClosed();
      ArrayList<String> itemStringIDs = new ArrayList<>();
      LootList list = new LootList();
      DeepCaveChestLootTable.desertMainItems.addPossibleLoot(list);

      for (InventoryItem invItem : list.getCombinedItemsAndCustomItems()) {
         if (invItem.item.isTrinketItem()) {
            itemStringIDs.add(invItem.item.getStringID());
         }
      }

      this.itemStringIDs = itemStringIDs.toArray(new String[0]);
   }
}
