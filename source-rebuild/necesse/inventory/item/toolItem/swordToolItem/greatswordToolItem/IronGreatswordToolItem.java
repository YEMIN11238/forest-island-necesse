package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GreatswordWeaponsLootTable;

public class IronGreatswordToolItem extends GreatswordToolItem {
   public IronGreatswordToolItem() {
      super(300, GreatswordWeaponsLootTable.greatswordWeapons, getThreeChargeLevels(500, 600, 700));
      this.rarity = Item.Rarity.COMMON;
      this.attackDamage.setBaseValue(50.0F).setUpgradedValue(1.0F, 186.66672F);
      this.attackRange.setBaseValue(110);
      this.knockback.setBaseValue(150);
      this.canBeUsedForRaids = true;
   }
}
