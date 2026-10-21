package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;

public class IncursionCrateWithAltarDustLootTable extends LootTable {
   public static final IncursionCrateWithAltarDustLootTable instance = new IncursionCrateWithAltarDustLootTable();

   private IncursionCrateWithAltarDustLootTable() {
      super(IncursionCrateLootTable.instance, ChanceLootItem.offset(0.5F, "altardust", 5, 3).splitItems(2));
   }
}
