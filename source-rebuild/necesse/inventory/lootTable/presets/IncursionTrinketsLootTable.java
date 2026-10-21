package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionTrinketsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionTrinkets = new OneOfLootItems();
   public static final IncursionTrinketsLootTable instance = new IncursionTrinketsLootTable();

   private IncursionTrinketsLootTable() {
      super(incursionTrinkets);
   }
}
