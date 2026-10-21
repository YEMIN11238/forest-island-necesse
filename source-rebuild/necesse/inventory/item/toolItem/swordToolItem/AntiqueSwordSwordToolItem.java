package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class AntiqueSwordSwordToolItem extends SwordToolItem {
   public AntiqueSwordSwordToolItem() {
      super(1850, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(92.0F).setUpgradedValue(1.0F, 116.6667F);
      this.attackRange.setBaseValue(90);
      this.knockback.setBaseValue(125);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
