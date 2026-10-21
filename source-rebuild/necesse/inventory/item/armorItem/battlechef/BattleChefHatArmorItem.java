package necesse.inventory.item.armorItem.battlechef;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.IncursionArmorSetsLootTable;
import necesse.inventory.lootTable.presets.IncursionHeadArmorLootTable;

public class BattleChefHatArmorItem extends SetHelmetArmorItem {
   public IntUpgradeValue maxHealth = new IntUpgradeValue().setBaseValue(15).setUpgradedValue(1.0F, 15).setUpgradedValue(10.0F, 25);

   public BattleChefHatArmorItem() {
      super(
         19,
         DamageTypeRegistry.MELEE,
         2000,
         IncursionHeadArmorLootTable.incursionHeadArmor,
         IncursionArmorSetsLootTable.incursionArmorSets,
         Item.Rarity.EPIC,
         "battlechefhat",
         "battlechefchestplate",
         "battlechefboots",
         "battlechefsetbonus"
      );
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.STAMINA_REGEN, -0.25F),
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.15F),
         new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, this.maxHealth.getValue(this.getUpgradeTier(item)))
      );
   }
}
