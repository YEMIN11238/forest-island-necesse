package necesse.level.maps.levelData.settlementData.settler;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.util.GameUtils;
import necesse.inventory.item.ItemCategory;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class FoodQuality {
   public final String thoughtStringID;
   public final GameMessage displayName;
   public final int happinessIncrease;
   public final String[] masterCategoryTree;
   public final ItemCategory foodCategory;
   public final ItemCategory craftingCategory;

   public FoodQuality(String thoughtStringID, GameMessage displayName, int happinessIncrease, String categorySortString, String... categoryTree) {
      this.thoughtStringID = thoughtStringID;
      this.displayName = displayName;
      this.happinessIncrease = happinessIncrease;
      this.masterCategoryTree = GameUtils.concat(new String[]{"consumable", "food"}, categoryTree);
      ItemCategory.createCategory(categorySortString, displayName, this.masterCategoryTree);
      this.foodCategory = ItemCategory.foodQualityManager.createCategory(categorySortString, displayName, categoryTree);
      this.craftingCategory = ItemCategory.craftingManager
         .createCategory(categorySortString, displayName, GameUtils.concat(new String[]{"consumable"}, categoryTree));
   }

   public void registerThought() {
      LocalMessage description = new LocalMessage("settlement", "foodmood").addReplacement("quality", this.displayName);
      SettlerThoughtRegistry.registerSettlerThought(this.thoughtStringID, new SimpleSettlerThought(description, this.happinessIncrease));
   }

   public SettlerThought getThought() {
      return SettlerThoughtRegistry.getSettlerThought(this.thoughtStringID);
   }
}
