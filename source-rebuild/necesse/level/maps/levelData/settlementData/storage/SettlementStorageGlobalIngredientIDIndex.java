package necesse.level.maps.levelData.settlementData.storage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Predicate;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;

public class SettlementStorageGlobalIngredientIDIndex extends SettlementStorageIndex {
   protected HashMap<Integer, SettlementStorageRecordsRegionData> regions = new HashMap<>();

   public SettlementStorageGlobalIngredientIDIndex(Level level) {
      super(level);
   }

   @Override
   public void clear() {
      this.regions.clear();
   }

   protected SettlementStorageRecordsRegionData getRegionData(int globalIngredientID) {
      return this.regions
         .compute(
            globalIngredientID,
            (id, last) -> last == null ? new SettlementStorageRecordsRegionData(this, item -> item.item.isGlobalIngredient(globalIngredientID)) : last
         );
   }

   @Override
   public void add(InventoryItem inventoryItem, SettlementStorageRecord record) {
      for (int globalIngredientID : inventoryItem.item.getGlobalIngredients()) {
         this.getRegionData(globalIngredientID).add(record);
      }
   }

   public SettlementStorageRecordsRegionData getGlobalIngredient(int globalIngredientID) {
      return this.regions.get(globalIngredientID);
   }

   public LinkedList<SettlementStoragePickupSlot> findPickupSlots(
      int globalIngredientID, EntityJobWorker worker, Predicate<InventoryItem> filter, int minAmount, int maxAmount
   ) {
      SettlementStorageRecordsRegionData data = this.getGlobalIngredient(globalIngredientID);
      return data != null ? data.startFinder(worker).findPickupSlots(minAmount, maxAmount, filter) : null;
   }
}
