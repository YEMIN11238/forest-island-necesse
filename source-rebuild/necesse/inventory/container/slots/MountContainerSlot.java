package necesse.inventory.container.slots;

import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;

public class MountContainerSlot extends ContainerSlot {
   public MountContainerSlot(Inventory inventory, int inventorySlot) {
      super(inventory, inventorySlot);
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      return item != null && !item.item.isMountItem() ? "" : null;
   }

   @Override
   public int getItemStackLimit(InventoryItem item) {
      return 1;
   }
}
