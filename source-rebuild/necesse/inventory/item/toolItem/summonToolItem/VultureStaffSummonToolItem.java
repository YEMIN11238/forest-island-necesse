package necesse.inventory.item.toolItem.summonToolItem;

import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;

public class VultureStaffSummonToolItem extends SummonToolItem {
   public VultureStaffSummonToolItem() {
      super("playervulturehatchling", FollowPosition.FLYING, 1.0F, 1050, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackDamage.setBaseValue(18.0F).setUpgradedValue(1.0F, 38.50001F);
   }
}
