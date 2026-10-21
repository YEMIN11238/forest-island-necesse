package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GreatswordWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class QuartzGreatswordToolItem extends GreatswordToolItem {
   public QuartzGreatswordToolItem() {
      super(1000, GreatswordWeaponsLootTable.greatswordWeapons, getThreeChargeLevels(500, 600, 700));
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(122.0F).setUpgradedValue(1.0F, 179.66672F);
      this.attackRange.setBaseValue(126);
      this.knockback.setBaseValue(150);
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
