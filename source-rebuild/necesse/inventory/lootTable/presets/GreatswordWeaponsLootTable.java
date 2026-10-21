package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class GreatswordWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems greatswordWeapons = new OneOfLootItems();
   public static final GreatswordWeaponsLootTable instance = new GreatswordWeaponsLootTable();

   private GreatswordWeaponsLootTable() {
      super(greatswordWeapons);
   }
}
