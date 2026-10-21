package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionCloseRangeWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionCloseRangeWeapons = new OneOfLootItems();
   public static final IncursionCloseRangeWeaponsLootTable instance = new IncursionCloseRangeWeaponsLootTable();

   private IncursionCloseRangeWeaponsLootTable() {
      super(incursionCloseRangeWeapons);
   }
}
