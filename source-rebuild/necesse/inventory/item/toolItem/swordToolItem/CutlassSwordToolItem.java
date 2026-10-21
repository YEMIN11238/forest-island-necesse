package necesse.inventory.item.toolItem.swordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class CutlassSwordToolItem extends SwordToolItem {
   public CutlassSwordToolItem() {
      super(1150, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(58.0F).setUpgradedValue(1.0F, 105.00003F);
      this.attackRange.setBaseValue(65);
      this.knockback.setBaseValue(80);
      this.attackXOffset = 8;
      this.attackYOffset = 8;
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
