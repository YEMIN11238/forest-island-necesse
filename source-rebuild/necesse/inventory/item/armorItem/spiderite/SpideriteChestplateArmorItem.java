package necesse.inventory.item.armorItem.spiderite;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.IncursionBodyArmorLootTable;

public class SpideriteChestplateArmorItem extends ChestArmorItem {
   public SpideriteChestplateArmorItem() {
      super(29, 1900, Item.Rarity.EPIC, "spideritechestplate", "spideritearms", IncursionBodyArmorLootTable.incursionBodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F), new ModifierValue<>(BuffModifiers.STAMINA_REGEN, 0.3F));
   }
}
