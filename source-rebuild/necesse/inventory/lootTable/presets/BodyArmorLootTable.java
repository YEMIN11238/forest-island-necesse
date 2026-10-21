package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class BodyArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems bodyArmor = new OneOfLootItems();
   public static final BodyArmorLootTable instance = new BodyArmorLootTable();

   private BodyArmorLootTable() {
      super(bodyArmor);
   }
}
