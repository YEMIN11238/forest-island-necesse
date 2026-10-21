package necesse.inventory.item.armorItem.pharaoh;

import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class PharaohsHeaddress extends SetHelmetArmorItem {
   public PharaohsHeaddress() {
      super(
         10,
         DamageTypeRegistry.MAGIC,
         1000,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "pharaohsheaddress",
         "pharaohsrobe",
         "pharaohssandals",
         "pharaohssetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.NO_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.MAGIC_DAMAGE, 0.1F), new ModifierValue<>(BuffModifiers.MANA_USAGE, 0.1F));
   }
}
