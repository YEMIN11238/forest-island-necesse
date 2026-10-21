package necesse.level.maps.levelData.settlementData.storage;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.util.GameLinkedList;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupFuture;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;

public class ItemFinder {
   private SettlementStorageRecordsRegionData regionData;
   private Point currentTile;
   private GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>> records;

   public ItemFinder(
      SettlementStorageRecordsRegionData regionData,
      Point startTile,
      GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>> records,
      Predicate<InventoryItem> indexFilter
   ) {
      this.regionData = regionData;
      this.currentTile = startTile;
      this.records = records;
   }

   public LinkedList<SettlementStoragePickupSlot> findPickupSlots(int minAmount, int maxAmount, Predicate<InventoryItem> itemFilter) {
      return Performance.record(
         this.regionData.index.level.tickManager(),
         "findItems",
         () -> {
            int foundAmount = 0;
            Point startTile = this.currentTile;
            HashSet<Point> beenAt = new HashSet<>();
            LinkedList<SettlementStoragePickupFuture> foundSlots = new LinkedList<>();

            while (foundAmount < maxAmount && !this.records.isEmpty()) {
               GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>>.Element best = this.records
                  .streamElements()
                  .filter(e -> !beenAt.contains(((Entry)e.object).getKey()))
                  .min(Comparator.comparingDouble(e -> this.currentTile.distance((Point2D)((Entry)e.object).getKey())))
                  .orElse(null);
               if (best == null) {
                  break;
               }

               beenAt.add((Point)((Entry)best.object).getKey());
               Point tile = (Point)((Entry)best.object).getKey();
               GameLinkedList<SettlementStorageRecord> slots = (GameLinkedList<SettlementStorageRecord>)((Entry)best.object).getValue();
               GameLinkedList<SettlementStorageRecord>.Element currentElement = slots.getLastElement();
               boolean found = false;

               while (true) {
                  if (currentElement != null) {
                     GameLinkedList<SettlementStorageRecord>.Element nextElement = currentElement.prev();

                     try {
                        InventoryItem item = this.regionData.validateItem(currentElement);
                        if (item == null) {
                           continue;
                        }

                        SettlementStorageRecord record = (SettlementStorageRecord)currentElement.object;
                        found = true;
                        if (itemFilter != null && !itemFilter.test(item)) {
                           continue;
                        }

                        int queryAmount = Math.min(record.itemAmount, maxAmount - foundAmount);
                        GameLinkedList<SettlementStorageRecord>.Element finalCurrentElement = currentElement;
                        SettlementStoragePickupFuture futureReserve = record.storage
                           .getFutureReserve(record.inventorySlot, item, queryAmount, acceptedSlot -> {
                              record.itemAmount = record.itemAmount - acceptedSlot.item.getAmount();
                              if (record.itemAmount <= 0 && !finalCurrentElement.isRemoved()) {
                                 finalCurrentElement.remove();
                              }
                           });
                        if (futureReserve == null) {
                           continue;
                        }

                        foundSlots.add(futureReserve);
                        foundAmount += futureReserve.item.getAmount();
                        this.currentTile = tile;
                        if (foundAmount < maxAmount) {
                           continue;
                        }
                     } finally {
                        ;
                     }
                  }

                  if (!found) {
                     best.remove();
                  }
                  break;
               }
            }

            if (foundAmount < minAmount) {
               this.currentTile = startTile;
               return null;
            }

            LinkedList<SettlementStoragePickupSlot> slots = new LinkedList<>();

            for (SettlementStoragePickupFuture foundSlot : foundSlots) {
               slots.add(foundSlot.accept(foundSlot.item.getAmount()));
            }

            return slots;
         }
      );
   }

