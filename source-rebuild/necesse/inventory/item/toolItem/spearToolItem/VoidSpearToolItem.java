package necesse.inventory.item.toolItem.spearToolItem;

import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SpearWeaponsLootTable;

public class VoidSpearToolItem extends SpearToolItem {
   public VoidSpearToolItem() {
      super(650, SpearWeaponsLootTable.spearWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(28.0F).setUpgradedValue(1.0F, 70.000015F);
      this.attackRange.setBaseValue(140);
      this.knockback.setBaseValue(25);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
   }
}
