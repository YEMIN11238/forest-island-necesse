package necesse.inventory.item.armorItem.dryad;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class DryadCrownArmorItem extends SetHelmetArmorItem {
   public DryadCrownArmorItem() {
      super(
         6,
         DamageTypeRegistry.SUMMON,
         1550,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "dryadcrown",
         "dryadchestplate",
         "dryadboots",
         "dryadcrownsetbonus"
      );
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.SUMMON_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1)
      );
   }
}
