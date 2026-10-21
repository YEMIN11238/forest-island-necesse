package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.BowWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class AntiqueBowProjectileToolItem extends BowProjectileToolItem {
   public AntiqueBowProjectileToolItem() {
      super(1850, BowWeaponsLootTable.bowWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(450);
      this.attackDamage.setBaseValue(95.0F).setUpgradedValue(1.0F, 112.00003F);
      this.attackRange.setBaseValue(1000);
      this.velocity.setBaseValue(240);
      this.attackXOffset = 12;
      this.attackYOffset = 12;
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
