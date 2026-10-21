package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;

public class RuneboundTombLootTable extends LootTable {
   public static final RuneboundTombLootTable instance = new RuneboundTombLootTable();

   private RuneboundTombLootTable() {
      super(
         new ChanceLootItem(0.15F, "enchantingscroll"),
         CrateLootTable.basicCrate,
         CrateLootTable.basicCrate,
         LootItem.offset("coin", 35, 5),
         ChanceLootItem.between(0.3F, "amber", 1, 2),
         ChanceLootItem.between(0.5F, "dryadsapling", 3, 5),
         ChanceLootItem.between(0.7F, "bone", 3, 5)
      );
   }
}
