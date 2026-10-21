package necesse.inventory.item.toolItem.spearToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class IvySpearToolItem extends SpearToolItem {
   public IvySpearToolItem() {
      super(850, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(32.0F).setUpgradedValue(1.0F, 70.000015F);
      this.attackRange.setBaseValue(120);
      this.knockback.setBaseValue(25);
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
