package necesse.inventory.item.armorItem.agedChampion;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.FeetArmorLootTable;

public class AgedChampionGreavesArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.2F).setUpgradedValue(1.0F, 0.25F);

   public AgedChampionGreavesArmorItem() {
      super(18, 1600, Item.Rarity.EPIC, "agedchampiongreaves", FeetArmorLootTable.feetArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item))),
         new ModifierValue<>(BuffModifiers.ATTACK_MOVEMENT_MOD, 0.75F)
      );
   }
}
