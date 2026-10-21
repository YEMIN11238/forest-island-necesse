package necesse.inventory.item.armorItem.battlechef;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.IncursionBodyArmorLootTable;

public class BattleChefChestplateArmorItem extends ChestArmorItem {
   public IntUpgradeValue maxHealth = new IntUpgradeValue().setBaseValue(15).setUpgradedValue(1.0F, 15).setUpgradedValue(10.0F, 30);

   public BattleChefChestplateArmorItem() {
      super(23, 2000, Item.Rarity.EPIC, "battlechefchestplate", "battlechefarms", IncursionBodyArmorLootTable.incursionBodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.KNOCKBACK_INCOMING_MOD).max(0.0F),
         new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, this.maxHealth.getValue(this.getUpgradeTier(item)))
      );
   }
}
