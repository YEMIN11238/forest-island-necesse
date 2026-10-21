package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem;

import java.awt.Color;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GreatbowWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class TungstenGreatbowProjectileToolItem extends GreatbowProjectileToolItem {
   public TungstenGreatbowProjectileToolItem() {
      super(1300, GreatbowWeaponsLootTable.greatbowWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(600);
      this.attackDamage.setBaseValue(120.0F).setUpgradedValue(1.0F, 170.33337F);
      this.attackRange.setBaseValue(1200);
      this.velocity.setBaseValue(400);
      this.attackXOffset = 10;
      this.attackYOffset = 36;
      this.particleColor = new Color(56, 70, 84);
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }
}
