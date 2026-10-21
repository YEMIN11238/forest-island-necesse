package necesse.inventory.item.toolItem.glaiveToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GlaiveWeaponsLootTable;

public class GoldGlaiveToolItem extends GlaiveToolItem {
   public GoldGlaiveToolItem() {
      super(350, GlaiveWeaponsLootTable.glaiveWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(23.0F).setUpgradedValue(1.0F, 81.66669F);
      this.attackRange.setBaseValue(140);
      this.knockback.setBaseValue(75);
      this.width = 20.0F;
      this.attackXOffset = 40;
      this.attackYOffset = 40;
      this.canBeUsedForRaids = true;
   }
}
