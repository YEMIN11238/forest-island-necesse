package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;

public interface FoodConsumedJournalChallengeListener {
   void onFoodConsumed(ServerClient var1, FoodConsumableItem var2);
}
