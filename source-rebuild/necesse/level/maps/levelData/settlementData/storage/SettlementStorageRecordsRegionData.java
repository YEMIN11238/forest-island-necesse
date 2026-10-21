package necesse.level.maps.levelData.settlementData.storage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import necesse.engine.GameTileRange;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.HashMapGameLinkedList;
import necesse.engine.util.PointHashMap;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.regionSystem.LevelRegionsSpliterator;

public class SettlementStorageRecordsRegionData {
   protected SettlementStorageIndex index;
   protected PointHashMap<HashMapGameLinkedList<Point, SettlementStorageRecord>> records;
   protected Predicate<InventoryItem> indexFilter;
   protected int totalItems;

   public SettlementStorageRecordsRegionData(SettlementStorageIndex index, Predicate<InventoryItem> indexFilter) {
      this.index = index;
      this.records = new PointHashMap<>();
      this.indexFilter = indexFilter;
   }

   public HashMapGameLinkedList<Point, SettlementStorageRecord> getRecordsInRegion(int regionX, int regionY) {
      HashMapGameLinkedList<Point, SettlementStorageRecord> list = this.records.get(regionX, regionY);
      if (list == null) {
         list = new HashMapGameLinkedList<>();
         this.records.put(regionX, regionY, list);
      }

      return list;
   }

   public Stream<SettlementStorageRecord> streamRecordsInTile(int tileX, int tileY) {
      int regionX = this.index.level.regionManager.getRegionCoordByTile(tileX);
      int regionY = this.index.level.regionManager.getRegionCoordByTile(tileY);
      return this.getRecordsInRegion(regionX, regionY).stream(new Point(tileX, tileY));
   }

   public Stream<HashMapGameLinkedList<Point, SettlementStorageRecord>> streamRecordsInRegionsShape(
      int minX, int minY, int maxX, int maxY, int extraRegionRange
   ) {
      return new LevelRegionsSpliterator(this.index.level, minX, minY, maxX, maxY, extraRegionRange).stream().map(rp -> this.getRecordsInRegion(rp.x, rp.y));
   }

   public Stream<HashMapGameLinkedList<Point, SettlementStorageRecord>> streamRecordsInRegionsShape(Shape shape, int extraRegionRange) {
      Rectangle bounds = shape.getBounds();
      return new LevelRegionsSpliterator(this.index.level, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, extraRegionRange)
         .stream()
         .map(rp -> this.getRecordsInRegion(rp.x, rp.y));
   }

   public Stream<HashMapGameLinkedList<Point, SettlementStorageRecord>> streamInRegionsInRange(float levelX, float levelY, int range) {
      int minX = (int)(levelX - range);
      int minY = (int)(levelY - range);
      int maxX = (int)(levelX + range);
      int maxY = (int)(levelY + range);
      return this.streamRecordsInRegionsShape(minX, minY, maxX, maxY, 0);
   }

   public Stream<HashMapGameLinkedList<Point, SettlementStorageRecord>> streamInRegionsInTileRange(int levelX, int levelY, int tileRange) {
      int levelRange = tileRange * 32;
      int minX = levelX - levelRange;
      int minY = levelY - levelRange;
      int maxX = levelX + levelRange;
      int maxY = levelY + levelRange;
      return this.streamRecordsInRegionsShape(minX, minY, maxX, maxY, 0);
   }

   public Stream<HashMapGameLinkedList<Point, SettlementStorageRecord>> streamAllRecords() {
      Builder<HashMapGameLinkedList<Point, SettlementStorageRecord>> builder = Stream.builder();

      for (HashMapGameLinkedList<Point, SettlementStorageRecord> record : this.records.values()) {
         if (record != null) {
            builder.add(record);
         }
      }

      return builder.build();
   }