   public SettlementStoragePickupSlot findFirstItemPickup(Predicate<InventoryItem> itemFilter) {
      return Performance.record(
         this.regionData.index.level.tickManager(),
         "findItems",
         () -> {
            HashSet<Point> beenAt = new HashSet<>();

            while (!this.records.isEmpty()) {
               GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>>.Element best = this.records
                  .streamElements()
                  .filter(e -> !beenAt.contains(((Entry)e.object).getKey()))
                  .min(Comparator.comparingDouble(e -> this.currentTile.distance((Point2D)((Entry)e.object).getKey())))
                  .orElse(null);
               if (best == null) {
                  break;
               }

               beenAt.add((Point)((Entry)best.object).getKey());
               Point tile = (Point)((Entry)best.object).getKey();
               GameLinkedList<SettlementStorageRecord> slots = (GameLinkedList<SettlementStorageRecord>)((Entry)best.object).getValue();
               GameLinkedList<SettlementStorageRecord>.Element currentElement = slots.getLastElement();
               boolean found = false;

               while (currentElement != null) {
                  GameLinkedList<SettlementStorageRecord>.Element nextElement = currentElement.prev();

                  try {
                     InventoryItem item = this.regionData.validateItem(currentElement);
                     if (item != null) {
                        SettlementStorageRecord record = (SettlementStorageRecord)currentElement.object;
                        if (itemFilter == null || itemFilter.test(item)) {
                           found = true;
                           SettlementStoragePickupSlot reserve = record.storage.reserve(record.inventorySlot, item, 1);
                           if (reserve != null) {
                              this.currentTile = tile;
                              record.itemAmount--;
                              if (record.itemAmount <= 0 && !currentElement.isRemoved()) {
                                 currentElement.remove();
                              }

                              return reserve;
                           }
                        }
                     }
                  } finally {
                     ;
                  }
               }

               if (!found) {
                  best.remove();
               }
            }

            return null;
         }
      );
   }

   public SettlementStoragePickupSlot findBestItem(Predicate<InventoryItem> itemFilter, Comparator<SettlementStoragePickupFuture> comparator) {
      return Performance.record(
         this.regionData.index.level.tickManager(),
         "findItems",
         () -> {
            HashSet<Point> beenAt = new HashSet<>();
            SettlementStoragePickupFuture bestSlot = null;

            while (!this.records.isEmpty()) {
               GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>>.Element best = this.records
                  .streamElements()
                  .filter(e -> !beenAt.contains(((Entry)e.object).getKey()))
                  .min(Comparator.comparingDouble(e -> this.currentTile.distance((Point2D)((Entry)e.object).getKey())))
                  .orElse(null);
               if (best == null) {
                  break;
               }

               beenAt.add((Point)((Entry)best.object).getKey());
               GameLinkedList<SettlementStorageRecord> slots = (GameLinkedList<SettlementStorageRecord>)((Entry)best.object).getValue();
               GameLinkedList<SettlementStorageRecord>.Element currentElement = slots.getLastElement();

               while (currentElement != null) {
                  GameLinkedList<SettlementStorageRecord>.Element nextElement = currentElement.prev();

                  try {
                     InventoryItem item = this.regionData.validateItem(currentElement);
                     if (item != null) {
                        SettlementStorageRecord record = (SettlementStorageRecord)currentElement.object;
                        if (itemFilter == null || itemFilter.test(item)) {
                           GameLinkedList<SettlementStorageRecord>.Element finalCurrentElement = currentElement;
                           SettlementStoragePickupFuture nextSlot = record.storage.getFutureReserve(record.inventorySlot, item, 1, acceptedSlot -> {
                              record.itemAmount = record.itemAmount - acceptedSlot.item.getAmount();
                              if (record.itemAmount <= 0 && !finalCurrentElement.isRemoved()) {
                                 finalCurrentElement.remove();
                              }
                           });
                           if (bestSlot == null || comparator.compare(bestSlot, nextSlot) < 0) {
                              bestSlot = nextSlot;
                           }
                        }
                     }
                  } finally {
                     ;
                  }
               }
            }

            if (bestSlot != null) {
               this.currentTile = new Point(bestSlot.storage.tileX, bestSlot.storage.tileY);
               return bestSlot.accept(1);
            } else {
               return null;
            }
         }
      );
   }
}
