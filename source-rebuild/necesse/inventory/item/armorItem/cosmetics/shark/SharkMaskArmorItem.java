package necesse.inventory.item.armorItem.cosmetics.shark;

import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.presets.CosmeticArmorLootTable;
import necesse.inventory.lootTable.presets.CosmeticSetArmorLootTable;

public class SharkMaskArmorItem extends SetHelmetArmorItem {
   public SharkMaskArmorItem() {
      super(
         0,
         null,
         0,
         CosmeticArmorLootTable.cosmeticArmor,
         CosmeticSetArmorLootTable.cosmeticSetArmor,
         Item.Rarity.RARE,
         "sharkmask",
         "sharkcostumeshirt",
         "sharkcostumeboots",
         null
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.NO_FACIAL_FEATURE;
   }
}
