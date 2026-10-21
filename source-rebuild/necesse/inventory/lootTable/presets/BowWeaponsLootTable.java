package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class BowWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems bowWeapons = new OneOfLootItems();
   public static final BowWeaponsLootTable instance = new BowWeaponsLootTable();

   private BowWeaponsLootTable() {
      super(bowWeapons);
   }
}
