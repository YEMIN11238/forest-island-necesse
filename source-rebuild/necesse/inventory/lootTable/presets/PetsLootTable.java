package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class PetsLootTable extends OneOfLootItems {
   public static final OneOfLootItems petsLootTable = new OneOfLootItems(
      new LootItem("eyeinaportal"),
      new LootItem("weticicle"),
      new LootItem("exoticseeds"),
      new LootItem("magicstilts"),
      new LootItem("squeakytoy"),
      new LootItem("grizzlycub"),
      new LootItem("dragonegg"),
      new LootItem("frozenberry")
   );
   public static final PetsLootTable instance = new PetsLootTable();

   private PetsLootTable() {
      super(petsLootTable);
   }
}
