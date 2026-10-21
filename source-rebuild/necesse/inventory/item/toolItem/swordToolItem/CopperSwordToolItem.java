package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;

public class CopperSwordToolItem extends SwordToolItem {
   public CopperSwordToolItem() {
      super(200, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 108.50003F);
      this.attackRange.setBaseValue(50);
      this.knockback.setBaseValue(75);
      this.canBeUsedForRaids = true;
   }
}
