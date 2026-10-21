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

public class AmethystHelmet extends SetHelmetArmorItem {
   public AmethystHelmet() {
      super(
         23,
         null,
         1900,
         IncursionHeadArmorLootTable.incursionHeadArmor,
         IncursionArmorSetsLootTable.incursionArmorSets,
         Item.Rarity.EPIC,
         "amethysthelmet",
         "crystalchestplate",
         "crystalboots",
         "amethystsetbonus"
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
         new ModifierValue<>(BuffModifiers.MELEE_ATTACK_SPEED, 0.2F),
         new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 25),
         new ModifierValue<>(BuffModifiers.STAMINA_REGEN, 0.25F)
      );
   }
}
