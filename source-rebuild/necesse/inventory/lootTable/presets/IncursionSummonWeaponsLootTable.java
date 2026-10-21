package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionSummonWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionSummonWeapons = new OneOfLootItems();
   public static final IncursionSummonWeaponsLootTable instance = new IncursionSummonWeaponsLootTable();

   private IncursionSummonWeaponsLootTable() {
      super(incursionSummonWeapons);
   }
}
