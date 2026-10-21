package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionThrowWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionThrowWeapons = new OneOfLootItems();
   public static final IncursionThrowWeaponsLootTable instance = new IncursionThrowWeaponsLootTable();

   private IncursionThrowWeaponsLootTable() {
      super(incursionThrowWeapons);
   }
}
