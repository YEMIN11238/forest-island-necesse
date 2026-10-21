package necesse.inventory.item.toolItem.spearToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;

public class IronSpearToolItem extends SpearToolItem {
   public IronSpearToolItem() {
      super(300, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 77.00002F);
      this.attackRange.setBaseValue(100);
      this.knockback.setBaseValue(25);
      this.canBeUsedForRaids = true;
   }
}
