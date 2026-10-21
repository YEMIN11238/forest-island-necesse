package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class ThrowWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems throwWeapons = new OneOfLootItems();
   public static final ThrowWeaponsLootTable instance = new ThrowWeaponsLootTable();

   private ThrowWeaponsLootTable() {
      super(throwWeapons);
   }
}
