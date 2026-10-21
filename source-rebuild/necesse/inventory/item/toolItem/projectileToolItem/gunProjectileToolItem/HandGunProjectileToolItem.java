package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GunWeaponsLootTable;

public class HandGunProjectileToolItem extends GunProjectileToolItem {
   public HandGunProjectileToolItem() {
      super(NORMAL_AMMO_TYPES, 350, GunWeaponsLootTable.gunWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 99.166695F);
      this.attackXOffset = 12;
      this.attackYOffset = 14;
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(350);
      this.addGlobalIngredient("bulletuser");
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
   }
}
