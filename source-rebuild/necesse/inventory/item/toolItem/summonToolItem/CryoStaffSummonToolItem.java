package necesse.inventory.item.toolItem.summonToolItem;

import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;

public class CryoStaffSummonToolItem extends SummonToolItem {
   public CryoStaffSummonToolItem() {
      super("playercryoflake", FollowPosition.FLYING_CIRCLE, 1.0F, 1450, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(29.0F).setUpgradedValue(1.0F, 38.50001F);
   }
}
