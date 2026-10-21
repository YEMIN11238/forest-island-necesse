package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.BowWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class TungstenBowProjectileToolItem extends BowProjectileToolItem {
   public TungstenBowProjectileToolItem() {
      super(1300, BowWeaponsLootTable.bowWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(60.0F).setUpgradedValue(1.0F, 114.33337F);
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(200);
      this.attackXOffset = 12;
      this.attackYOffset = 28;
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
