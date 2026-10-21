package necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.ThrowWeaponsLootTable;

public class SpiderBoomerangToolItem extends BoomerangToolItem {
   public SpiderBoomerangToolItem() {
      super(350, ThrowWeaponsLootTable.throwWeapons, "spiderboomerang");
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(25.0F).setUpgradedValue(1.0F, 99.166695F);
      this.attackRange.setBaseValue(400);
      this.velocity.setBaseValue(125);
      this.itemAttackerProjectileCanHitWidth = 8.0F;
   }
}
