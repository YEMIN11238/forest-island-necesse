package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.LoadData;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.PointHashMap;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.activeJob.DropOffSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.level.maps.levelData.settlementData.LevelStorage;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class HaulFromLevelJob extends TileLevelJob {
   public LevelStorage storage;
   public boolean onlyAcceptSpecificAmount = false;
   public InventoryItem item;
   public LinkedList<HaulFromLevelJob.HaulPosition> dropOffPositions = new LinkedList<>();

   public HaulFromLevelJob(LevelStorage storage, InventoryItem item) {
      super(storage.tileX, storage.tileY);
      this.storage = storage;
      this.item = item;
   }

   public HaulFromLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public boolean isSameJob(TileLevelJob other) {
      if (this.isSameTile(other) && other.getID() == this.getID()) {
         HaulFromLevelJob otherHaul = (HaulFromLevelJob)other;
         return this.item.equals(this.getLevel(), otherHaul.item, true, false, "equals");
      } else {
         return false;
      }
   }

   @Override
   public boolean isValid() {
      if (!super.isValid()) {
         return false;
      }

      if (this.item != null && this.item.getAmount() > 0 && !this.dropOffPositions.isEmpty()) {
         InventoryRange range = this.getInventoryRange();
         if (range != null) {
            int amount = 0;

            for (int i = range.startSlot; i <= range.endSlot; i++) {
               if (!range.inventory.isSlotClear(i)) {
                  InventoryItem invItem = range.inventory.getItem(i);
                  if (this.item.equals(this.getLevel(), invItem, true, false, "dropoff")) {
                     amount += invItem.getAmount();
                     if (this.onlyAcceptSpecificAmount) {
                        if (amount >= this.item.getAmount()) {
                           return true;
                        }
                     } else if (amount > 0) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public InventoryRange getInventoryRange() {
      return this.storage.getInventoryRange();
   }

   @Override
   public boolean estimateCanMoveTo(EntityJobWorker worker) {
      return this.storage.estimateCanMoveTo(worker);
   }

   public static <T extends HaulFromLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      return foundJob.job.getJobSequence(worker, foundJob.priority);
   }

   public JobSequence getJobSequence(final EntityJobWorker worker, JobTypeHandler.TypePriority priority) {
      ZoneTester restrictZone = worker.getJobRestrictZone();
      InventoryRange range = this.getInventoryRange();
      if (range != null) {
         LinkedList<HaulFromLevelJob.HaulPosition> validHaulPositions = new LinkedList<>();
         ListIterator<HaulFromLevelJob.HaulPosition> dropOffLI = this.dropOffPositions.listIterator();
         PointHashMap<Boolean> estimatedCanMoveToCache = new PointHashMap<>();

         while (dropOffLI.hasNext()) {
            final HaulFromLevelJob.HaulPosition pos = dropOffLI.next();
            if (restrictZone.containsTile(pos.storage.tileX, pos.storage.tileY)) {
               InventoryRange dropOffRange = pos.getInventoryRange();
               if (dropOffRange != null) {
                  int addAmount = pos.storage.getFilter().getAddAmount(this.getLevel(), this.item, dropOffRange, false);
                  if (addAmount > 0
                     && dropOffRange.inventory.canAddItem(this.getLevel(), null, this.item, dropOffRange.startSlot, dropOffRange.endSlot, "hauljob") > 0) {
                     boolean canMoveTo = estimatedCanMoveToCache.compute(pos.storage.tileX, pos.storage.tileY, new PointHashMap.RemappingFunction<Boolean>() {
                        public Boolean remap(int x, int y, Boolean lastValue) {
                           return lastValue == null ? pos.storage.estimateCanMoveTo(worker) : lastValue;
                        }
                     });
                     if (canMoveTo) {
                        validHaulPositions.add(pos);
                     }
                  } else {
                     dropOffLI.remove();
                  }
               }
            }
         }

         if (!validHaulPositions.isEmpty()) {
            Comparator<ComputedObjectValue<HaulFromLevelJob.HaulPosition, Double>> comparator = Comparator.comparingInt(p -> -p.object.priority);
            comparator = comparator.thenComparingDouble(ComputedValue::get);
            HaulFromLevelJob.HaulPosition pos = validHaulPositions.stream()
               .map(
                  p -> new ComputedObjectValue<>(
                     p, () -> new Point(this.tileX * 32 + 16, this.tileY * 32 + 16).distance(p.storage.tileX * 32 + 16, p.storage.tileY * 32 + 16)
                  )
               )
               .min(comparator)
               .map(p -> (HaulFromLevelJob.HaulPosition)p.object)
               .orElse(null);
            if (pos != null) {
               int amount = Math.min(pos.storage.canAddFutureDropOff(this.item), Math.min(pos.amount, this.item.itemStackSize()));
               if (amount > 0) {
                  LinkedList<SettlementStoragePickupSlot> slots = this.storage.findUnreservedSlots(this.item.copy(), amount, amount);
                  if (slots != null && !slots.isEmpty()) {
                     GameMessage targetDescription = this.storage.getInventoryName();
                     GameMessage itemDescription = slots.getFirst().item.getItemLocalization();
                     GameMessage activityDescription = new LocalMessage("activities", "hauling", "item", itemDescription, "target", targetDescription);
                     LinkedListJobSequence sequence = new LinkedListJobSequence(activityDescription, false);

                     for (SettlementStoragePickupSlot slot : slots) {
                        AtomicReference<InventoryItem> pickedUpItemRef = new AtomicReference<>();
                        sequence.addFirst(new PickupSettlementStorageActiveJob(worker, priority, slot, pickedUpItemRef));
                        sequence.addLast(
                           new DropOffSettlementStorageActiveJob(
                              worker, priority, pos.storage, null, false, () -> slot.isRemoved() ? pickedUpItemRef.get() : slot.item
                           )
                        );
                        this.item.setAmount(this.item.getAmount() - amount);
                     }

                     sequence.add(new PerformLevelJobEventActiveJob(worker, priority, this));
                     return sequence;
                  }
               }

               return null;
            }
         }
      }

      return null;
   }

   public static JobTypeHandler.SubHandler<HaulFromLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<HaulFromLevelJob>setJobHandler(HaulFromLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike() && !humanMob.hasCompletedMission() && humanMob.isSettlerWithinSettlement() && !humanMob.isInventoryFull(true)
            );
      } else {
         return null;
      }
   }

   public static class HaulPosition {
      public final LevelStorage storage;
      public final int priority;
      public final int amount;

      public HaulPosition(LevelStorage storage, int priority, int amount) {
         this.storage = storage;
         this.priority = priority;
         this.amount = amount;
      }

      public InventoryRange getInventoryRange() {
         return this.storage.getInventoryRange();
      }
   }
}
