package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class ReinforcedKatanaToolItem extends KatanaToolItem {
   public ReinforcedKatanaToolItem() {
      super(1300, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(200);
      this.attackDamage.setBaseValue(62.0F).setUpgradedValue(1.0F, 110.83337F);
      this.attackRange.setBaseValue(100);
      this.knockback.setBaseValue(75);
      this.resilienceGain.setBaseValue(1.5F).setUpgradedValue(1.0F, 1.5F).setUpgradedValue(10.0F, 2.0F);
      this.maxDashStacks.setBaseValue(15);
      this.dashRange.setBaseValue(400);
      this.attackXOffset = 4;
      this.attackYOffset = 4;
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
