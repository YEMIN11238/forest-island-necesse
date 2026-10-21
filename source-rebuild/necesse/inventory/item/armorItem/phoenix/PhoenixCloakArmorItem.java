package necesse.inventory.item.armorItem.phoenix;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;

public class PhoenixCloakArmorItem extends ChestArmorItem {
   public PhoenixCloakArmorItem() {
      super(30, 1450, Item.Rarity.UNCOMMON, "phoenixcloak", null, BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F), new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.1F));
   }
}
