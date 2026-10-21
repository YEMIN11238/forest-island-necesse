package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;

public class IronSwordToolItem extends SwordToolItem {
   public IronSwordToolItem() {
      super(300, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(23.0F).setUpgradedValue(1.0F, 107.33336F);
      this.attackRange.setBaseValue(50);
      this.knockback.setBaseValue(75);
      this.canBeUsedForRaids = true;
   }
}
