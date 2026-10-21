package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;

public class StopSignSwordToolItem extends SwordToolItem {
   public StopSignSwordToolItem() {
      super(2000, null);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(56.0F).setUpgradedValue(1.0F, 80.0F);
      this.attackRange.setBaseValue(80);
      this.knockback.setBaseValue(100);
      this.attackXOffset = 18;
      this.attackYOffset = 18;
      this.canBeUsedForRaids = false;
   }
}
