package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GreatswordWeaponsLootTable;

public class FrostGreatswordToolItem extends GreatswordToolItem {
   public FrostGreatswordToolItem() {
      super(500, GreatswordWeaponsLootTable.greatswordWeapons, getThreeChargeLevels(500, 600, 700));
      this.rarity = Item.Rarity.COMMON;
      this.attackDamage.setBaseValue(75.0F).setUpgradedValue(1.0F, 184.33339F);
      this.attackRange.setBaseValue(114);
      this.knockback.setBaseValue(150);
      this.canBeUsedForRaids = true;
   }
}
