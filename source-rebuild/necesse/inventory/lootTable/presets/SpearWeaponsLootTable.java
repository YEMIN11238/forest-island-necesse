package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class SpearWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems spearWeapons = new OneOfLootItems();
   public static final SpearWeaponsLootTable instance = new SpearWeaponsLootTable();

   private SpearWeaponsLootTable() {
      super(spearWeapons);
   }
}
