package necesse.inventory.container.slots;

import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;

public class EnchantableSlot extends ContainerSlot {
   public EnchantableSlot(Inventory inventory, int inventorySlot) {
      super(inventory, inventorySlot);
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (item == null) {
         return null;
      } else if (!item.item.isEnchantable(item)) {
         String isEnchantableError = item.item.getIsEnchantableError(item);
         return isEnchantableError == null ? "" : isEnchantableError;
      } else {
         return null;
      }
   }
}
