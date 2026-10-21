package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class TrinketsLootTable extends OneOfLootItems {
   public static final OneOfLootItems trinkets = new OneOfLootItems();
   public static final TrinketsLootTable instance = new TrinketsLootTable();

   private TrinketsLootTable() {
      super(trinkets);
   }
}
