package necesse.inventory.container.slots;

import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.miscItem.InternalInventoryItemInterface;

public class InternalInventoryItemContainerSlot extends ContainerSlot {
   private InternalInventoryItemInterface internalInventoryItemInterface;

   public InternalInventoryItemContainerSlot(Inventory inventory, int inventorySlot, InternalInventoryItemInterface internalInventoryItemInterface) {
      super(inventory, inventorySlot);
      this.internalInventoryItemInterface = internalInventoryItemInterface;
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      return item != null && !this.internalInventoryItemInterface.isValidItem(item) ? "" : null;
   }
}
