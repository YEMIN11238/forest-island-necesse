package necesse.inventory.item.armorItem.arcanic;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;

public class ArcanicChestplateArmorItem extends ChestArmorItem {
   public ArcanicChestplateArmorItem() {
      super(29, 1900, Item.Rarity.EPIC, "arcanicchestplate", "arcanicarms", null);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 100),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 1.5F),
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.15F),
         new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F)
      );
   }
}
