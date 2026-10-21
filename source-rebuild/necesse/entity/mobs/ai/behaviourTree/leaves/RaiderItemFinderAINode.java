package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import necesse.engine.GameTileRange;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PriorityMap;
import necesse.engine.util.TicketSystemList;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;

public abstract class RaiderItemFinderAINode<T extends Mob> extends MoveTaskAINode<T> {
   public static GameTileRange SEARCH_RANGE = new GameTileRange(40);
   public static String[] validCategories = new String[]{"materials", "rawfood", "commonfish", "food", "seeds", "saplings"};
   public long nextFindTime;
   protected RaiderItemFinderAINode.FoundItem foundItem;

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   public abstract float getMaxPickupValue(T var1);

   public abstract void onPickedUpItem(T var1, int var2, int var3, InventoryItem var4);

   public void onFoundItem(T mob, int tileX, int tileY, int slot, InventoryItem inventoryItem) {
   }

   public boolean isValidItem(T mob, int tileX, int tileY, int slot, InventoryItem inventoryItem) {
      return true;
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      if (this.foundItem != null) {
         if (!this.checkStillValid(this.foundItem)) {
            this.foundItem = null;
            this.nextFindTime = 0L;
         } else {
            float distance = mob.getDistance(this.foundItem.tileX * 32 + 16, this.foundItem.tileY * 32 + 16);
            if (distance <= 64.0F) {
               ObjectEntity objectEntity = this.mob().getLevel().entityManager.getObjectEntity(this.foundItem.tileX, this.foundItem.tileY);
               Inventory inventory = ((OEInventory)objectEntity).getInventory();
               InventoryItem invItem = inventory.getItem(this.foundItem.inventorySlot);
               if (invItem != null) {
                  InventoryItem pickedUpItem = invItem.copy(Math.min(invItem.getAmount(), this.foundItem.invItem.getAmount()));
                  this.onPickedUpItem(mob, this.foundItem.tileX, this.foundItem.tileY, pickedUpItem);
                  inventory.setAmount(this.foundItem.inventorySlot, invItem.getAmount() - pickedUpItem.getAmount());
                  this.foundItem = null;
               } else {
                  this.foundItem = null;
                  this.nextFindTime = 0L;
               }
            }
         }
      }

      if (this.nextFindTime <= mob.getTime()) {
         if (this.foundItem != null) {
            return this.moveToTileTask(
               this.foundItem.tileX,
               this.foundItem.tileY,
               TilePathfinding.isAtOrAdjacentObject(mob.getLevel(), this.foundItem.tileX, this.foundItem.tileY),
               path -> {
                  if (path.moveIfWithin(-1, -1, () -> this.nextFindTime = 0L)) {
                     int nextPathTimeAdd = path.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 2000, 0.1F);
                     this.nextFindTime = mob.getTime() + nextPathTimeAdd;
                  }

                  return AINodeResult.SUCCESS;
               }
            );
         }

         this.runFinder();
         if (this.foundItem != null) {
            this.nextFindTime = 0L;
         } else {
            this.nextFindTime = mob.getTime() + 5000L;
         }
      }

      return this.foundItem == null ? AINodeResult.FAILURE : AINodeResult.SUCCESS;
   }

   private boolean itemHasCategory(Item item, String... categoryStringIDs) {
      for (ItemCategory category = ItemCategory.getItemsCategory(item); category != null; category = category.parent) {
         for (String categoryStringID : categoryStringIDs) {
            if (category.stringID.equals(categoryStringID)) {
               return true;
            }
         }
      }

      return false;
   }

   public void runFinder() {
      T mob = this.mob();
      PriorityMap<RaiderItemFinderAINode.FoundItem> foundItems = new PriorityMap<>();
      float maxValue = this.getMaxPickupValue(mob);

      for (Point tile : SEARCH_RANGE.getValidTiles(mob.getTileX(), mob.getTileY())) {
         ObjectEntity objectEntity = mob.getLevel().entityManager.getObjectEntity(tile.x, tile.y);
         if (objectEntity instanceof OEInventory) {
            Inventory inventory = ((OEInventory)objectEntity).getInventory();

            for (int slot = 0; slot < inventory.getSize(); slot++) {
               InventoryItem invItem = inventory.getItem(slot);
               if (invItem != null && this.itemHasCategory(invItem.item, validCategories)) {
                  float singleBrokerValue = invItem.item.getBrokerValue(invItem);
                  if (!(singleBrokerValue > maxValue) && this.isValidItem(mob, tile.x, tile.y, slot, invItem)) {
                     int maxAmount = (int)(maxValue / singleBrokerValue);
                     float totalBrokerValue = singleBrokerValue * maxAmount;
                     foundItems.add(
                        (int)totalBrokerValue, new RaiderItemFinderAINode.FoundItem(tile.x, tile.y, slot, invItem.copy(maxAmount), singleBrokerValue)
                     );
                  }
               }
            }
         }
      }

      TicketSystemList<RaiderItemFinderAINode.FoundItem> lottery = new TicketSystemList<>();

      for (RaiderItemFinderAINode.FoundItem foundItem : foundItems.getBestObjects(40)) {
         lottery.addObject((int)(foundItem.singleBrokerValue * 100.0F), foundItem);
      }

      this.foundItem = lottery.getRandomObject(GameRandom.globalRandom);
      if (this.foundItem != null) {
         this.onFoundItem(mob, this.foundItem.tileX, this.foundItem.tileY, this.foundItem.inventorySlot, this.foundItem.invItem);
      }
   }

   protected boolean checkStillValid(RaiderItemFinderAINode.FoundItem foundItem) {
      ObjectEntity objectEntity = this.mob().getLevel().entityManager.getObjectEntity(foundItem.tileX, foundItem.tileY);
      if (objectEntity instanceof OEInventory) {
         Inventory inventory = ((OEInventory)objectEntity).getInventory();
         if (foundItem.inventorySlot >= inventory.getSize()) {
            return false;
         }

         InventoryItem invItem = inventory.getItem(foundItem.inventorySlot);
         return invItem == null ? false : invItem.equals(this.mob().getLevel(), foundItem.invItem, true, true, "equals");
      } else {
         return false;
      }
   }

   protected static class FoundItem {
      public int tileX;
      public int tileY;
      public int inventorySlot;
      public InventoryItem invItem;
      public float singleBrokerValue;

      public FoundItem(int tileX, int tileY, int inventorySlot, InventoryItem invItem, float singleBrokerValue) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.inventorySlot = inventorySlot;
         this.invItem = invItem;
         this.singleBrokerValue = singleBrokerValue;
      }
   }
}
