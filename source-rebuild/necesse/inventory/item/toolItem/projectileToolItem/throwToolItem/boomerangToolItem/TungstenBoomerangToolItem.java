package necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.ThrowWeaponsLootTable;

public class TungstenBoomerangToolItem extends BoomerangToolItem {
   public TungstenBoomerangToolItem() {
      super(1300, ThrowWeaponsLootTable.throwWeapons, "tungstenboomerang");
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(300);
      this.attackCooldownTime.setBaseValue(400);
      this.attackDamage.setBaseValue(60.0F).setUpgradedValue(1.0F, 93.33336F);
      this.attackRange.setBaseValue(600);
      this.velocity.setBaseValue(180);
      this.stackSize = 4;
      this.resilienceGain.setBaseValue(0.75F);
      this.knockback.setBaseValue(100);
      this.itemAttackerProjectileCanHitWidth = 18.0F;
   }
}
