package necesse.entity.mobs.itemAttacker;

import necesse.inventory.InventoryItem;

public interface ItemAttackSlot {
   void setItem(InventoryItem var1);

   InventoryItem getItem();

   boolean isStillValid(ItemAttackerMob var1, InventoryItem var2);

   boolean isSameSlot(ItemAttackSlot var1);

   void markDirty();
}
