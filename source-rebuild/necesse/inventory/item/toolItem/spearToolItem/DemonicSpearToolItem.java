package necesse.inventory.item.toolItem.spearToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;

public class DemonicSpearToolItem extends SpearToolItem {
   public DemonicSpearToolItem() {
      super(400, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(450);
      this.attackDamage.setBaseValue(24.0F).setUpgradedValue(1.0F, 70.000015F);
      this.attackRange.setBaseValue(130);
      this.knockback.setBaseValue(25);
      this.canBeUsedForRaids = true;
   }
}
