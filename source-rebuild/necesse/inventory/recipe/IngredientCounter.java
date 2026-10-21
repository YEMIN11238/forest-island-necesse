package necesse.inventory.recipe;

import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;

public interface IngredientCounter {
   IngredientStep handle(Inventory var1, int var2, InventoryItem var3);
}
