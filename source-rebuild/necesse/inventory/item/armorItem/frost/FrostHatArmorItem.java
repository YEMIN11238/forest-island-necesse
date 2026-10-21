package necesse.inventory.item.armorItem.frost;

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
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class FrostHatArmorItem extends SetHelmetArmorItem {
   public IntUpgradeValue maxMana = new IntUpgradeValue(0, 0.1F).setBaseValue(50).setUpgradedValue(1.0F, 250);
   public FloatUpgradeValue manaRegen = new FloatUpgradeValue().setBaseValue(0.5F).setUpgradedValue(1.0F, 1.5F);

   public FrostHatArmorItem() {
      super(
         3,
         DamageTypeRegistry.MAGIC,
         500,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.COMMON,
         "frosthat",
         "frostchestplate",
         "frostboots",
         "frosthatsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.hairMaskTextureName = "frosthat_hairmask";
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, this.maxMana.getValue(this.getUpgradeTier(item))),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, this.manaRegen.getValue(this.getUpgradeTier(item)))
      );
   }
}
