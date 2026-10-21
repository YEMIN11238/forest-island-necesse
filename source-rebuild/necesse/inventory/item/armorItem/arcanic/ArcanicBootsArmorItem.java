package necesse.inventory.item.armorItem.arcanic;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;

public class ArcanicBootsArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.25F).setUpgradedValue(1.0F, 0.25F);

   public ArcanicBootsArmorItem() {
      super(17, 1900, Item.Rarity.EPIC, "arcanicboots", null);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item))),
         new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 100),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.5F)
      );
   }
}
