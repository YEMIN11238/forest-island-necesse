package necesse.level.maps.levelData.jobs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Optional;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.WorkInventory;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.DropOffSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupItemEntityActiveJob;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.pickup.ItemPickupReservedAmount;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class StorePickupItemLevelJob extends EntityLevelJob<ItemPickupEntity> {
   public StorePickupItemLevelJob(ItemPickupEntity target) {
      super(target);
   }

   public static <T extends StorePickupItemLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      return foundJob.job.getJobSequence(worker, foundJob.priority);
   }

   public static JobTypeHandler.JobStreamSupplier<? extends StorePickupItemLevelJob> getJobStreamer() {
      return (worker, handler) -> {
         Mob mob = worker.getMobWorker();
         ZoneTester restrictZone = worker.getJobRestrictZone();
         return mob.getLevel()
            .entityManager
            .pickups
            .streamInRegionsShape(worker.getJobSearchBounds(), 0)
            .filter(p -> !p.removed() && mob.isSamePlace(p) && p.getTimeSinceSpawned() > 10000L && p instanceof ItemPickupEntity)
            .filter(p -> restrictZone.containsTile(p.getTileX(), p.getTileY()))
            .map(p -> (ItemPickupEntity)p)
            .filter(ItemPickupEntity::canBePickedUpBySettlers)
            .map(StorePickupItemLevelJob::new);
      };
   }

   public LinkedListJobSequence getJobSequence(EntityJobWorker worker, JobTypeHandler.TypePriority priority) {
      GameMessage targetDescription = new LocalMessage("activities", "theground");
      GameMessage itemDescription = this.target.item.getItemLocalization();
      GameMessage activityDescription = new LocalMessage("activities", "hauling", "item", itemDescription, "target", targetDescription);
      LinkedListJobSequence jobs = new LinkedListJobSequence(activityDescription, false);
      return this.addToJobSequence(worker, priority, jobs) ? jobs : null;
   }

   public boolean addToJobSequence(EntityJobWorker worker, JobTypeHandler.TypePriority priority, final LinkedListJobSequence sequence) {
      int availableAmount = this.target.getAvailableAmount();
      if (availableAmount <= 0) {
         return false;
      }

      WorkInventory workInventory = worker.getWorkInventory();
      availableAmount = Math.min(workInventory.getCanAddAmount(this.target.item), availableAmount);
      if (availableAmount <= 0) {
         return false;
      }

      ArrayList<HasStorageLevelJob.DropOffFind> dropOffLocations = HasStorageLevelJob.findDropOffLocation(
         worker, this.target.item.copy(Math.min(availableAmount, this.target.item.itemStackSize())), this.target.getPositionPoint()
      );
      int amount = 0;
      LinkedList<DropOffSettlementStorageActiveJob> dropOffJobs = new LinkedList<>();

      for (HasStorageLevelJob.DropOffFind dropOffLocation : dropOffLocations) {
         amount += dropOffLocation.item.getAmount();
         dropOffJobs.add(dropOffLocation.getActiveJob(worker, priority, null, false));
      }

      if (amount > 0) {
         ItemPickupReservedAmount pickup = this.target.reservePickupAmount(amount);
         if (pickup != null) {
            for (DropOffSettlementStorageActiveJob dropOffJob : dropOffJobs) {
               sequence.addLast(dropOffJob);
            }

            sequence.addFirst(
               new PickupItemEntityActiveJob(worker, priority, pickup) {
                  @Override
                  public ActiveJobResult performTarget() {
                     ActiveJobResult result = super.performTarget();
                     if (result == ActiveJobResult.FINISHED && !this.worker.getWorkInventory().isFull()) {
                        JobTypeHandler.SubHandler<StorePickupItemLevelJob> jobHandler = this.worker
                           .getJobTypeHandler()
                           .getJobHandler(StorePickupItemLevelJob.class);
                        Comparator<FoundJob<StorePickupItemLevelJob>> comparator = Comparator.comparingInt(o -> -o.job.getSameJobPriority());
                        comparator = comparator.thenComparingDouble(FoundJob::getDistanceFromWorker);
                        Optional var4 = jobHandler.streamFoundJobs(this.worker, null)
                           .sorted(comparator)
                           .filter(FoundJob::canMoveTo)
                           .filter(e -> e.job.addToJobSequence(this.worker, e.priority, sequence))
                           .findFirst();
                     }

                     return result;
                  }
               }
            );
            sequence.add(new PerformLevelJobEventActiveJob(worker, priority, this));
            return true;
         }
      }

      return false;
   }

   public static JobTypeHandler.SubHandler<StorePickupItemLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<StorePickupItemLevelJob>setJobHandler(StorePickupItemLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike() && !humanMob.hasCompletedMission() && humanMob.isSettlerWithinSettlement() && !humanMob.isInventoryFull(true)
            )
            .setExtraJobStreamer(false, getJobStreamer());
      } else {
         return null;
      }
   }
}
