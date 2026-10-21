package necesse.inventory.item.armorItem.witch;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;

public class WitchRobeArmorItem extends ChestArmorItem {
   public WitchRobeArmorItem() {
      super(14, 850, Item.Rarity.UNCOMMON, "witchrobe", "witchrobearms", BodyArmorLootTable.bodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.05F), new ModifierValue<>(BuffModifiers.SUMMON_DAMAGE, 0.05F));
   }
}
