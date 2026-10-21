package necesse.inventory.item.armorItem.quartz;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;

public class QuartzChestplateArmorItem extends ChestArmorItem {
   public QuartzChestplateArmorItem() {
      super(18, 1000, Item.Rarity.UNCOMMON, "quartzchest", "quartzarms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F));
   }
}
