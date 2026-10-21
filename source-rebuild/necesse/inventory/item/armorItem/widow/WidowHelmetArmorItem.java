package necesse.inventory.item.armorItem.widow;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class WidowHelmetArmorItem extends SetHelmetArmorItem {
   public FloatUpgradeValue manaRegen = new FloatUpgradeValue().setBaseValue(1.5F).setUpgradedValue(1.0F, 1.5F);

   public WidowHelmetArmorItem() {
      super(
         20,
         DamageTypeRegistry.MAGIC,
         1600,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "widowhelmet",
         "widowchestplate",
         "widowboots",
         "widowsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HEAD;
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAGIC_ATTACK_SPEED, 0.1F),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, this.manaRegen.getValue(this.getUpgradeTier(item)))
      );
   }
}
