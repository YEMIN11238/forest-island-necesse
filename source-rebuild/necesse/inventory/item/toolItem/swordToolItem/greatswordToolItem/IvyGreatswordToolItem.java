package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GreatswordWeaponsLootTable;

public class IvyGreatswordToolItem extends GreatswordToolItem {
   public IvyGreatswordToolItem() {
      super(850, GreatswordWeaponsLootTable.greatswordWeapons, getThreeChargeLevels(500, 600, 700));
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(100.0F).setUpgradedValue(1.0F, 179.66672F);
      this.attackRange.setBaseValue(126);
      this.knockback.setBaseValue(150);
      this.canBeUsedForRaids = true;
   }
}
