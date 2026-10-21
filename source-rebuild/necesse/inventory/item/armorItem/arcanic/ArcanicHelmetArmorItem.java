package necesse.inventory.item.armorItem.arcanic;

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

public class ArcanicHelmetArmorItem extends SetHelmetArmorItem {
   public ArcanicHelmetArmorItem() {
      super(
         23,
         null,
         1900,
         IncursionHeadArmorLootTable.incursionHeadArmor,
         IncursionArmorSetsLootTable.incursionArmorSets,
         Item.Rarity.EPIC,
         "arcanichelmet",
         "arcanicchestplate",
         "arcanicboots",
         "arcanichelmetsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HEAD;
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 200),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 2.0F),
         new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.15F)
      );
   }
}
