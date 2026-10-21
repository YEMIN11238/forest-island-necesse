package necesse.inventory.item.toolItem.summonToolItem;

import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.inventory.item.Item;

public class PirateHookSummonToolItem extends SummonToolItem {
   public PirateHookSummonToolItem() {
      super("playerghostship", FollowPosition.WIDE_CIRCLE_MOVEMENT, 2.0F, 1050, null);
      this.rarity = Item.Rarity.RARE;
      this.attackDamage.setBaseValue(150.0F).setUpgradedValue(1.0F, 262.50006F);
   }
}
