package necesse.entity.mobs.job.activeJob;

import java.util.ListIterator;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.WorkInventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.maps.levelData.settlementData.settler.personalities.CollectorSettlerPersonality;

public class ConsumeCollectorItemsActiveJob extends SimplePerformActiveJob {
   public Ingredient[] cost;
   public CollectorSettlerPersonality personality;

   public ConsumeCollectorItemsActiveJob(
      EntityJobWorker worker, JobTypeHandler.TypePriority priority, CollectorSettlerPersonality personality, Ingredient[] cost
   ) {
      super(worker, priority);
      this.personality = personality;
      this.cost = cost;
   }

   @Override
   public ActiveJobResult perform() {
      if (!this.personality.canGiveGift()) {
         return ActiveJobResult.FAILED;
      }

      this.personality.onGiftGiven();
      WorkInventory inventory = this.worker.getWorkInventory();

      for (Ingredient ingredient : this.cost) {
         int removedAmount = 0;
         ListIterator<InventoryItem> li = inventory.listIterator();

         while (li.hasNext()) {
            InventoryItem item = li.next();
            if (ingredient.matchesItem(item.item)) {
               int missingAmount = ingredient.getIngredientAmount() - removedAmount;
               int itemAmount = item.getAmount();
               int toRemove = Math.min(itemAmount, missingAmount);
               removedAmount += toRemove;
               item.setAmount(itemAmount - toRemove);
               if (item.getAmount() <= 0) {
                  li.remove();
               }

               if (removedAmount >= ingredient.getIngredientAmount()) {
                  break;
               }
            }
         }
      }

      return ActiveJobResult.FINISHED;
   }
}
