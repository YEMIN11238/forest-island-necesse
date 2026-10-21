package necesse.inventory.item.armorItem.makeshift;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.IncursionBodyArmorLootTable;

public class MakeshiftChestplate extends ChestArmorItem {
   public MakeshiftChestplate() {
      super(29, 1900, Item.Rarity.EPIC, "makeshiftchestplate", "makeshiftchestplatearms", IncursionBodyArmorLootTable.incursionBodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 100),
         new ModifierValue<>(BuffModifiers.ARMOR, 0.1F),
         new ModifierValue<>(BuffModifiers.KNOCKBACK_INCOMING_MOD, 0.5F)
      );
   }
}
