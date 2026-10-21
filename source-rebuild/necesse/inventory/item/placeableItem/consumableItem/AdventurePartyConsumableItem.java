package necesse.inventory.item.placeableItem.consumableItem;

import java.util.ArrayList;
import java.util.Comparator;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.ComparableSequence;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.SlotPriority;
import necesse.level.maps.Level;

public interface AdventurePartyConsumableItem {
   default boolean canAddToPartyInventory(InventoryItem item, NetworkClient client, Inventory inventory, int slot) {
      return true;
   }

   boolean canAndShouldPartyConsume(Level var1, HumanMob var2, ServerClient var3, InventoryItem var4, String var5);

   InventoryItem onPartyConsume(Level var1, HumanMob var2, ServerClient var3, InventoryItem var4, String var5);

   default boolean shouldPreventHit(Level level, HumanMob mob, ServerClient partyClient, InventoryItem item) {
      return false;
   }

   default ComparableSequence<Integer> getPartyPriority(
      Level level, HumanMob mob, ServerClient partyClient, Inventory inventory, int inventorySlot, InventoryItem item, String purpose
   ) {
      return new ComparableSequence<>(inventorySlot);
   }

   static ArrayList<SlotPriority> getPartyPriorityList(Level level, HumanMob mob, ServerClient partyClient, Inventory inventory, String purpose) {
      ArrayList<SlotPriority> priorityList = new ArrayList<>();

      for (int i = 0; i <= inventory.getSize(); i++) {
         InventoryItem invItem = inventory.getItem(i);
         if (invItem != null && invItem.item instanceof AdventurePartyConsumableItem) {
            AdventurePartyConsumableItem partyItem = (AdventurePartyConsumableItem)invItem.item;
            if (partyItem.canAndShouldPartyConsume(level, mob, partyClient, invItem, purpose)) {
               priorityList.add(new SlotPriority(i, partyItem.getPartyPriority(level, mob, partyClient, inventory, i, invItem, purpose)));
            }
         }
      }

      Comparator<SlotPriority> comparator = Comparator.comparing(i -> i.comparable);
      priorityList.sort(comparator);
      return priorityList;
   }
}
