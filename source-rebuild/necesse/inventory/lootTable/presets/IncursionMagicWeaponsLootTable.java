package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionMagicWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionMagicWeapons = new OneOfLootItems();
   public static final IncursionMagicWeaponsLootTable instance = new IncursionMagicWeaponsLootTable();

   private IncursionMagicWeaponsLootTable() {
      super(incursionMagicWeapons);
   }
}
