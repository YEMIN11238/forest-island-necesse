package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;

public class IvySwordToolItem extends SwordToolItem {
   public IvySwordToolItem() {
      super(850, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(47.0F).setUpgradedValue(1.0F, 109.666695F);
      this.attackRange.setBaseValue(65);
      this.knockback.setBaseValue(80);
      this.canBeUsedForRaids = true;
   }
}
