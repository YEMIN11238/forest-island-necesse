package necesse.engine.journal;

import java.util.ArrayList;
import necesse.engine.journal.listeners.CraftedRecipeJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.recipe.Recipe;

public class CraftItemJournalChallenge extends SimpleJournalChallenge implements CraftedRecipeJournalChallengeListener {
   protected String[] itemStringIDs;

   public CraftItemJournalChallenge(String... items) {
      this.itemStringIDs = items;
   }

   public CraftItemJournalChallenge(ArrayList<String> items) {
      this(items.toArray(new String[0]));
   }

   @Override
   public void onCraftedRecipe(ServerClient serverClient, Recipe recipe, int amount) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         for (String itemStringID : this.itemStringIDs) {
            if (recipe.resultStringID.equals(itemStringID)) {
               this.markCompleted(serverClient);
               serverClient.forceCombineNewStats();
               return;
            }
         }
      }
   }
}
