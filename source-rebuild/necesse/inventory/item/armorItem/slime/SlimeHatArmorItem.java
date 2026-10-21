package necesse.inventory.item.armorItem.slime;

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
import necesse.inventory.lootTable.presets.IncursionArmorSetsLootTable;
import necesse.inventory.lootTable.presets.IncursionHeadArmorLootTable;
import necesse.level.maps.incursion.IncursionData;

public class SlimeHatArmorItem extends SetHelmetArmorItem {
   public FloatUpgradeValue manaRegen = new FloatUpgradeValue().setBaseValue(1.5F).setUpgradedValue(1.0F, 1.5F);

   public SlimeHatArmorItem() {
      super(
         21,
         DamageTypeRegistry.MAGIC,
         1900,
         IncursionHeadArmorLootTable.incursionHeadArmor,
         IncursionArmorSetsLootTable.incursionArmorSets,
         Item.Rarity.EPIC,
         "slimehat",
         "slimechestplate",
         "slimeboots",
         "slimehatsetbonus"
      );
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.hairMaskTextureName = "slimehat_hairmask";
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.15F),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, this.manaRegen.getValue(this.getUpgradeTier(item)))
      );
   }
}
