package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class MagicWeaponsLootTable extends OneOfLootItems {
   public static final OneOfLootItems magicWeapons = new OneOfLootItems();
   public static final MagicWeaponsLootTable instance = new MagicWeaponsLootTable();

   private MagicWeaponsLootTable() {
      super(magicWeapons);
   }
}
