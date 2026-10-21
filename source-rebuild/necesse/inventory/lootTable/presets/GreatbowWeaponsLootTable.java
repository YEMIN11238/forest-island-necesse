package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class GreatbowWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems greatbowWeapons = new OneOfLootItems();
   public static final GreatbowWeaponsLootTable instance = new GreatbowWeaponsLootTable();

   private GreatbowWeaponsLootTable() {
      super(greatbowWeapons);
   }
}
