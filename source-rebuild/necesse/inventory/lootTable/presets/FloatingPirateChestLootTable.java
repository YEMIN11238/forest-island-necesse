package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class FloatingPirateChestLootTable extends LootTable {
   public static final OneOfLootItems gold = new OneOfLootItems(LootItem.offset("goldbar", 6, 2), LootItem.offset("goldore", 11, 5));
   public static final OneOfLootItems goldTools = new OneOfLootItems(
      new LootItem("goldpickaxe"),
      new LootItem("goldshovel"),
      new LootItem("goldaxe"),
      new LootItem("goldspear"),
      new LootItem("goldglaive"),
      new LootItem("goldbow"),
      new LootItem("goldgreatbow"),
      new LootItem("goldfishingrod"),
      new LootItem("goldsword")
   );
   public static final OneOfLootItems goldWear = new OneOfLootItems(
      new LootItem("goldboots"), new LootItem("goldchestplate"), new LootItem("goldhelmet"), new LootItem("goldcrown")
   );
   public static final OneOfLootItems goldDecor = new OneOfLootItems(
      new LootItem("golddinnertable"),
      new LootItem("goldstreetlamp"),
      new LootItem("goldchalice"),
      new LootItem("golddoublestreetlamp"),
      LootItem.offset("goldlamp", 3, 1)
   );
   public static final OneOfLootItems extra = new OneOfLootItems(
      LootItem.offset("mapfragment", 3, 1),
      new LootItem("piratebandana"),
      new LootItem("pirateboots"),
      new LootItem("pirateshirt"),
      new LootItem("captainshat"),
      new LootItem("captainsshirt"),
      new LootItem("captainsboots"),
      new LootItem("pirateeyepatch")
   );
   public static final FloatingPirateChestLootTable instance = new FloatingPirateChestLootTable();

   private FloatingPirateChestLootTable() {
      super(
         LootItem.offset("coin", 150, 80),
         gold,
         LootItem.offset("sapphire", 3, 1),
         new OneOfLootItems(goldTools, goldWear, goldDecor),
         new OneOfLootItems(new LootItem("piratemap"), new LootItem("dungeonmap")),
         extra
      );
   }
}
