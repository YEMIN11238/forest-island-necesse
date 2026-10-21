package necesse.level.maps.levelData.settlementData;

import java.util.ArrayList;
import java.util.stream.Stream;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.Level;

public interface SettlementWorkstationObject {
   Stream<Recipe> streamSettlementRecipes(Level var1, int var2, int var3);

   default boolean canCurrentlyCraft(Level level, int tileX, int tileY, Recipe recipe) {
      return true;
   }

   default int getMaxCraftsAtOnce(Level level, int tileX, int tileY, Recipe recipe) {
      return 1;
   }

   default void tickCrafting(Level level, int tileX, int tileY, Recipe recipe) {
   }

   default void onCraftFinished(Level level, int tileX, int tileY, Recipe recipe) {
   }

   default SettlementRequestOptions getFuelRequestOptions(Level level, int tileX, int tileY) {
      return null;
   }

   default InventoryRange getFuelInventoryRange(Level level, int tileX, int tileY) {
      return null;
   }

   default boolean isProcessingInventory(Level level, int tileX, int tileY) {
      return false;
   }

   default InventoryRange getProcessingInputRange(Level level, int tileX, int tileY) {
      return null;
   }

   default InventoryRange getProcessingOutputRange(Level level, int tileX, int tileY) {
      return null;
   }

   default ArrayList<InventoryItem> getCurrentAndFutureProcessingOutputs(Level level, int tileX, int tileY) {
      return new ArrayList<>();
   }
}
