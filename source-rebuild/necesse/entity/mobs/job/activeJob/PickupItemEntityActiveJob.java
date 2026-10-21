package necesse.entity.mobs.job.activeJob;

import java.util.LinkedList;
import java.util.List;
import necesse.engine.GameTileRange;
import necesse.engine.util.GameLinkedList;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.GameLinkedListJobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.pickup.ItemPickupReservedAmount;
import necesse.inventory.InventoryItem;

public class PickupItemEntityActiveJob extends EntityActiveJob<ItemPickupEntity> {
   protected LinkedList<ItemPickupReservedAmount> pickups = new LinkedList<>();
   protected GameLinkedList<ActiveJob>.Element followCombinedSequence;

   public PickupItemEntityActiveJob(EntityJobWorker worker, JobTypeHandler.TypePriority priority, ItemPickupReservedAmount pickup) {
      super(worker, priority, pickup.entity);
      this.addPickup(pickup);
      this.acceptAdjacentTiles = true;
   }

   public void addPickup(ItemPickupReservedAmount pickup) {
      this.pickups.add(pickup);
      pickup.onItemCombined(
         event -> {
            if (this.followCombinedSequence != null
               && event.next.canBePickedUpBySettlers()
               && event.next.item.equals(this.getLevel(), this.target.item, true, false, "equals")) {
               ItemPickupReservedAmount nextPickup = event.next.reservePickupAmount(event.combinedAmount);
               if (nextPickup != null) {
                  for (ActiveJob activeJob : this.followCombinedSequence.getList()) {
                     if (activeJob instanceof PickupItemEntityActiveJob) {
                        PickupItemEntityActiveJob pickupJob = (PickupItemEntityActiveJob)activeJob;
                        if (pickupJob.target == event.next) {
                           pickupJob.addPickup(nextPickup);
                           return;
                        }
                     }
                  }

                  PickupItemEntityActiveJob newJob = new PickupItemEntityActiveJob(this.worker, this.priority, nextPickup);
                  if (this.followCombinedSequence.isRemoved()) {
                     newJob.setFollowCombinedSequence(this.followCombinedSequence.getList().addLast(newJob));
                  } else {
                     newJob.setFollowCombinedSequence(this.followCombinedSequence.insertAfter(newJob));
                  }
               }
            }
         }
      );
   }

   public void setFollowCombinedSequence(GameLinkedList<ActiveJob>.Element followCombinedSequence) {
      this.followCombinedSequence = followCombinedSequence;
   }

   @Override
   public int getDirectMoveToDistance() {
      return super.getDirectMoveToDistance();
   }

   public MobMovement getDirectMoveTo(ItemPickupEntity target) {
      return new MobMovementLevelPos(target.x, target.y);
   }

   @Override
   public void tick(boolean isCurrent, boolean isMovingTo) {
      for (ItemPickupReservedAmount pickup : this.pickups) {
         pickup.reserve(this.worker.getMobWorker());
      }
   }

   @Override
   public boolean isAtTarget() {
      return this.getDistanceToTarget() <= this.getCompleteRange();
   }

   @Override
   public boolean shouldClearSequence() {
      return false;
   }

   @Override
   public boolean isJobValid(boolean isCurrent) {
      if (!isCurrent) {
         return true;
      }

      if (!this.target.canBePickedUpBySettlers()) {
         return false;
      }

      this.pickups.removeIf(next -> !next.isValid());
      return !this.pickups.isEmpty();
   }

   @Override
   public void onMadeCurrent() {
      super.onMadeCurrent();
   }

   @Override
   public ActiveJobResult performTarget() {
      if (this.worker.isInWorkAnimation()) {
         return ActiveJobResult.PERFORMING;
      }

      boolean first = true;

      for (ItemPickupReservedAmount pickup : this.pickups) {
         InventoryItem item = pickup.pickupItem();
         if (first) {
            this.worker.showPickupAnimation(this.target.getX(), this.target.getY(), item.item, 250, true);
         }

         this.worker.getWorkInventory().add(item);
         first = false;
      }

      return ActiveJobResult.FINISHED;
   }

   public static void addItemPickupJobs(
      EntityJobWorker worker, JobTypeHandler.TypePriority priority, List<ItemPickupEntity> items, GameLinkedListJobSequence sequence
   ) {
      addItemPickupJobs(worker, priority, null, items, sequence);
   }

   public static void addItemPickupJobs(
      EntityJobWorker worker, JobTypeHandler.TypePriority priority, GameTileRange maxRange, List<ItemPickupEntity> items, GameLinkedListJobSequence sequence
   ) {
      for (ItemPickupEntity itemPickupEntity : items) {
         if (itemPickupEntity.canBePickedUpBySettlers()) {
            ItemPickupReservedAmount pickup = itemPickupEntity.reservePickupAmount(
               Math.min(itemPickupEntity.item.getAmount(), itemPickupEntity.item.itemStackSize())
            );
            if (pickup != null) {
               itemPickupEntity.pickupCooldown = 5000;
               PickupItemEntityActiveJob job = new PickupItemEntityActiveJob(worker, priority, pickup);
               job.setFollowCombinedSequence(sequence.addLast(job));
            }
         }
      }
   }
}
