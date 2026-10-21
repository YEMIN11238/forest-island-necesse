package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class RareIncursionArmorSetsLootTable extends OneOfLootItems {
   public static final OneOfLootItems rareIncursionArmorSetsRewards = new OneOfLootItems();
   public static final RareIncursionArmorSetsLootTable instance = new RareIncursionArmorSetsLootTable();

   private RareIncursionArmorSetsLootTable() {
      super(rareIncursionArmorSetsRewards);
   }
}
