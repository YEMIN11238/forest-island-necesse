package necesse.inventory.container.slots;

import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;

public class ExtractOnlyContainerSlot extends ContainerSlot {
   public ExtractOnlyContainerSlot(Inventory inventory, int inventorySlot) {
      super(inventory, inventorySlot);
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      return "";
   }
}
