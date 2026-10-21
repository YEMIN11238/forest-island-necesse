package necesse.inventory.lootTable.presets;

import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItemList;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class ChefCoolingBoxLootTable extends LootTable {
   public static final ChefCoolingBoxLootTable instance = new ChefCoolingBoxLootTable();

   private ChefCoolingBoxLootTable() {
      OneOfLootItems simpleFood = new OneOfLootItems();

      for (Item simple : ItemCategory.foodQualityManager.getCategory("simple").getItems()) {
         simpleFood.add(LootItem.between(simple.getStringID(), 5, 10));
      }

      OneOfLootItems fineFood = new OneOfLootItems();

      for (Item fine : ItemCategory.foodQualityManager.getCategory("fine").getItems()) {
         fineFood.add(LootItem.between(fine.getStringID(), 3, 6));
      }

      OneOfLootItems gourmetFood = new OneOfLootItems();

      for (Item gourmet : ItemCategory.foodQualityManager.getCategory("gourmet").getItems()) {
         gourmetFood.add(LootItem.between(gourmet.getStringID(), 2, 4));
      }

      this.items.add(simpleFood);
      this.items.add(new ChanceLootItemList(0.75F, fineFood));
      this.items.add(new ChanceLootItemList(0.5F, gourmetFood));
   }
}
