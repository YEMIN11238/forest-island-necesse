package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class SummonWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems summonWeapons = new OneOfLootItems();
   public static final SummonWeaponsLootTable instance = new SummonWeaponsLootTable();

   private SummonWeaponsLootTable() {
      super(summonWeapons);
   }
}
