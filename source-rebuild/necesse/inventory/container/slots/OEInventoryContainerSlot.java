package necesse.inventory.container.slots;

import necesse.entity.objectEntity.interfaces.OEInventory;

public class OEInventoryContainerSlot extends ContainerSlot {
   public OEInventoryContainerSlot(OEInventory oeInventory, int inventorySlot) {
      super(oeInventory.getInventory(), inventorySlot);
   }
}
