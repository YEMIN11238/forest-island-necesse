package necesse.level.maps.levelData.jobs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.save.LoadData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.LinkedListJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.CraftSettlementRecipeActiveJob;
import necesse.entity.mobs.job.activeJob.DropOffSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.entity.mobs.job.activeJob.PickupSettlementStorageActiveJob;
import necesse.entity.mobs.job.activeJob.SimplePerformActiveJob;
import necesse.inventory.InventoryItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.SettlementInventory;
import necesse.level.maps.levelData.settlementData.SettlementStoragePickupSlot;
import necesse.level.maps.levelData.settlementData.SettlementWorkstation;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationLevelObject;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationObject;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationRecipe;
import necesse.level.maps.levelData.settlementData.StorageDropOff;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageGlobalIngredientIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageItemIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageRecords;

public class UseWorkstationLevelJob extends TileLevelJob {
   public SettlementWorkstation workstation;
   private Supplier<Boolean> validCheck;

   public UseWorkstationLevelJob(SettlementWorkstation workstation, Supplier<Boolean> validCheck) {
      super(workstation.tileX, workstation.tileY);
      this.workstation = workstation;
      this.validCheck = validCheck;
   }

   public UseWorkstationLevelJob(LoadData save) {
      super(save);
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   @Override
   public boolean isValid() {
      if (!super.isValid()) {
         return false;
      }

      GameObject object = this.getLevel().getObject(this.tileX, this.tileY);
      return object instanceof SettlementWorkstationObject && this.validCheck.get();
   }

   @Override
   public boolean estimateCanMoveTo(EntityJobWorker worker) {
      return this.workstation.estimateCanMoveTo(worker);
   }

   public static <T extends UseWorkstationLevelJob> JobSequence getJobSequence(EntityJobWorker worker, final FoundJob<T> foundJob) {
      if (foundJob.job.workstation.isAllAdjacentSolid()) {
         return null;
      }

      SettlementWorkstationLevelObject workstationObject = foundJob.job.workstation.getWorkstationObject();
      if (workstationObject != null) {
         Iterator var3 = foundJob.job.workstation.recipes.iterator();

         while (true) {
            SettlementWorkstationRecipe selectedRecipe;
            int totalCrafts;
            boolean isProcessingWorkstation;
            SettlementInventory dropOffInventory;
            int maxCrafts;
            while (true) {
               if (!var3.hasNext()) {
                  return null;
               }

               selectedRecipe = (SettlementWorkstationRecipe)var3.next();
               final SettlementWorkstationRecipe recipe = selectedRecipe;
               if (workstationObject.canCurrentlyCraft(recipe.recipe)) {
                  int countToCraft = 0;
                  switch (recipe.mode) {
                     case DO_COUNT:
                        countToCraft = recipe.modeCount;
                        break;
                     case DO_UNTIL:
                        if (recipe.modeCount != 0) {
                           maxCrafts = HasStorageLevelJob.getItemCount(
                              worker,
                              item -> item.equals(workstationObject.level, recipe.recipe.resultItem, true, true, "equals"),
                              recipe.modeCount,
                              true,
                              true
                           );
                           countToCraft = recipe.modeCount - maxCrafts;
                        }
                        break;
                     case DO_FOREVER:
                        countToCraft = Integer.MAX_VALUE;
                  }

                  if (countToCraft > 0) {
                     maxCrafts = Math.min(workstationObject.getMaxCraftsAtOnce(recipe.recipe), countToCraft);
                     totalCrafts = 0;
                     isProcessingWorkstation = foundJob.job.workstation.isProcessingWorkstation();
                     dropOffInventory = null;
                     if (!isProcessingWorkstation) {
                        break;
                     }

                     dropOffInventory = foundJob.job.workstation.getProcessingInputInventory();
                     if (dropOffInventory != null) {
                        break;
                     }
                  }
               }
            }

            final SettlementWorkstationRecipe recipe = selectedRecipe;
            ArrayList<SettlementStoragePickupSlot> pickups = new ArrayList<>();
            ArrayList<StorageDropOff> dropOffs = new ArrayList<>();
            ArrayList<InventoryItem> tempWorkInventory = new ArrayList<>(worker.getWorkInventory().getTotalItemStacks());

            for (InventoryItem item : worker.getWorkInventory().items()) {
               tempWorkInventory.add(item.copy());
            }

            for (int i = 0; i < maxCrafts; i++) {
               boolean valid = true;

               for (Ingredient ingredient : recipe.recipe.ingredients) {
                  ArrayList<InventoryItem> hasIngredientsItems = new ArrayList<>();
                  int hasIngredients = 0;
                  ListIterator<InventoryItem> li = tempWorkInventory.listIterator();

                  while (li.hasNext()) {
                     InventoryItem item = li.next();
                     if (recipe.canUseItem(ingredient, item)) {
                        int required = ingredient.getIngredientAmount() - hasIngredients;
                        InventoryItem taken = item.copy(Math.min(required, item.getAmount()));
                        hasIngredientsItems.add(taken);
                        hasIngredients += taken.getAmount();
                        item.setAmount(item.getAmount() - taken.getAmount());
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
                           .findPickupSlots(ingredient.getIngredientID(), worker, item -> recipe.canUseItem(ingredient, item), required, required);
                     } else {
                        slots = storageRecords.getIndex(SettlementStorageItemIDIndex.class)
                           .findPickupSlots(ingredient.getIngredientID(), worker, null, required, required);
                     }

                     if (slots == null) {
                        valid = false;
                        break;
                     }

                     if (isProcessingWorkstation) {
                        int totalAdded = 0;

                        for (InventoryItem hasItem : hasIngredientsItems) {
                           int added = dropOffInventory.canAddFutureDropOff(hasItem);
                           if (added <= 0) {
                              valid = false;
                              break;
                           }

                           totalAdded += added;
                           dropOffs.add(dropOffInventory.addFutureDropOff(() -> hasItem));
                        }

                        for (SettlementStoragePickupSlot pickup : slots) {
                           int added = dropOffInventory.canAddFutureDropOff(pickup.item);
                           if (added <= 0) {
                              valid = false;
                              break;
                           }

                           totalAdded += added;
                           dropOffs.add(dropOffInventory.addFutureDropOff(() -> pickup.item));
                           if (totalAdded >= ingredient.getIngredientAmount()) {
                              break;
                           }
                        }

                        if (!valid || totalAdded < ingredient.getIngredientAmount()) {
                           valid = false;
                           break;
                        }
                     }

                     pickups.addAll(slots);
                  } else if (isProcessingWorkstation) {
                     int totalAdded = 0;

                     for (InventoryItem hasItem : hasIngredientsItems) {
                        int added = dropOffInventory.canAddFutureDropOff(hasItem);
                        if (added <= 0) {
                           valid = false;
                           break;
                        }

                        totalAdded += added;
                        dropOffs.add(dropOffInventory.addFutureDropOff(() -> hasItem));
                     }

                     if (totalAdded < ingredient.getIngredientAmount()) {
                        valid = false;
                        break;
                     }
                  }
               }

               if (!valid) {
                  break;
               }

               totalCrafts++;
            }

            if (totalCrafts > 0) {
               GameMessage targetDescription = workstationObject.object.getLocalization();
               GameMessage itemDescription = recipe.name != null && !recipe.name.isEmpty()
                  ? new StaticMessage(recipe.name)
                  : recipe.recipe.resultItem.getItemLocalization();
               GameMessage activityDescription = new LocalMessage("activities", "crafting", "item", itemDescription, "target", targetDescription);
               LinkedListJobSequence sequence = new LinkedListJobSequence(activityDescription, false);

               for (SettlementStoragePickupSlot slot : pickups) {
                  sequence.add(slot.toPickupJob(worker, foundJob.priority));
               }

               if (isProcessingWorkstation) {
                  for (StorageDropOff dropOff : dropOffs) {
                     sequence.add(new DropOffSettlementStorageActiveJob(worker, foundJob.priority, foundJob.job.reservable, true, dropOff) {
                        @Override
                        public boolean isValid(boolean isCurrent) {
                           return recipe.removed ? false : super.isValid(isCurrent);
                        }
                     });
                  }

                  sequence.add(new SimplePerformActiveJob(worker, null) {
                     @Override
                     public ActiveJobResult perform() {
                        recipe.onCrafted(foundJob.job.workstation, 1);
                        return ActiveJobResult.FINISHED;
                     }
                  });
               } else {
                  for (int i = 0; i < totalCrafts; i++) {
                     sequence.add(foundJob.job.getCraftActiveJob(worker, foundJob.priority, recipe, sequence));
                  }
               }

               sequence.add(new PerformLevelJobEventActiveJob(worker, foundJob));
               return sequence;
            }

            pickups.forEach(SettlementStoragePickupSlot::remove);
            dropOffs.forEach(StorageDropOff::remove);
         }
      } else {
         return null;
      }
   }

   public ActiveJob getCraftActiveJob(
      EntityJobWorker worker, JobTypeHandler.TypePriority priority, SettlementWorkstationRecipe recipe, LinkedListJobSequence dropOffSequence
   ) {
      float secondsToComplete = (float)Math.pow(recipe.recipe.ingredients.length, 0.7F) + 1.0F;
      return new CraftSettlementRecipeActiveJob(
         worker, priority, this.tileX, this.tileY, this.workstation, recipe, secondsToComplete, this.reservable, dropOffSequence
      );
   }

   public static JobTypeHandler.SubHandler<UseWorkstationLevelJob> handler(EntityJobWorker worker, JobTypeHandler handler) {
      if (worker instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)worker;
         return handler.<UseWorkstationLevelJob>setJobHandler(UseWorkstationLevelJob.class, foundJob -> getJobSequence(humanMob, foundJob))
            .setPredicate(
               () -> !humanMob.isOnStrike() && !humanMob.hasCompletedMission() && humanMob.isSettlerWithinSettlement() && !humanMob.isInventoryFull(true)
            );
      } else {
         return null;
      }
   }
}
