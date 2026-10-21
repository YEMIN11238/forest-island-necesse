package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class ArmorSetsLootTable extends OneOfLootItems {
   public static final OneOfLootItems armorSets = new OneOfLootItems();
   public static final ArmorSetsLootTable instance = new ArmorSetsLootTable();

   private ArmorSetsLootTable() {
      super(armorSets);
   }
}
