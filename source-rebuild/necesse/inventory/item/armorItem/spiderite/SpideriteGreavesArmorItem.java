package necesse.inventory.item.armorItem.spiderite;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.IncursionFeetArmorLootTable;

public class SpideriteGreavesArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.25F).setUpgradedValue(1.0F, 0.25F);

   public SpideriteGreavesArmorItem() {
      super(17, 1900, Item.Rarity.EPIC, "spideritegreaves", IncursionFeetArmorLootTable.incursionFeetArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.DASH_COOLDOWN, -0.1F), new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item)))
      );
   }
}
