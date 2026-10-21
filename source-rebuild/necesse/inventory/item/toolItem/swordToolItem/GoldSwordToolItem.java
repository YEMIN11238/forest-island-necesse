package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;

public class GoldSwordToolItem extends SwordToolItem {
   public GoldSwordToolItem() {
      super(350, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(26.0F).setUpgradedValue(1.0F, 105.00003F);
      this.attackRange.setBaseValue(55);
      this.knockback.setBaseValue(75);
      this.canBeUsedForRaids = true;
   }
}
