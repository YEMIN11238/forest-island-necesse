package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class CosmeticArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems cosmeticArmor = new OneOfLootItems();
   public static final CosmeticArmorLootTable instance = new CosmeticArmorLootTable();

   private CosmeticArmorLootTable() {
      super(cosmeticArmor);
   }
}
