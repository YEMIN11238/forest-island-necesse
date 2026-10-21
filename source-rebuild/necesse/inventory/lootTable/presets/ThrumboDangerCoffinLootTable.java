package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.CountOfTicketLootItems;
import necesse.inventory.lootTable.lootItem.LootItem;

public class ThrumboDangerCoffinLootTable extends LootTable {
   public static CountOfTicketLootItems lottery = new CountOfTicketLootItems(
      2,
      new LootItem("goldsword"),
      new LootItem("goldspear"),
      new LootItem("goldglaive"),
      new LootItem("goldbow"),
      new LootItem("goldgreatbow"),
      new LootItem("brokencoppertool"),
      new LootItem("brokenirontool"),
      LootItem.between("goldbar", 3, 7),
      LootItem.between("ironbar", 5, 10),
      LootItem.between("stone", 50, 5)
   );
   public static final ThrumboDangerCoffinLootTable instance = new ThrumboDangerCoffinLootTable();

   private ThrumboDangerCoffinLootTable() {
      super(lottery, LootItem.offset("torch", 12, 4), LootItem.offset("coin", 140, 40));
   }
}
