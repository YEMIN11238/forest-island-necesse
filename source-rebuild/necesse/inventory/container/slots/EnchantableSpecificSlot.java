package necesse.inventory.container.slots;

import necesse.engine.localization.Localization;
import necesse.engine.registries.ItemRegistry;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.Enchantable;
import necesse.inventory.enchants.ItemEnchantment;

public class EnchantableSpecificSlot extends EnchantableSlot {
   public ItemEnchantment enchantment;

   public EnchantableSpecificSlot(Inventory inventory, int inventorySlot, ItemEnchantment enchantment) {
      super(inventory, inventorySlot);
      this.enchantment = enchantment;
   }

   @Override
   public String getItemInvalidError(InventoryItem item) {
      if (this.enchantment == null) {
         return "";
      } else {
         String superInvalid = super.getItemInvalidError(item);
         if (superInvalid != null) {
            return superInvalid;
         } else {
            return item != null && !((Enchantable)item.item).isValidEnchantment(item, this.enchantment)
               ? Localization.translate(
                  "ui", "enchantingscrollwrongtype", "item", ItemRegistry.getLocalization(item.item.getID()), "enchantment", this.enchantment.getLocalization()
               )
               : null;
         }
      }
   }
}
