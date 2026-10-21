package necesse.inventory.item.toolItem.summonToolItem;

import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;

public class MagicBranchSummonToolItem extends SummonToolItem {
   public MagicBranchSummonToolItem() {
      super("babysnowman", FollowPosition.PYRAMID, 1.0F, 500, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(14.0F).setUpgradedValue(1.0F, 32.666676F);
   }
}
