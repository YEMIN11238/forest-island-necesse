package necesse.inventory.item.armorItem.cosmetics.shark;

import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.lootTable.presets.CosmeticArmorLootTable;

public class SharkCostumeBootsArmorItem extends BootsArmorItem {
   public SharkCostumeBootsArmorItem() {
      super(0, 0, Item.Rarity.RARE, "sharkcostumeboots", CosmeticArmorLootTable.cosmeticArmor);
   }
}
