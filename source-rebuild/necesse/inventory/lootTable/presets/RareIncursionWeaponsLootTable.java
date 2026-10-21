package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class RareIncursionWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems rareIncursionWeapons = new OneOfLootItems();
   public static final RareIncursionWeaponsLootTable instance = new RareIncursionWeaponsLootTable();

   private RareIncursionWeaponsLootTable() {
      super(rareIncursionWeapons);
   }
}
