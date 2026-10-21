package necesse.inventory.item.toolItem.spearToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;

public class WoodSpearToolItem extends SpearToolItem {
   public WoodSpearToolItem() {
      super(100, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(14.0F).setUpgradedValue(1.0F, 81.66669F);
      this.attackRange.setBaseValue(100);
      this.knockback.setBaseValue(25);
      this.canBeUsedForRaids = true;
   }
}
