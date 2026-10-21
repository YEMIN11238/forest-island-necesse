package necesse.entity.objectEntity;

import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.level.maps.Level;

public class DresserObjectEntity extends InventoryObjectEntity {
   public static final int SETS = 10;

   public DresserObjectEntity(Level level, int x, int y) {
      super(level, x, y, 30);
   }

   public static ArmorItem.ArmorType getArmorType(int slot) {
      int slotDiv = slot / 10;
      if (slotDiv == 0) {
         return ArmorItem.ArmorType.HEAD;
      } else if (slotDiv == 1) {
         return ArmorItem.ArmorType.CHEST;
      } else {
         return slotDiv == 2 ? ArmorItem.ArmorType.FEET : null;
      }
   }

   @Override
   public boolean isItemValid(int slot, InventoryItem item) {
      if (item != null) {
         if (item.item.isArmorItem()) {
            ArmorItem armorItem = (ArmorItem)item.item;
            if (armorItem.canMobEquip(null, item)) {
               ArmorItem.ArmorType armorType = getArmorType(slot);
               if (armorType != null) {
                  return armorItem.armorType == armorType;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean isSettlementStorageItemDisabled(Item item) {
      return !item.isArmorItem();
   }

   @Override
   public boolean canQuickStackInventory() {
      return false;
   }

   @Override
   public boolean canRestockInventory() {
      return false;
   }

   @Override
   public boolean canSortInventory() {
      return false;
   }

   @Override
   public boolean canUseForNearbyCrafting() {
      return false;
   }
}
