package necesse.inventory.item.toolItem.spearToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;

public class CopperPitchforkToolItem extends SpearToolItem {
   public CopperPitchforkToolItem() {
      super(200, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 77.00002F);
      this.attackRange.setBaseValue(80);
      this.knockback.setBaseValue(25);
      this.width = 80.0F;
      this.canBeUsedForRaids = true;
   }
}
