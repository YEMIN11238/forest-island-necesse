package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionHeadArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionHeadArmor = new OneOfLootItems();
   public static final IncursionHeadArmorLootTable instance = new IncursionHeadArmorLootTable();

   private IncursionHeadArmorLootTable() {
      super(incursionHeadArmor);
   }
}
