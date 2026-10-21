package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionGreatswordWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionGreatswordWeapons = new OneOfLootItems();
   public static final IncursionGreatswordWeaponsLootTable instance = new IncursionGreatswordWeaponsLootTable();

   private IncursionGreatswordWeaponsLootTable() {
      super(incursionGreatswordWeapons);
   }
}
