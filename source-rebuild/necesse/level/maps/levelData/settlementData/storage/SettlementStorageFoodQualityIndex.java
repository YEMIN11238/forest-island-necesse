package necesse.level.maps.levelData.settlementData.storage;

import java.util.HashMap;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.FoodQuality;

public class SettlementStorageFoodQualityIndex extends SettlementStorageIndex {
   protected HashMap<FoodQuality, SettlementStorageRecordsRegionData> regions = new HashMap<>();
   protected int totalItems;
   protected int totalNutrition;

   public SettlementStorageFoodQualityIndex(Level level) {
      super(level);
   }

   @Override
   public void clear() {
      this.regions.clear();
   }

   protected SettlementStorageRecordsRegionData getRegionData(FoodQuality quality) {
      return this.regions
         .compute(
            quality,
            (id, last) -> last == null
               ? new SettlementStorageRecordsRegionData(this, item -> item.item.isFoodItem() && ((FoodConsumableItem)item.item).quality == quality)
               : last
         );
   }

   @Override
   public void add(InventoryItem inventoryItem, SettlementStorageRecord record) {
      if (inventoryItem.item.isFoodItem()) {
         FoodConsumableItem foodItem = (FoodConsumableItem)inventoryItem.item;
         this.totalNutrition = this.totalNutrition + foodItem.nutrition * inventoryItem.getAmount();
         this.totalItems = this.totalItems + inventoryItem.getAmount();
         this.getRegionData(foodItem.quality).add(record);
      }
   }

   public SettlementStorageRecordsRegionData getFoodQuality(FoodQuality quality) {
      return this.regions.get(quality);
   }

   public int getTotalNutrition() {
      return this.totalNutrition;
   }

   public int getTotalItems() {
      return this.totalItems;
   }
}
