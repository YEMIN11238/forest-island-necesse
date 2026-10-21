package necesse.inventory.item.armorItem.soulseed;

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

public class SoulseedCrownArmorItem extends SetHelmetArmorItem {
   public SoulseedCrownArmorItem() {
      super(
         4,
         DamageTypeRegistry.SUMMON,
         1300,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "soulseedcrown",
         "soulseedchestplate",
         "soulseedboots",
         "soulseedcrownsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.MAX_SUMMONS, 1), new ModifierValue<>(BuffModifiers.SUMMONS_SPEED, 0.3F));
   }
}
