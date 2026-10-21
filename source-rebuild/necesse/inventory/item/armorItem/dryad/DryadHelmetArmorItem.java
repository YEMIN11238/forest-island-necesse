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

public class DryadHelmetArmorItem extends SetHelmetArmorItem {
   public DryadHelmetArmorItem() {
      super(
         26,
         DamageTypeRegistry.MELEE,
         1550,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "dryadhelmet",
         "dryadchestplate",
         "dryadboots",
         "dryadhelmetsetbonus"
      );
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.MELEE_DAMAGE, 0.1F), new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.1F));
   }
}
