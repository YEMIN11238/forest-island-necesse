package necesse.entity.mobs.job.activeJob;

import java.util.ListIterator;
import java.util.function.Supplier;
import necesse.engine.util.GameObjectReservable;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.jobs.JobMoveToTile;
import necesse.level.maps.levelData.settlementData.LevelStorage;
import necesse.level.maps.levelData.settlementData.StorageDropOff;

public class DropOffSettlementStorageActiveJob extends TileActiveJob {
   public final StorageDropOff dropOff;
   public final GameObjectReservable reservable;
   public boolean requireFullAmount;
   public long lastHoldTickTime;
   public int holdTickCooldown = 2000;

   public DropOffSettlementStorageActiveJob(
      EntityJobWorker worker,
      JobTypeHandler.TypePriority priority,
      LevelStorage storage,
      GameObjectReservable reservable,
      boolean requireFullAmount,
      Supplier<InventoryItem> dropOffItem
   ) {
      this(worker, priority, reservable, requireFullAmount, storage.addFutureDropOff(dropOffItem));
   }

   public DropOffSettlementStorageActiveJob(
      EntityJobWorker worker, JobTypeHandler.TypePriority priority, GameObjectReservable reservable, boolean requireFullAmount, StorageDropOff dropOff
   ) {
      super(worker, priority, dropOff.storage.tileX, dropOff.storage.tileY);
      this.reservable = reservable;
      this.requireFullAmount = requireFullAmount;
      this.dropOff = dropOff;
   }

   @Override
   public JobMoveToTile getMoveToTile(JobMoveToTile lastTile) {
      return new JobMoveToTile(this.tileX, this.tileY, true);
   }

   @Override
   public void tick(boolean isCurrent, boolean isMovingTo) {
      if (this.reservable != null) {
         this.reservable.reserve(this.worker.getMobWorker());
      }

      this.dropOff.reserve(this.worker.getMobWorker());
      if (isCurrent) {
         long currentTime = this.getLevel().getTime();
         long timeSinceIdleTick = currentTime - this.lastHoldTickTime;
         if (timeSinceIdleTick >= this.holdTickCooldown) {
            InventoryItem item = this.dropOff.getItem();
            if (item != null) {
               this.worker.showHoldAnimation(item.item, this.holdTickCooldown + 500);
            }

            this.lastHoldTickTime = currentTime;
         }
      }
   }

   @Override
   public boolean isValid(boolean isCurrent) {
      if (this.reservable != null && !this.reservable.isAvailable(this.worker.getMobWorker())) {
         return false;
      } else if (!this.dropOff.storage.isStorageValid()) {
         return false;
      } else {
         return this.requireFullAmount ? this.dropOff.canAddFullAmount() : this.dropOff.canAddAmount() > 0;
      }
   }

   @Override
   public void onCancelled(boolean becauseOfInvalid, boolean isCurrent, boolean isMovingTo) {
      super.onCancelled(becauseOfInvalid, isCurrent, isMovingTo);
      this.dropOff.remove();
   }

   @Override
   public ActiveJobResult perform() {
      if (this.worker.isInWorkAnimation()) {
         return ActiveJobResult.PERFORMING;
      }

      this.worker.clearHoldAnimation();
      InventoryItem dropOffItem = this.dropOff.getItem();
      ListIterator<InventoryItem> li = this.worker.getWorkInventory().listIterator();

      while (li.hasNext()) {
         InventoryItem item = li.next();
         if (item.equals(this.getLevel(), dropOffItem, true, false, "equals")) {
            int added = this.dropOff.addItem(dropOffItem);
            if (added > 0) {
               item.setAmount(item.getAmount() - added);
               if (item.getAmount() <= 0) {
                  li.remove();
               }

               this.worker.getWorkInventory().markDirty();
               this.worker.showPlaceAnimation(this.tileX * 32 + 16, this.tileY * 32 + 16, item.item, 250, true);
               return ActiveJobResult.FINISHED;
            }
         }
      }

      return ActiveJobResult.FAILED;
   }
}
