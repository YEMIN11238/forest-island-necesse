package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.HashMapGameLinkedList;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.itemFilter.ItemFilter;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.HaulFromLevelJob;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.SubRegion;

public abstract class LevelStorage {
   public final Level level;
   public final int tileX;
   public final int tileY;
   protected LinkedList<HaulFromLevelJob> haulFromLevelJobs = new LinkedList<>();
   protected StorageDropOffSimulation dropOffSimulation = new StorageDropOffSimulation(this);
   protected HashMapGameLinkedList<Integer, SettlementStoragePickupSlot> pickupSlots = new HashMapGameLinkedList<>();
   protected boolean isAllAdjacentSolid = false;

   public LevelStorage(Level level, int tileX, int tileY) {
      this.level = level;
      this.tileX = tileX;
      this.tileY = tileY;
   }

   public void updateAdjacentSolidState() {
      this.isAllAdjacentSolid = true;

      for (Point tile : this.level
         .getObject(this.tileX, this.tileY)
         .getMultiTile(this.level, 0, this.tileX, this.tileY)
         .getAdjacentTiles(this.tileX, this.tileY, true)) {
         Region region = this.level.regionManager.getRegionByTile(tile.x, tile.y, false);
         if (region != null) {
            SubRegion subRegion = region.subRegionData.getSubRegionByRegion(tile.x - region.tileXOffset, tile.y - region.tileYOffset);
            if (subRegion != null && !subRegion.getType().isAlwaysSolid) {
               this.isAllAdjacentSolid = false;
               break;
            }
         }
      }
   }

   public boolean isAllAdjacentSolid() {
      return this.isAllAdjacentSolid;
   }

   public boolean estimateCanMoveTo(EntityJobWorker worker) {
      Mob mob = worker.getMobWorker();
      return this.estimateCanMoveTo(mob.getTileX(), mob.getTileY(), mob.getPathDoorOption());
   }

   public boolean estimateCanMoveTo(int fromTileX, int fromTileY, PathDoorOption doorOption) {
      if (doorOption == null) {
         return true;
      } else {
         return this.isAllAdjacentSolid ? false : doorOption.canMoveToTile(fromTileX, fromTileY, this.tileX, this.tileY, true);
      }
   }

   public abstract InventoryRange getInventoryRange();

   public abstract ItemCategoriesFilter getFilter();

   public abstract GameMessage getInventoryName();

   public abstract boolean isStorageValid();

   public boolean isTileValid() {
      return this.getInventoryRange() != null;
   }

   public void removeInvalidPickups() {
      InventoryRange range = this.getInventoryRange();
      if (range != null) {
         for (int slot : this.pickupSlots.keySet()) {
            boolean valid = slot >= range.startSlot && slot <= range.endSlot;
            InventoryItem item = range.inventory.getItem(slot);
            GameLinkedList<SettlementStoragePickupSlot> slots = this.pickupSlots.get(slot);
            slots.removeIf(
               e -> !valid || item == null || !item.equals(this.level, e.item, true, false, "pickups") || !e.isReserved(this.level.getWorldEntity())
            );
         }
      } else {
         for (int slot : this.pickupSlots.keySet()) {
            this.pickupSlots.get(slot).elements().forEach(GameLinkedList.Element::remove);
         }
      }
   }

   public SettlementStoragePickupSlot reserve(int slot, InventoryItem currentItem, int amount) {
      GameLinkedList<SettlementStoragePickupSlot> storageSlots = this.pickupSlots.get(slot);
      int reservedAmount = storageSlots.stream().filter(e -> e.isReserved(this.level.getWorldEntity())).mapToInt(e -> e.item.getAmount()).sum();
      if (reservedAmount >= currentItem.getAmount()) {
         return null;
      }

      int amountLeft = currentItem.getAmount() - reservedAmount;
      amount = Math.min(amount, amountLeft);
      SettlementStoragePickupSlot pickup = new SettlementStoragePickupSlot(this, slot, currentItem.copy(amount), reservedAmount);
      pickup.init(storageSlots.addFirst(pickup), this.level.getWorldEntity());
      return pickup;
   }

   public SettlementStoragePickupFuture getFutureReserve(
      final int slot, final InventoryItem currentItem, final int maxAmount, final Consumer<SettlementStoragePickupSlot> accepted
   ) {
      final GameLinkedList<SettlementStoragePickupSlot> storageSlots = this.pickupSlots.get(slot);
      final int reservedAmount = storageSlots.stream().filter(e -> e.isReserved(this.level.getWorldEntity())).mapToInt(e -> e.item.getAmount()).sum();
      if (reservedAmount >= currentItem.getAmount()) {
         return null;
      }

      int amountLeft = currentItem.getAmount() - reservedAmount;
      return new SettlementStoragePickupFuture(this, currentItem.copy(Math.min(amountLeft, maxAmount))) {
         @Override
         public SettlementStoragePickupSlot accept(int amount) {
            InventoryItem pickedUpItems = currentItem.copy(Math.min(amount, maxAmount));
            SettlementStoragePickupSlot pickupSlot = new SettlementStoragePickupSlot(this.storage, slot, pickedUpItems, reservedAmount);
            pickupSlot.init(storageSlots.addFirst(pickupSlot), LevelStorage.this.level.getWorldEntity());
            LevelStorage.this.haulFromLevelJobs
               .stream()
               .filter(e -> e.item.equals(LevelStorage.this.level, pickedUpItems, true, false, "pickups"))
               .findFirst()
               .ifPresent(job -> job.item.setAmount(job.item.getAmount() - pickedUpItems.getAmount()));
            accepted.accept(pickupSlot);
            return pickupSlot;
         }
      };
   }

