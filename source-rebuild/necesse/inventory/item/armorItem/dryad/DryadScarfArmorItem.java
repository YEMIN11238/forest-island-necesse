package necesse.inventory.item.armorItem.dryad;

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

public class DryadScarfArmorItem extends SetHelmetArmorItem {
   public DryadScarfArmorItem() {
      super(
         16,
         DamageTypeRegistry.RANGED,
         1550,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "dryadscarf",
         "dryadchestplate",
         "dryadboots",
         "dryadscarfsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.UNDER_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.NO_FACIAL_FEATURE;
      this.hairMaskTextureName = "dryadscarf_hairmask";
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.RANGED_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.CRIT_DAMAGE, 0.1F),
         new ModifierValue<>(BuffModifiers.PROJECTILE_VELOCITY, 0.2F)
      );
   }
}
