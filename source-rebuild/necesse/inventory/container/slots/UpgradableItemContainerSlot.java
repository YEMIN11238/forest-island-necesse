package necesse.inventory.container.slots;

import necesse.engine.localization.Localization;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.upgradeUtils.UpgradableItem;

public class UpgradableItemContainerSlot extends ContainerSlot {
   public UpgradableItemContainerSlot(Inventory inventory, int inventorySlot) {
      super(inventory, inventorySlot);
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (item == null) {
         return null;
      } else {
         return item.item instanceof UpgradableItem
            ? ((UpgradableItem)item.item).getCanBeUpgradedError(item)
            : Localization.translate("ui", "itemnotupgradable");
      }
   }
}
