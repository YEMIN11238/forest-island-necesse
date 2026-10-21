package necesse.inventory.item.armorItem.phoenix;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class PhoenixMaskArmorItem extends SetHelmetArmorItem {
   public PhoenixMaskArmorItem() {
      super(
         24,
         null,
         1450,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "phoenixmask",
         "phoenixcloak",
         "phoenixboots",
         "phoenixsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.UNDER_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.NO_FACIAL_FEATURE;
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, 1.0F), new ModifierValue<>(BuffModifiers.HEALTH_REGEN_FLAT, 10.0F));
   }
}
