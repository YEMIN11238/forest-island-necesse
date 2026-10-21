package necesse.level.maps.levelData.jobs;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.HumanMobChatterHandler;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.SlowlyConsumeItemActiveJob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.FoodQuality;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageFoodQualityIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecordsRegionData;

public class ConsumeFoodLevelJob extends EntityLevelJob<Mob> {
   public JobTypeHandler.SubHandler<?> handler;
   public ItemCategoriesFilter dietFilter;

   public ConsumeFoodLevelJob(Mob mob, JobTypeHandler.SubHandler<?> handler, ItemCategoriesFilter dietFilter) {
      super(mob);
      this.handler = handler;
      this.dietFilter = dietFilter;
   }

   @Override
   public boolean isWithinRestrictZone(ZoneTester zone) {
      return true;
   }

   @Override
   public boolean isValid() {
      return true;
   }

   @Override
   public int getFirstPriority(FoundJob<?> foundJob) {
      return Integer.MAX_VALUE;
   }

   public static <T extends ConsumeFoodLevelJob> JobSequence getJobSequence(
      HumanMob humanMob, FoundJob<T> foundJob, HashSet<Integer> preferredItemIDs, Set<Integer> lastPriorityItemIDs
   ) {
      Predicate<InventoryItem> itemFilter = item -> {
         if (foundJob.job.dietFilter != null && !foundJob.job.dietFilter.isItemAllowed(item.item)) {
            return false;
         }

         if (preferredItemIDs != null && !preferredItemIDs.isEmpty()) {
            return preferredItemIDs.contains(item.item.getID());
         }

         if (lastPriorityItemIDs != null && lastPriorityItemIDs.contains(item.item.getID())) {
            return false;
         }

         if (!item.item.isFoodItem()) {
            return false;
         }

         FoodConsumableItem foodItem = (FoodConsumableItem)item.item;
         return foodItem.nutrition > 0 && foodItem.quality != null;
      };
      InventoryItem workerItem = humanMob.getWorkInventory()
         .stream()
         .filter(itemFilter)
         .max(Comparator.comparingInt(i -> ((FoodConsumableItem)i.item).quality.happinessIncrease))
         .orElse(null);
      FoodConsumableItem bestWorkerItem = workerItem == null ? null : (FoodConsumableItem)workerItem.item;
      SettlementStorageRecords storageRecords = PickupSettlementStorageActiveJob.getStorageRecords(humanMob);
      if (storageRecords != null) {
         for (FoodQuality quality : Settler.foodQualities.descendingSet()) {
            if (bestWorkerItem != null && bestWorkerItem.quality.happinessIncrease >= quality.happinessIncrease) {
               break;
            }

            SettlementStorageRecordsRegionData data = storageRecords.getIndex(SettlementStorageFoodQualityIndex.class).getFoodQuality(quality);
            if (data != null) {
               SettlementStoragePickupSlot slot = data.startFinder(humanMob).findFirstItemPickup(itemFilter);
               if (slot != null) {
                  GameMessage activityDescription = new LocalMessage("activities", "consuming", "item", slot.item.getItemLocalization());
                  LinkedListJobSequence sequence = new LinkedListJobSequence(activityDescription, false);
                  AtomicReference<InventoryItem> pickedUpItem = new AtomicReference<>();
                  sequence.add(slot.toPickupJob(humanMob, foundJob.priority, pickedUpItem));
                  sequence.add(
                     new SlowlyConsumeItemActiveJob(humanMob, foundJob.priority, pickedUpItem, humanMob, new HumanMobChatterHandler(humanMob, true), 10000)
                  );
                  sequence.add(new PerformLevelJobEventActiveJob(humanMob, foundJob));
                  humanMob.removeHungryNotification();
                  return sequence;
               }
            }
         }
      }

      if (workerItem != null) {
         GameMessage activityDescription = new LocalMessage("activities", "consuming", "item", workerItem.copy(1).getItemLocalization());
         humanMob.removeHungryNotification();
         return new SingleJobSequence(
               new SlowlyConsumeItemActiveJob(humanMob, foundJob.priority, workerItem.copy(1), humanMob, new HumanMobChatterHandler(humanMob, true), 10000),
               activityDescription
            )
            .withPerformedLevelJob(foundJob.job);
      }

      if (preferredItemIDs != null && !preferredItemIDs.isEmpty()) {
         return getJobSequence(humanMob, foundJob, null, lastPriorityItemIDs);
      }

      if (lastPriorityItemIDs != null && !lastPriorityItemIDs.isEmpty()) {
         return getJobSequence(humanMob, foundJob, null, null);
      }

      humanMob.submitHungryNotification();
      return null;
   }

   public static JobTypeHandler.JobStreamSupplier<? extends ConsumeFoodLevelJob> getJobStreamer(Supplier<ItemCategoriesFilter> dietFilterSupplier) {
      return (worker, handler) -> {
         ItemCategoriesFilter dietFilter = dietFilterSupplier.get();
         if (dietFilter != null && dietFilter.master.isAnyAllowed()) {
            Mob mobWorker = worker.getMobWorker();
            return Stream.of(new ConsumeFoodLevelJob(mobWorker, handler, dietFilter));
         } else {
            return Stream.empty();
         }
      };
   }

   public static JobTypeHandler.SubHandler<ConsumeFoodLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<ConsumeFoodLevelJob>setJobHandler(ConsumeFoodLevelJob.class, foundJob -> {
               HashSet<Integer> preferredFoodItemIDs = null;

               for (SettlerPersonality personality : humanMob.getPersonalities()) {
                  HashSet<Integer> itemIDs = personality.getPreferredFoodItemIDs();
                  if (itemIDs != null && !itemIDs.isEmpty()) {
                     if (preferredFoodItemIDs == null) {
                        preferredFoodItemIDs = new HashSet<>(itemIDs);
                     } else {
                        preferredFoodItemIDs.addAll(itemIDs);
                     }
                  }
               }

               return getJobSequence(humanMob, foundJob, preferredFoodItemIDs, new HashSet<>(humanMob.recentFoodItemIDsEaten));
            })
            .setPredicate(() -> humanMob.hungerLevel <= 0.25F)
            .setExtraJobStreamer(false, getJobStreamer(() -> humanMob.levelSettler == null ? null : humanMob.levelSettler.dietFilter));
      } else {
         return null;
      }
   }
}
