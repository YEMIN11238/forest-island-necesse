package necesse.inventory.item.armorItem.spider;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.FeetArmorLootTable;

public class SpiderBootsArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.1F).setUpgradedValue(1.0F, 0.25F);

   public SpiderBootsArmorItem() {
      super(2, 350, Item.Rarity.UNCOMMON, "spiderboots", FeetArmorLootTable.feetArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item))), new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.2F)
      );
   }
}
