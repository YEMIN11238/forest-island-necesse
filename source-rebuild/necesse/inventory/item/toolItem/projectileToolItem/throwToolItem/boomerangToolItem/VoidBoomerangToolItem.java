package necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.ThrowWeaponsLootTable;

public class VoidBoomerangToolItem extends BoomerangToolItem {
   public VoidBoomerangToolItem() {
      super(650, ThrowWeaponsLootTable.throwWeapons, "voidboomerang");
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(33.0F).setUpgradedValue(1.0F, 75.83335F);
      this.attackRange.setBaseValue(400);
      this.velocity.setBaseValue(150);
      this.stackSize = 3;
      this.resilienceGain.setBaseValue(0.5F);
      this.itemAttackerProjectileCanHitWidth = 10.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }
}
