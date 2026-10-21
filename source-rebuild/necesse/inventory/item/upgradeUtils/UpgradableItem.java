package necesse.inventory.item.upgradeUtils;

import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemStatTipList;

public interface UpgradableItem {
   default String getCanBeUpgradedError(InventoryItem item) {
      return null;
   }

   void addUpgradeStatTips(ItemStatTipList var1, InventoryItem var2, InventoryItem var3, ItemAttackerMob var4, ItemAttackerMob var5);

   UpgradedItem getUpgradedItem(InventoryItem var1);
}
