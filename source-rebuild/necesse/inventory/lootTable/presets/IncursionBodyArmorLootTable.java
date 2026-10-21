package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionBodyArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionBodyArmor = new OneOfLootItems();
   public static final IncursionBodyArmorLootTable instance = new IncursionBodyArmorLootTable();

   private IncursionBodyArmorLootTable() {
      super(incursionBodyArmor);
   }
}
