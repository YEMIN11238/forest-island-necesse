package necesse.inventory.item.toolItem.glaiveToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GlaiveWeaponsLootTable;

public class FrostGlaiveToolItem extends GlaiveToolItem {
   public FrostGlaiveToolItem() {
      super(500, GlaiveWeaponsLootTable.glaiveWeapons);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(26.0F).setUpgradedValue(1.0F, 81.66669F);
      this.attackRange.setBaseValue(140);
      this.knockback.setBaseValue(75);
      this.width = 20.0F;
      this.attackXOffset = 40;
      this.attackYOffset = 40;
      this.canBeUsedForRaids = true;
   }
}
