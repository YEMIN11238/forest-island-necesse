package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class FeetArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems feetArmor = new OneOfLootItems();
   public static final FeetArmorLootTable instance = new FeetArmorLootTable();

   private FeetArmorLootTable() {
      super(feetArmor);
   }
}
