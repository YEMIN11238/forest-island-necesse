package necesse.inventory;

public interface InventoryFilter {
   boolean isItemValid(int var1, InventoryItem var2);

   default int getItemStackLimit(int slot, InventoryItem item) {
      return item.itemStackSize();
   }
}
