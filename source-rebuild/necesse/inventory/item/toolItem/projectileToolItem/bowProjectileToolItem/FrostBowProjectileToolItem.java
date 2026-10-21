package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.BowWeaponsLootTable;

public class FrostBowProjectileToolItem extends BowProjectileToolItem {
   public FrostBowProjectileToolItem() {
      super(500, BowWeaponsLootTable.bowWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(550);
      this.attackDamage.setBaseValue(22.0F).setUpgradedValue(1.0F, 128.33337F);
      this.attackRange.setBaseValue(700);
      this.velocity.setBaseValue(160);
      this.attackXOffset = 8;
      this.attackYOffset = 22;
      this.canBeUsedForRaids = true;
   }
}
