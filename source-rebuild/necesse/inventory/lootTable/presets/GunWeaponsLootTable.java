package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class GunWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems gunWeapons = new OneOfLootItems();
   public static final GunWeaponsLootTable instance = new GunWeaponsLootTable();

   private GunWeaponsLootTable() {
      super(gunWeapons);
   }
}
