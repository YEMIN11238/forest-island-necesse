package necesse.inventory.item.toolItem.summonToolItem;

import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;

public class SpiderStaffSummonToolItem extends SummonToolItem {
   public SpiderStaffSummonToolItem() {
      super("babyspider", FollowPosition.PYRAMID, 1.0F, 350, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(14.0F).setUpgradedValue(1.0F, 33.833344F);
   }
}
