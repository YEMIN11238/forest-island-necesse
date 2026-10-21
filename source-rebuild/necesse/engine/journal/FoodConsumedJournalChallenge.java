package necesse.engine.journal;

import java.util.ArrayList;
import necesse.engine.journal.listeners.FoodConsumedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;

public class FoodConsumedJournalChallenge extends SimpleJournalChallenge implements FoodConsumedJournalChallengeListener {
   protected String[] itemStringIDs;

   public FoodConsumedJournalChallenge(String... itemStringID) {
      this.itemStringIDs = itemStringID;
   }

   public FoodConsumedJournalChallenge(ArrayList<String> itemStringID) {
      this(itemStringID.toArray(new String[0]));
   }

   @Override
   public void onFoodConsumed(ServerClient serverClient, FoodConsumableItem item) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         for (String itemStringID : this.itemStringIDs) {
            if (item.getStringID().equals(itemStringID)) {
               this.markCompleted(serverClient);
               serverClient.forceCombineNewStats();
               return;
            }
         }
      }
   }
}
