package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.inventory.recipe.Recipe;

public interface CraftedRecipeJournalChallengeListener {
   void onCraftedRecipe(ServerClient var1, Recipe var2, int var3);
}
