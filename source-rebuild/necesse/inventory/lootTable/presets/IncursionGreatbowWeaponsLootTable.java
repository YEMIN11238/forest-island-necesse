package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionGreatbowWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionGreatbowWeapons = new OneOfLootItems();
   public static final IncursionGreatbowWeaponsLootTable instance = new IncursionGreatbowWeaponsLootTable();

   private IncursionGreatbowWeaponsLootTable() {
      super(incursionGreatbowWeapons);
   }
}
