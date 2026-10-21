package necesse.level.maps.levelData.jobs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.activeJob.ConsumeCollectorItemsActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.inventory.InventoryItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.CollectorSettlerPersonality;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageGlobalIngredientIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageItemIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;

public class FindCollectorItemsLevelJob extends EntityLevelJob<Mob> {
   public CollectorSettlerPersonality personality;
   public Ingredient[] cost;

   public FindCollectorItemsLevelJob(Mob mob, CollectorSettlerPersonality personality, Ingredient[] cost) {
      super(mob);
      this.personality = personality;
      this.cost = cost;
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
   public int getAfterPrioritizedPriority(FoundJob<?> foundJob) {
      return 10000;
   }

   public static <T extends FindCollectorItemsLevelJob> JobSequence getJobSequence(EntityJobWorker worker, FoundJob<T> foundJob) {
      Mob mob = worker.getMobWorker();
      if (!(mob instanceof SettlerMob)) {
         return null;
      }

      SettlerMob settlerMob = (SettlerMob)mob;
      if (!settlerMob.isSettlerWithinSettlement()) {
         return null;
      }

      ServerSettlementData settlement = settlerMob.getSettlerSettlementServerData();
      if (settlement == null) {
         return null;
      }

      ArrayList<SettlementStoragePickupSlot> pickups = new ArrayList<>();
      ArrayList<InventoryItem> tempWorkInventory = new ArrayList<>(worker.getWorkInventory().getTotalItemStacks());

      for (InventoryItem item : worker.getWorkInventory().items()) {
         tempWorkInventory.add(item.copy());
      }

      GameMessage itemName = null;
      boolean valid = true;

      for (Ingredient ingredient : foundJob.job.cost) {
         int hasIngredients = 0;
         ListIterator<InventoryItem> li = tempWorkInventory.listIterator();

         while (li.hasNext()) {
            InventoryItem item = li.next();
            if (ingredient.matchesItem(item.item)) {
               int required = ingredient.getIngredientAmount() - hasIngredients;
               InventoryItem taken = item.copy(Math.min(required, item.getAmount()));
               hasIngredients += taken.getAmount();
               item.setAmount(item.getAmount() - taken.getAmount());
               if (itemName == null) {
                  itemName = item.getItemLocalization();
               }

               if (item.getAmount() <= 0) {
                  li.remove();
               }

               if (hasIngredients >= ingredient.getIngredientAmount()) {
                  break;
               }
            }
         }

         if (hasIngredients < ingredient.getIngredientAmount()) {
            SettlementStorageRecords storageRecords = PickupSettlementStorageActiveJob.getStorageRecords(worker);
            if (storageRecords == null) {
               valid = false;
               break;
            }

            int required = ingredient.getIngredientAmount() - hasIngredients;
            LinkedList<SettlementStoragePickupSlot> slots;
            if (ingredient.isGlobalIngredient()) {
               slots = storageRecords.getIndex(SettlementStorageGlobalIngredientIDIndex.class)
                  .findPickupSlots(ingredient.getIngredientID(), worker, item -> ingredient.matchesItem(item.item), required, required);
            } else {
               slots = storageRecords.getIndex(SettlementStorageItemIDIndex.class)
                  .findPickupSlots(ingredient.getIngredientID(), worker, null, required, required);
            }

            if (slots == null) {
               valid = false;
               break;
            }

            if (itemName == null && !slots.isEmpty()) {
               itemName = slots.getFirst().item.getItemLocalization();
            }

            pickups.addAll(slots);
         }
      }

      if (!valid) {
         pickups.forEach(SettlementStoragePickupSlot::remove);
         return null;
      }

      if (itemName == null) {
         itemName = new StaticMessage("N/A");
      }

      GameMessage activityDescription = new LocalMessage("activities", "usingcollector")
         .addReplacement("item", itemName)
         .addReplacement("personality", foundJob.job.personality.getName());
      LinkedListJobSequence sequence = new LinkedListJobSequence(activityDescription, false);

      for (SettlementStoragePickupSlot slot : pickups) {
         sequence.add(slot.toPickupJob(worker, foundJob.priority));
      }

      sequence.add(new ConsumeCollectorItemsActiveJob(worker, foundJob.priority, foundJob.job.personality, foundJob.job.cost));
      sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
      return sequence;
   }

   public static JobTypeHandler.SubHandler<FindCollectorItemsLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      return null;
   }
}
