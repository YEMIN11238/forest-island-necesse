package necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.ThrowWeaponsLootTable;

public class WoodBoomerangToolItem extends BoomerangToolItem {
   public WoodBoomerangToolItem() {
      super(100, ThrowWeaponsLootTable.throwWeapons, "woodboomerang");
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(15.0F).setUpgradedValue(1.0F, 105.00003F);
      this.attackRange.setBaseValue(200);
      this.velocity.setBaseValue(100);
      this.itemAttackerProjectileCanHitWidth = 10.0F;
   }
}
