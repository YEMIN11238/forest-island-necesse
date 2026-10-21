package necesse.inventory.item.armorItem.dryad;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class DryadHatArmorItem extends SetHelmetArmorItem {
   public FloatUpgradeValue manaRegen = new FloatUpgradeValue().setBaseValue(1.25F).setUpgradedValue(1.0F, 1.5F);

   public DryadHatArmorItem() {
      super(
         10,
         DamageTypeRegistry.MAGIC,
         1550,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "dryadhat",
         "dryadchestplate",
         "dryadboots",
         "dryadhatsetbonus"
      );
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, this.manaRegen.getValue(this.getUpgradeTier(item)))
      );
   }
}
