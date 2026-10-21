package necesse.inventory.container.slots;

import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;

public class CloudContainerSlot extends ContainerSlot {
   public final int itemID;

   public CloudContainerSlot(Inventory inventory, int inventorySlot, int itemID) {
      super(inventory, inventorySlot);
      this.itemID = itemID;
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      return item != null && item.item.getID() == this.itemID ? "" : super.getItemInvalidError(item);
   }
}
