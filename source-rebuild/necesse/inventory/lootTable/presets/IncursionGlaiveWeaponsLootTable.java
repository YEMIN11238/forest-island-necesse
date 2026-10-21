package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionGlaiveWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionGlaiveWeapons = new OneOfLootItems();
   public static final IncursionGlaiveWeaponsLootTable instance = new IncursionGlaiveWeaponsLootTable();

   private IncursionGlaiveWeaponsLootTable() {
      super(incursionGlaiveWeapons);
   }
}
