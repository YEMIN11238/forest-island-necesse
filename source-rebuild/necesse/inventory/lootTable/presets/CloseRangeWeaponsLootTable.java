package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class CloseRangeWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems closeRangeWeapons = new OneOfLootItems();
   public static final CloseRangeWeaponsLootTable instance = new CloseRangeWeaponsLootTable();

   private CloseRangeWeaponsLootTable() {
      super(closeRangeWeapons);
   }
}
