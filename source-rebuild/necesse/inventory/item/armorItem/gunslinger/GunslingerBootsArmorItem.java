package necesse.inventory.item.armorItem.gunslinger;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.FeetArmorLootTable;

public class GunslingerBootsArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.15F).setUpgradedValue(1.0F, 0.25F);

   public GunslingerBootsArmorItem() {
      super(12, 1000, Item.Rarity.UNCOMMON, "gunslingerboots", FeetArmorLootTable.feetArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item))), new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.2F)
      );
   }
}
