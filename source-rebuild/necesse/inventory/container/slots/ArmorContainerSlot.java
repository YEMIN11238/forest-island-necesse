package necesse.inventory.container.slots;

import necesse.entity.mobs.Mob;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerEquipmentInventory;
import necesse.inventory.item.armorItem.ArmorItem;

public class ArmorContainerSlot extends ContainerSlot {
   private ArmorItem.ArmorType armorType;

   public ArmorContainerSlot(Inventory inventory, int inventorySlot, ArmorItem.ArmorType armorType) {
      super(inventory, inventorySlot);
      this.armorType = armorType;
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (item == null) {
         return null;
      }

      if (item.item.isArmorItem()) {
         ArmorItem armorItem = (ArmorItem)item.item;
         if (armorItem.armorType == this.armorType) {
            Mob mob = null;
            if (this.getInventory() instanceof PlayerEquipmentInventory) {
               mob = ((PlayerEquipmentInventory)this.getInventory()).player;
            }

            if (armorItem.canMobEquip(mob, item)) {
               return null;
            }
         }
      }

      return "";
   }

   @Override
   public int getItemStackLimit(InventoryItem item) {
      return 1;
   }
}
