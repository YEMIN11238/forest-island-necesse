package necesse.inventory.item.armorItem.bloodplate;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.ArmorSetsLootTable;
import necesse.inventory.lootTable.presets.HeadArmorLootTable;

public class BloodplateCowlArmorItem extends SetHelmetArmorItem {
   public FloatUpgradeValue healthRegen = new FloatUpgradeValue().setBaseValue(0.15F).setUpgradedValue(1.0F, 0.5F);

   public BloodplateCowlArmorItem() {
      super(
         6,
         null,
         400,
         HeadArmorLootTable.headArmor,
         ArmorSetsLootTable.armorSets,
         Item.Rarity.UNCOMMON,
         "bloodplatecowl",
         "bloodplatechestplate",
         "bloodplateboots",
         "bloodplatecowlsetbonus"
      );
      this.hairDrawOptions = ArmorItem.HairDrawMode.OVER_HAIR;
      this.hairMaskTextureName = "bloodplatecowl_hairmask";
      this.canBeUsedForRaids = true;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.COMBAT_HEALTH_REGEN_FLAT, this.healthRegen.getValue(this.getUpgradeTier(item))));
   }
}
