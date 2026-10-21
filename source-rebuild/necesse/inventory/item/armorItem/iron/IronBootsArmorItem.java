package necesse.inventory.item.armorItem.iron;

import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.lootTable.presets.FeetArmorLootTable;

public class IronBootsArmorItem extends BootsArmorItem {
   public IronBootsArmorItem() {
      super(2, 300, "ironboots", FeetArmorLootTable.feetArmor);
   }
}
