package necesse.level.maps.levelData.settlementData;

import necesse.inventory.InventoryItem;

public abstract class SettlementStoragePickupFuture {
   public final LevelStorage storage;
   public final InventoryItem item;

   public SettlementStoragePickupFuture(LevelStorage storage, InventoryItem item) {
      this.storage = storage;
      this.item = item;
   }

   public abstract SettlementStoragePickupSlot accept(int var1);
}
