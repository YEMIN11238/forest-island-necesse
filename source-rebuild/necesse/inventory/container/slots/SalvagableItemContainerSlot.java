package necesse.inventory.container.slots;

import necesse.engine.localization.Localization;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.upgradeUtils.SalvageableItem;

public class SalvagableItemContainerSlot extends ContainerSlot {
   public SalvagableItemContainerSlot(Inventory inventory, int inventorySlot) {
      super(inventory, inventorySlot);
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (item == null) {
         return null;
      } else {
         return item.item instanceof SalvageableItem
            ? ((SalvageableItem)item.item).getCanBeSalvagedError(item)
            : Localization.translate("ui", "itemnotsalvageable");
      }
   }
}
