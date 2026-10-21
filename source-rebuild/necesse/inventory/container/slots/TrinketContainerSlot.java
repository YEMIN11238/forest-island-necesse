package necesse.inventory.container.slots;

import necesse.engine.localization.Localization;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;

public class TrinketContainerSlot extends ContainerSlot {
   public TrinketContainerSlot(Inventory inventory, int inventorySlot) {
      super(inventory, inventorySlot);
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (item == null) {
         return null;
      }

      if (item.item.isTrinketItem()) {
         TrinketItem trinketItem = (TrinketItem)item.item;
         String invalidInSlotError = trinketItem.getInvalidInSlotError(this.getContainer(), this, item);
         if (invalidInSlotError != null) {
            return invalidInSlotError;
         }

         if (trinketItem.isAbilityTrinket(item)) {
            ContainerSlot slot = this.getContainer().getSlot(this.getContainer().CLIENT_TRINKET_ABILITY_SLOT);
            if (slot.isClear()) {
               return Localization.translate("itemtooltip", "useabilityslot");
            }
         }

         return null;
      } else {
         return "";
      }
   }

   @Override
   public int getItemStackLimit(InventoryItem item) {
      return 1;
   }
}
