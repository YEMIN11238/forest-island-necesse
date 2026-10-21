package necesse.level.maps.levelData.jobs;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameObjectReservable;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.DropOffSettlementStorageActiveJob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.level.maps.levelData.settlementData.SettlementInventory;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupFuture;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationLevelObject;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class HasStorageLevelJob extends TileLevelJob {
   public SettlementInventory settlementInventory;
   private Supplier<Boolean> validCheck;

   public HasStorageLevelJob(SettlementInventory settlementInventory, Supplier<Boolean> validCheck) {
      super(settlementInventory.tileX, settlementInventory.tileY);
      this.settlementInventory = settlementInventory;
      this.validCheck = validCheck;
      this.reservable = new GameObjectReservable() {
         @Override
         public boolean isAvailable(Object worker, WorldEntity worldEntity) {
            return true;
         }
      };
   }

   public HasStorageLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public boolean isValid() {
      return !super.isValid() ? false : this.getInventory() != null && this.validCheck.get();
   }

   public Inventory getInventory() {
      ObjectEntity objectEntity = this.getLevel().entityManager.getObjectEntity(this.tileX, this.tileY);
      return objectEntity instanceof OEInventory ? ((OEInventory)objectEntity).getInventory() : null;
   }

   @Override
   public int getSameJobPriority() {
      return this.settlementInventory.priority;
   }

   @Override
   public boolean prioritizeForSameJobAgain() {
      return true;
   }

   public static <T extends HasStorageLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      if (worker.getWorkInventory().isEmpty()) {
         return null;
      }

      for (InventoryItem item : worker.getWorkInventory().items()) {
         int addAmount = foundJob.job.settlementInventory.canAddFutureDropOff(item);
         if (addAmount > 0) {
            InventoryItem addItem = item.copy();
            GameMessage activityDescription = new LocalMessage("activities", "droppingoffinv");
            return new SingleJobSequence(
                  new DropOffSettlementStorageActiveJob(worker, foundJob.priority, foundJob.job.settlementInventory, null, false, () -> addItem),
                  activityDescription
               )
               .withPerformedLevelJob(foundJob.job);
         }
      }

      return null;
   }

   public static JobTypeHandler.SubHandler<HasStorageLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<HasStorageLevelJob>setJobHandler(HasStorageLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(() -> !humanMob.hasCompletedMission() && humanMob.isSettlerWithinSettlement());
      } else {
         return null;
      }
   }

   public static Stream<HasStorageLevelJob> streamStorageJobs(EntityJobWorker worker, JobTypeHandler.SubHandler<?> handler) {
      ZoneTester restrictZone = worker.getJobRestrictZone();
      int jobID = LevelJobRegistry.hasStorageID;
      return worker.streamValidJobsWithinRange(null)
         .filter(j -> j.getID() == jobID)
         .filter(e -> e.isWithinRestrictZone(restrictZone))
         .map(e -> (HasStorageLevelJob)e);
   }

   public static Stream<SettlementStoragePickupFuture> findPickupItems(
      EntityJobWorker worker, JobTypeHandler.SubHandler<?> handler, Predicate<InventoryItem> filter
   ) {
      return streamStorageJobs(worker, handler)
         .filter(e -> e.settlementInventory.estimateCanMoveTo(worker))
         .flatMap(j -> j.settlementInventory.findFutureUnreservedSlots().filter(p -> filter.test(p.item)));
   }

   public static int getItemCount(
      EntityJobWorker worker, Predicate<InventoryItem> filter, int maxCount, boolean includeDropOffSimulation, boolean includeFutureWorkstationOutput
   ) {
      ZoneTester restrictZone = worker.getJobRestrictZone();
      AtomicInteger count = new AtomicInteger();
      final int storageJobID = LevelJobRegistry.hasStorageID;
      count.addAndGet(
         worker.streamValidJobsWithinRange(null)
            .filter(j -> j.getID() == storageJobID)
            .filter(e -> e.isWithinRestrictZone(restrictZone))
            .map(e -> (HasStorageLevelJob)e)
            .sequential()
            .reduce(0, (last, storageJob) -> {
               int nextMaxCount = maxCount - last - count.get();
               if (nextMaxCount > 0) {
                  last = last + storageJob.settlementInventory.getItemCount(filter, nextMaxCount, includeDropOffSimulation);
               }

               return last;
            }, Integer::sum)
      );
      if (count.get() < maxCount && includeFutureWorkstationOutput) {
         final int workstationJobID = LevelJobRegistry.useWorkstationID;
         count.addAndGet(
            worker.streamValidJobsWithinRange(null)
               .filter(j -> j.getID() == workstationJobID)
               .filter(e -> e.isWithinRestrictZone(restrictZone))
               .map(e -> (UseWorkstationLevelJob)e)
               .sequential()
               .reduce(0, (last, workstationJob) -> {
                  int nextMaxCount = maxCount - last - count.get();
                  if (nextMaxCount > 0) {
                     SettlementWorkstationLevelObject workstationObject = workstationJob.workstation.getWorkstationObject();
                     if (workstationObject != null) {
                        int nextCount = 0;

                        for (InventoryItem item : workstationObject.getCurrentAndFutureProcessingOutputs()) {
                           if (item != null && filter.test(item)) {
                              nextCount += item.getAmount();
                              if (nextCount >= nextMaxCount) {
                                 return nextMaxCount;
                              }
                           }
                        }

                        last = last + nextCount;
                     }
                  }

                  return last;
               }, Integer::sum)
         );
      }

      return Math.min(count.get(), maxCount);
   }

   public static ArrayList<HasStorageLevelJob.DropOffFind> findDropOffLocation(EntityJobWorker worker, InventoryItem item) {
      return findDropOffLocation(worker, item, null);
   }

   public static ArrayList<HasStorageLevelJob.DropOffFind> findDropOffLocation(EntityJobWorker worker, InventoryItem item, Point startPosition) {
      Mob mob = worker.getMobWorker();
      int jobID = LevelJobRegistry.hasStorageID;
      ZoneTester restrictZone = worker.getJobRestrictZone();
      GameLinkedList<ComputedObjectValue<HasStorageLevelJob, Integer>> storageJobs = worker.streamValidJobsWithinRange(null)
         .filter(j -> j.getID() == jobID)
         .filter(e -> e.isWithinRestrictZone(restrictZone))
         .map(e -> (HasStorageLevelJob)e)
         .map(storageJob -> new ComputedObjectValue<>(storageJob, () -> {
            int addAmount = storageJob.settlementInventory.canAddFutureDropOff(item);
            return Math.max(addAmount, 0);
         }))
         .filter(e -> e.get() > 0)
         .filter(e -> e.object.settlementInventory.estimateCanMoveTo(worker))
         .collect(GameLinkedList::new, GameLinkedList::add, GameLinkedList::addAll);
      ArrayList<HasStorageLevelJob.DropOffFind> jobs = new ArrayList<>();
      HasStorageLevelJob lastPickupLocation = null;
      int amount = 0;

      while (amount < item.getAmount() && !storageJobs.isEmpty()) {
         Point from;
         if (lastPickupLocation != null) {
            from = new Point(lastPickupLocation.tileX * 32 + 16, lastPickupLocation.tileY * 32 + 16);
         } else if (startPosition != null) {
            from = startPosition;
         } else {
            from = new Point(mob.getX(), mob.getY());
         }

         Comparator<GameLinkedList<ComputedObjectValue<HasStorageLevelJob, Integer>>.Element> comparator = (o1, o2) -> -Integer.compare(
            ((HasStorageLevelJob)((ComputedObjectValue)o1.object).object).getSameJobPriority(),
            ((HasStorageLevelJob)((ComputedObjectValue)o2.object).object).getSameJobPriority()
         );
         comparator = comparator.thenComparingDouble(
            e -> from.distance(
               ((HasStorageLevelJob)((ComputedObjectValue)e.object).object).tileX * 32 + 16,
               ((HasStorageLevelJob)((ComputedObjectValue)e.object).object).tileY * 32 + 16
            )
         );
         GameLinkedList<ComputedObjectValue<HasStorageLevelJob, Integer>>.Element best = storageJobs.streamElements().min(comparator).orElse(null);
         if (best == null) {
            break;
         }

         int remaining = item.getAmount() - amount;
         int dropOffAmount = Math.min((Integer)((ComputedObjectValue)best.object).get(), remaining);
         InventoryItem dropOffItem = item.copy(dropOffAmount);
         jobs.add(new HasStorageLevelJob.DropOffFind(((HasStorageLevelJob)((ComputedObjectValue)best.object).object).settlementInventory, dropOffItem));
         amount += dropOffAmount;
         if (amount >= item.getAmount()) {
            break;
         }

         lastPickupLocation = (HasStorageLevelJob)((ComputedObjectValue)best.object).object;
         best.remove();
      }

      return jobs;
   }

   public static class DropOffFind {
      public final SettlementInventory inventory;
      public final InventoryItem item;

      public DropOffFind(SettlementInventory inventory, InventoryItem item) {
         this.inventory = inventory;
         this.item = item;
      }

      public DropOffSettlementStorageActiveJob getActiveJob(
         EntityJobWorker worker, JobTypeHandler.TypePriority priority, GameObjectReservable reservable, boolean requireFullAmount
      ) {
         return new DropOffSettlementStorageActiveJob(worker, priority, this.inventory, reservable, requireFullAmount, () -> this.item);
      }
   }
}
