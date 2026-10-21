package necesse.inventory.item.armorItem.crystal;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.presets.IncursionArmorSetsLootTable;
import necesse.inventory.lootTable.presets.IncursionHeadArmorLootTable;
import necesse.level.maps.incursion.IncursionData;

public class RubyCrown extends SetHelmetArmorItem {
   public RubyCrown() {
      super(
         23,
         null,
         1900,
         IncursionHeadArmorLootTable.incursionHeadArmor,
         IncursionArmorSetsLootTable.incursionArmorSets,
         Item.Rarity.EPIC,
         "rubycrown",
         "crystalchestplate",
         "crystalboots",
         "rubysetbonus"
      );
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1),
         new ModifierValue<>(BuffModifiers.SUMMON_CRIT_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.SUMMON_CRIT_CHANCE, 0.1F),
         new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.1F),
         new ModifierValue<>(BuffModifiers.SUMMON_ATTACK_SPEED, 0.1F),
         new ModifierValue<>(BuffModifiers.SUMMONS_TARGET_RANGE, 0.1F)
      );
   }
}
