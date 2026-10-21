package necesse.inventory.item.armorItem.sharkarmor;

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

public class SharkHelmetArmorItem extends SetHelmetArmorItem {
   public FloatUpgradeValue resilienceRegen = new FloatUpgradeValue().setBaseValue(0.25F).setUpgradedValue(1.0F, 0.75F);

   public SharkHelmetArmorItem() {
      super(
         5,
         DamageTypeRegistry.MELEE,
         500,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "sharkarmorhelmet",
         "sharkarmorchestplate",
         "sharkarmorboots",
         "sharkarmorsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F),
         new ModifierValue<>(BuffModifiers.RESILIENCE_REGEN_FLAT, this.resilienceRegen.getValue(this.getUpgradeTier(item))),
         new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 10)
      );
   }
}
