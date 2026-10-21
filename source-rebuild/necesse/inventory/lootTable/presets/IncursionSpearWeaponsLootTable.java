package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionSpearWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionSpearWeapons = new OneOfLootItems();
   public static final IncursionSpearWeaponsLootTable instance = new IncursionSpearWeaponsLootTable();

   private IncursionSpearWeaponsLootTable() {
      super(incursionSpearWeapons);
   }
}
