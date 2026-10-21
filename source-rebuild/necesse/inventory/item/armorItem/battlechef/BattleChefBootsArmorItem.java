package necesse.inventory.item.armorItem.battlechef;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.IncursionFeetArmorLootTable;

public class BattleChefBootsArmorItem extends BootsArmorItem {
   public IntUpgradeValue maxHealth = new IntUpgradeValue().setBaseValue(10).setUpgradedValue(1.0F, 10).setUpgradedValue(10.0F, 20);

   public BattleChefBootsArmorItem() {
      super(16, 2000, Item.Rarity.EPIC, "battlechefboots", IncursionFeetArmorLootTable.incursionFeetArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, this.maxHealth.getValue(this.getUpgradeTier(item))),
         new ModifierValue<>(BuffModifiers.SPEED, 0.25F)
      );
   }
}
