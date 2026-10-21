package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class VendingMachineLootTable extends LootTable {
   public static final VendingMachineLootTable instance = new VendingMachineLootTable();

   private VendingMachineLootTable() {
      super(
         new OneOfLootItems(new LootItem("fireresistancepotion"), new LootItem("superiorhealthpotion")),
         new OneOfLootItems(new LootItem("sweetlemonade"), new LootItem("fruitsmoothie"), new LootItem("blackcoffee")),
         new OneOfLootItems(new LootItem("lemontart"), new LootItem("blueberrycake"), new LootItem("donut")),
         LootItem.offset("spoiledfood", 10, 5)
      );
   }
}
