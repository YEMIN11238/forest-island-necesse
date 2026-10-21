package necesse.level.maps.levelData.settlementData;

import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.util.GameLinkedList;
import necesse.engine.world.WorldEntity;
import necesse.entity.Entity;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;

public class SettlementStoragePickupSlot {
   private GameLinkedList<SettlementStoragePickupSlot>.Element element;
   public final LevelStorage storage;
   public final int slot;
   public final InventoryItem item;
   protected int prevAmountReserved;
   protected long reserveTick;

   protected SettlementStoragePickupSlot(LevelStorage storage, int slot, InventoryItem item, int prevAmountReserved) {
      this.storage = storage;
      this.slot = slot;
      this.prevAmountReserved = prevAmountReserved;
      this.item = item;
   }

   protected void init(GameLinkedList<SettlementStoragePickupSlot>.Element element, WorldEntity worldEntity) {
      if (this.element != null) {
         throw new IllegalStateException("Storage slot already initialized");
      }

      this.element = element;
      this.reserve(worldEntity);
   }

   public void reserve(WorldEntity worldEntity) {
      this.reserveTick = worldEntity.getGameTicks();
   }

   public void reserve(Entity entity) {
      this.reserve(entity.getWorldEntity());
   }

   public boolean isReserved(WorldEntity worldEntity) {
      return this.reserveTick >= worldEntity.getGameTicks() - 2L;
   }

   protected void removeUnsafe() {
      this.element.remove();
   }

   public void remove() {
      for (GameLinkedList<SettlementStoragePickupSlot>.Element next = this.element;
         next.hasPrev();
         ((SettlementStoragePickupSlot)next.object).prevAmountReserved = ((SettlementStoragePickupSlot)next.object).prevAmountReserved - this.item.getAmount()
      ) {
         next = next.prev();
      }

      this.element.remove();
   }

   public boolean isValid() {
      if (this.element.isRemoved()) {
         return false;
      }

      if (!this.storage.isStorageValid()) {
         this.remove();
         return false;
      }

      InventoryRange inventoryRange = this.storage.getInventoryRange();
      if (inventoryRange == null) {
         this.remove();
         return false;
      }

      if (inventoryRange.startSlot <= this.slot && this.slot <= inventoryRange.endSlot) {
         InventoryItem item = inventoryRange.inventory.getItem(this.slot);
         if (item != null && item.equals(this.storage.level, this.item, true, false, "pickups") && item.getAmount() >= this.item.getAmount()) {
            return true;
         }

         this.remove();
         return false;
      } else {
         this.remove();
         return false;
      }
   }

   public InventoryItem pickupItem() {
      if (this.element.isRemoved()) {
         return null;
      }

      if (!this.storage.isStorageValid()) {
         this.remove();
         return null;
      }

      InventoryRange inventoryRange = this.storage.getInventoryRange();
      if (inventoryRange == null) {
         this.remove();
         return null;
      }

      if (inventoryRange.startSlot <= this.slot && this.slot <= inventoryRange.endSlot) {
         InventoryItem item = inventoryRange.inventory.getItem(this.slot);
         if (item != null && item.equals(this.storage.level, this.item, true, false, "pickups") && item.getAmount() >= this.item.getAmount()) {
            InventoryItem pickedUp = item.copy(this.item.getAmount());
            inventoryRange.inventory.setAmount(this.slot, item.getAmount() - pickedUp.getAmount());
            this.remove();
            return pickedUp;
         } else {
            this.remove();
            return null;
         }
      } else {
         this.remove();
         return null;
      }
   }

   public boolean isRemoved() {
      return this.element.isRemoved();
   }

   public PickupSettlementStorageActiveJob toPickupJob(
      EntityJobWorker worker, JobTypeHandler.TypePriority priority, AtomicReference<InventoryItem> pickedUpItemRef
   ) {
      return new PickupSettlementStorageActiveJob(worker, priority, this, pickedUpItemRef);
   }

   public PickupSettlementStorageActiveJob toPickupJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority) {
      return this.toPickupJob(worker, priority, new AtomicReference<>());
   }
}
