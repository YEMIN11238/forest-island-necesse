package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class ToolsLootTable extends OneOfLootItems {
   public static final OneOfLootItems tools = new OneOfLootItems();
   public static final ToolsLootTable instance = new ToolsLootTable();

   private ToolsLootTable() {
      super(tools);
   }
}
