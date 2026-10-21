package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionArmorSetsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionArmorSets = new OneOfLootItems();
   public static final IncursionArmorSetsLootTable instance = new IncursionArmorSetsLootTable();

   private IncursionArmorSetsLootTable() {
      super(incursionArmorSets);
   }
}
