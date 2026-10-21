package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class RareIncursionTrinketsLootTable extends OneOfLootItems {
   public static final OneOfLootItems rareIncursionTrinkets = new OneOfLootItems();
   public static final RareIncursionTrinketsLootTable instance = new RareIncursionTrinketsLootTable();

   private RareIncursionTrinketsLootTable() {
      super(rareIncursionTrinkets);
   }
}
