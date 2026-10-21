package necesse.inventory.lootTable.presets;

import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class CosmeticSetArmorLootTable extends OneOfLootItems {
   public static final OneOfLootItems cosmeticSetArmor = new OneOfLootItems();
   public static final CosmeticSetArmorLootTable instance = new CosmeticSetArmorLootTable();

   private CosmeticSetArmorLootTable() {
      super(cosmeticSetArmor);
   }
}
