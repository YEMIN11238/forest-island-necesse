package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class HeadArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems headArmor = new OneOfLootItems();
   public static final HeadArmorLootTable instance = new HeadArmorLootTable();

   private HeadArmorLootTable() {
      super(headArmor);
   }
}
