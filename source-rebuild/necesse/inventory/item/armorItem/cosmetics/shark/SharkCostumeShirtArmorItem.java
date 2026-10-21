package necesse.inventory.item.armorItem.cosmetics.shark;

import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.CosmeticArmorLootTable;

public class SharkCostumeShirtArmorItem extends ChestArmorItem {
   public SharkCostumeShirtArmorItem() {
      super(0, 0, Item.Rarity.RARE, "sharkcostumeshirt", "sharkcostumearms", CosmeticArmorLootTable.cosmeticArmor);
   }
}