   public LinkedList<SettlementStoragePickupSlot> findUnreservedSlots(Predicate<InventoryItem> itemPredicate, int minAmount, int maxAmount) {
      InventoryRange range = this.getInventoryRange();
      if (range == null) {
         return new LinkedList<>();
      }

      LinkedList<SettlementStoragePickupSlot> slots = new LinkedList<>();
      int pickedUpAmount = 0;

      for (int i = range.endSlot; i >= range.startSlot; i--) {
         InventoryItem invItem = range.inventory.getItem(i);
         if (invItem != null && itemPredicate.test(invItem)) {
            GameLinkedList<SettlementStoragePickupSlot> storageSlots = this.pickupSlots.get(i);
            int reservedAmount = storageSlots.stream().filter(e -> e.isReserved(this.level.getWorldEntity())).mapToInt(e -> e.item.getAmount()).sum();
            if (reservedAmount < invItem.getAmount()) {
               int amount = Math.min(maxAmount - pickedUpAmount, invItem.getAmount());
               if (amount > 0) {
                  pickedUpAmount += amount;
                  SettlementStoragePickupSlot slot = new SettlementStoragePickupSlot(this, i, invItem.copy(amount), reservedAmount);
                  slot.init(storageSlots.addFirst(slot), this.level.getWorldEntity());
                  slots.addFirst(slot);
                  if (pickedUpAmount >= maxAmount) {
                     break;
                  }
               }
            }
         }
      }

      if (pickedUpAmount < minAmount) {
         slots.forEach(SettlementStoragePickupSlot::remove);
         return null;
      } else {
         return slots.isEmpty() ? null : slots;
      }
   }

   public LinkedList<SettlementStoragePickupSlot> findUnreservedSlots(ItemFilter filter, int minAmount, int maxAmount) {
      return this.findUnreservedSlots(filter::matchesItem, minAmount, maxAmount);
   }

   public LinkedList<SettlementStoragePickupSlot> findUnreservedSlots(InventoryItem item, int minAmount, int maxAmount) {
      return this.findUnreservedSlots(invItem -> invItem.canCombine(this.level, null, item, "hauljob"), minAmount, maxAmount);
   }

   public Stream<SettlementStoragePickupFuture> findFutureUnreservedSlots() {
      InventoryRange range = this.getInventoryRange();
      if (range != null) {
         Builder<SettlementStoragePickupFuture> builder = Stream.builder();

         for (int i = range.endSlot; i >= range.startSlot; i--) {
            final int slot = i;
            final InventoryItem invItem = range.inventory.getItem(slot);
            if (invItem != null) {
               final GameLinkedList<SettlementStoragePickupSlot> storageSlots = this.pickupSlots.get(slot);
               final int reservedAmount = storageSlots.stream().filter(e -> e.isReserved(this.level.getWorldEntity())).mapToInt(e -> e.item.getAmount()).sum();
               if (reservedAmount < invItem.getAmount()) {
                  final int maxAmount = invItem.getAmount() - reservedAmount;
                  builder.add(
                     new SettlementStoragePickupFuture(this, invItem.copy(maxAmount)) {
                        @Override
                        public SettlementStoragePickupSlot accept(int amount) {
                           InventoryItem pickedUpItems = invItem.copy(Math.min(amount, maxAmount));
                           SettlementStoragePickupSlot pickupSlot = new SettlementStoragePickupSlot(this.storage, slot, pickedUpItems, reservedAmount);
                           pickupSlot.init(storageSlots.addFirst(pickupSlot), LevelStorage.this.level.getWorldEntity());
                           LevelStorage.this.haulFromLevelJobs
                              .stream()
                              .filter(e -> e.item.equals(LevelStorage.this.level, pickedUpItems, true, false, "pickups"))
                              .findFirst()
                              .ifPresent(job -> job.item.setAmount(job.item.getAmount() - pickedUpItems.getAmount()));
                           return pickupSlot;
                        }
                     }
                  );
               }
            }
         }

         return builder.build();
      } else {
         return Stream.empty();
      }
   }

   public int canAddFutureDropOff(InventoryItem item) {
      return this.dropOffSimulation.canAddFutureDropOff(item);
   }

   public StorageDropOff addFutureDropOff(Supplier<InventoryItem> itemSupplier) {
      return this.dropOffSimulation.addFutureDropOff(this, itemSupplier);
   }

   public int getItemCount(Predicate<InventoryItem> filter, int maxCount, boolean useSimulatedInventory) {
      return this.dropOffSimulation.getItemCount(filter, maxCount, useSimulatedInventory);
   }
}
