package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionFeetArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionFeetArmor = new OneOfLootItems();
   public static final IncursionFeetArmorLootTable instance = new IncursionFeetArmorLootTable();

   private IncursionFeetArmorLootTable() {
      super(incursionFeetArmor);
   }
}
