package necesse.inventory.item.armorItem.cosmetics.misc;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.armorItem.ArmorModifiers;

public class CheatShirtArmorItem extends ShirtArmorItem {
   public CheatShirtArmorItem() {
      super(1);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("Cheat Shirt");
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 1000),
         new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 50.0F),
         new ModifierValue<>(BuffModifiers.MAX_MANA_FLAT, 1000),
         new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN_FLAT, 10.0F)
      );
   }
}
