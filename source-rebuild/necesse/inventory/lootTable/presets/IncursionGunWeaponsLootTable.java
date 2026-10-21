package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class IncursionGunWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems incursionGunWeapons = new OneOfLootItems();
   public static final IncursionGunWeaponsLootTable instance = new IncursionGunWeaponsLootTable();

   private IncursionGunWeaponsLootTable() {
      super(incursionGunWeapons);
   }
}
