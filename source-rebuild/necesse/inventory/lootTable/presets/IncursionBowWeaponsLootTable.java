package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionBowWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionBowWeapons = new OneOfLootItems();
   public static final IncursionBowWeaponsLootTable instance = new IncursionBowWeaponsLootTable();

   private IncursionBowWeaponsLootTable() {
      super(incursionBowWeapons);
   }
}
