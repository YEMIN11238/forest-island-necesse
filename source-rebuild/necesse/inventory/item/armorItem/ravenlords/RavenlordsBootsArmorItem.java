package necesse.inventory.item.armorItem.ravenlords;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;

public class RavenlordsBootsArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.4F).setUpgradedValue(1.0F, 0.4F);

   public RavenlordsBootsArmorItem() {
      super(17, 1900, Item.Rarity.EPIC, "ravenlordsboots", null);
      this.drawBodyPart = false;
      this.defaultLootTier = 1.0F;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item))));
   }
}
