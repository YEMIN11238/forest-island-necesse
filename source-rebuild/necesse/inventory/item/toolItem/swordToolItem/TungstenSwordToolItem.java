package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class TungstenSwordToolItem extends SwordToolItem {
   public TungstenSwordToolItem() {
      super(1300, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(65.0F).setUpgradedValue(1.0F, 93.33336F);
      this.attackRange.setBaseValue(80);
      this.knockback.setBaseValue(100);
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
