package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class GlaiveWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems glaiveWeapons = new OneOfLootItems();
   public static final GlaiveWeaponsLootTable instance = new GlaiveWeaponsLootTable();

   private GlaiveWeaponsLootTable() {
      super(glaiveWeapons);
   }
}
