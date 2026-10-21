package necesse.inventory.item.armorItem.cryowitch;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.BodyArmorLootTable;

public class CryoWitchRobeArmorItem extends ChestArmorItem {
   public CryoWitchRobeArmorItem() {
      super(22, 1450, Item.Rarity.UNCOMMON, "cryowitchrobe", "cryowitchrobearms", BodyArmorLootTable.bodyArmor);
      this.hairDrawOptions = ArmorItem.HairDrawMode.UNDER_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.UNDER_FACIAL_FEATURE;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.05F), new ModifierValue<>(BuffModifiers.SUMMON_DAMAGE, 0.05F));
   }
}
