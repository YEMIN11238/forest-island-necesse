package necesse.inventory.item.armorItem.iron;

import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;

public class IronChestplateArmorItem extends ChestArmorItem {
   public IronChestplateArmorItem() {
      super(3, 300, "ironchest", "ironarms", BodyArmorLootTable.bodyArmor);
   }
}