   public void add(SettlementStorageRecord record) {
      int regionX = this.index.level.regionManager.getRegionCoordByTile(record.storage.tileX);
      int regionY = this.index.level.regionManager.getRegionCoordByTile(record.storage.tileY);
      HashMapGameLinkedList<Point, SettlementStorageRecord> map = this.getRecordsInRegion(regionX, regionY);
      map.add(new Point(record.storage.tileX, record.storage.tileY), record);
      this.totalItems = this.totalItems + record.itemAmount;
   }

   public InventoryItem validateItem(SettlementStorageRecord record, Runnable onShouldRemove) {
      if (onShouldRemove == null) {
         onShouldRemove = () -> {};
      }

      int slot = record.inventorySlot;
      InventoryRange range = record.storage.getInventoryRange();
      if (range == null) {
         onShouldRemove.run();
         return null;
      }

      if (slot >= range.startSlot && slot <= range.endSlot) {
         InventoryItem item = range.inventory.getItem(slot);
         if (item != null && this.indexFilter.test(item)) {
            record.itemAmount = item.getAmount();
            return item;
         } else {
            onShouldRemove.run();
            return null;
         }
      } else {
         onShouldRemove.run();
         return null;
      }
   }

   public InventoryItem validateItem(GameLinkedList<SettlementStorageRecord>.Element element) {
      return this.validateItem((SettlementStorageRecord)element.object, element::remove);
   }

   public GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>> getRecords(EntityJobWorker worker) {
      ZoneTester restrictZone = worker.getJobRestrictZone();
      return this.getRecords(worker, t -> restrictZone.containsTile(t.x, t.y) && worker.estimateCanMoveTo(t.x, t.y, true));
   }

   public GameLinkedList<Entry<Point, GameLinkedList<SettlementStorageRecord>>> getRecords(EntityJobWorker worker, Predicate<Point> isValidPickupTile) {
      return this.streamRecordsInRegionsShape(worker.getJobSearchBounds(), 0)
         .flatMap(list -> list.entrySet().stream())
         .filter(e -> isValidPickupTile.test(e.getKey()))
         .collect(GameLinkedList::new, GameLinkedList::add, GameLinkedList::addAll);
   }

   public ItemFinder startFinder(EntityJobWorker worker) {
      ZoneTester restrictZone = worker.getJobRestrictZone();
      return this.startFinder(worker, t -> restrictZone.containsTile(t.x, t.y) && worker.estimateCanMoveTo(t.x, t.y, true));
   }

   public ItemFinder startFinder(EntityJobWorker worker, Predicate<Point> isValidPickupTile) {
      return Performance.record(
         this.index.level.tickManager(),
         "findItems",
         () -> new ItemFinder(this, worker.getJobSearchTile(), this.getRecords(worker, isValidPickupTile), this.indexFilter)
      );
   }

   public int countItems(Point baseTile, GameTileRange range, Predicate<Point> isValidTile, Predicate<InventoryItem> itemFilter) {
      Level level = this.index.level;
      return Performance.record(
         level.tickManager(),
         "countItems",
         () -> this.streamRecordsInRegionsShape(range.getRangeBounds(baseTile), 0)
            .flatMap(list -> list.entrySet().stream())
            .filter(e -> range.isWithinRange(baseTile, e.getKey()))
            .filter(e -> isValidTile.test(e.getKey()))
            .reduce(0, (last, e) -> {
               GameLinkedList<SettlementStorageRecord> value = e.getValue();
               int amount = last;

               for (SettlementStorageRecord record : value) {
                  InventoryRange inventoryRange = record.storage.getInventoryRange();
                  if (inventoryRange != null) {
                     InventoryItem item = inventoryRange.inventory.getItem(record.inventorySlot);
                     if (item != null && (itemFilter == null || itemFilter.test(item))) {
                        amount += item.getAmount();
                     }
                  }
               }

               return amount;
            }, Integer::sum)
      );
   }

   public int getTotalItems() {
      return this.totalItems;
   }
}
