package necesse.inventory.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import necesse.engine.GameLog;
import necesse.engine.registries.ItemRegistry;

public class RecipeBrokerValueCompute {
   private final RecipeList recipeList;
   private HashMap<Integer, RecipeBrokerValueCompute.Element> openHashMap = new HashMap<>();
   private LinkedList<RecipeBrokerValueCompute.Element> open = new LinkedList<>();
   private boolean calculated = false;

   public RecipeBrokerValueCompute(RecipeList recipeList) {
      this.recipeList = recipeList;
   }

   public void addItem(int itemID, float valueMultiplier) {
      if (this.openHashMap.containsKey(itemID)) {
         throw new IllegalArgumentException("Already added item with itemID " + itemID);
      }

      RecipeBrokerValueCompute.Element element = new RecipeBrokerValueCompute.Element(itemID, valueMultiplier);
      this.openHashMap.put(itemID, element);
      this.open.add(element);
   }

   public void calculate(RecipeBrokerValueCompute.CalculatedHandler handler) {
      if (this.calculated) {
         throw new IllegalStateException("Already calculated");
      }

      while (!this.open.isEmpty()) {
         RecipeBrokerValueCompute.Element element = this.open.removeFirst();
         ArrayList<Recipe> recipes = this.recipeList.getRecipesFromResult(element.itemID);
         RecipeBrokerValueCompute.Element invalidator = null;
         Recipe bestRecipe = null;
         boolean bestRecipeIsValid = false;
         float bestRecipeBrokerValue = 0.0F;

         for (Recipe recipe : recipes) {
            float totalBrokerValue = 0.0F;
            boolean isRecipeValid = true;

            for (Ingredient ingredient : recipe.ingredients) {
               if (!ingredient.isGlobalIngredient()) {
                  int ingredientItemID = ingredient.getIngredientID();
                  RecipeBrokerValueCompute.Element other = this.openHashMap.get(ingredientItemID);
                  if (other != null) {
                     element.addWaitingOn(this.openHashMap, ingredientItemID);
                     invalidator = other;
                     isRecipeValid = false;
                  }

                  totalBrokerValue += ItemRegistry.getBrokerValue(ingredientItemID) * ingredient.getIngredientAmount();
               } else {
                  int bestIngredientItemID = -1;
                  float bestIngredientItemValue = 0.0F;

                  for (int ingredientItemID : ingredient.getGlobalIngredient().getRegisteredItemIDs()) {
                     float brokerValue = ItemRegistry.getBrokerValue(ingredientItemID);
                     RecipeBrokerValueCompute.Element other = this.openHashMap.get(ingredientItemID);
                     if (other != null) {
                        element.addWaitingOn(this.openHashMap, ingredientItemID);
                        invalidator = other;
                     } else if (bestIngredientItemID == -1 || brokerValue < bestIngredientItemValue) {
                        bestIngredientItemID = ingredientItemID;
                        bestIngredientItemValue = brokerValue;
                     }
                  }

                  if (bestIngredientItemID >= 0) {
                     totalBrokerValue += bestIngredientItemValue * ingredient.getIngredientAmount();
                  } else {
                     isRecipeValid = false;
                  }
               }
            }

            if ((bestRecipe == null || !bestRecipeIsValid || isRecipeValid)
               && (bestRecipe == null || totalBrokerValue < bestRecipeBrokerValue || !bestRecipeIsValid && isRecipeValid)) {
               bestRecipe = recipe;
               if (isRecipeValid) {
                  bestRecipeIsValid = true;
               }

               bestRecipeBrokerValue = totalBrokerValue;
            }
         }

         if (invalidator == null) {
            if (bestRecipe == null && ItemRegistry.isObtainable(element.itemID)) {
               GameLog.warn
                  .println(
                     "Could not calculate " + ItemRegistry.getDisplayName(element.itemID) + " broker value based on recipe because there is no recipe for it"
                  );
            }

            element.bestRecipeBrokerValue = bestRecipeBrokerValue;
            this.openHashMap.remove(element.itemID);
            handler.handle(element.itemID, element.bestRecipeBrokerValue * element.valueMultiplier);
         } else if (bestRecipeIsValid) {
            element.bestRecipeBrokerValue = bestRecipeBrokerValue;
            this.openHashMap.remove(element.itemID);
            handler.handle(element.itemID, element.bestRecipeBrokerValue * element.valueMultiplier);
         } else if (invalidator.waitingOnItemsGroup.contains(element.itemID) && ItemRegistry.isObtainable(element.itemID)) {
            GameLog.warn
               .println(
                  "Could not calculate "
                     + ItemRegistry.getDisplayName(element.itemID)
                     + " broker value based on recipe because it goes in circle with: "
                     + Arrays.toString(invalidator.waitingOnItemsGroup.stream().map(ItemRegistry::getDisplayName).toArray())
               );
            element.bestRecipeBrokerValue = bestRecipeBrokerValue;
            handler.handle(element.itemID, element.bestRecipeBrokerValue * element.valueMultiplier);
         } else {
            this.open.addLast(element);
         }
      }

      this.calculated = true;
   }

   private static String displayName(int itemID) {
      return ItemRegistry.getDisplayName(itemID);
   }

   @FunctionalInterface
   public interface CalculatedHandler {
      void handle(int var1, float var2);
   }

   private static class Element {
      public final int itemID;
      public final float valueMultiplier;
      public float bestRecipeBrokerValue;
      public HashSet<Integer> waitingOnItemsGroup = new HashSet<>();

      public Element(int itemID, float valueMultiplier) {
         this.itemID = itemID;
         this.valueMultiplier = valueMultiplier;
      }

      public void addWaitingOn(HashMap<Integer, RecipeBrokerValueCompute.Element> openHashMap, int itemID) {
         RecipeBrokerValueCompute.Element other = openHashMap.get(itemID);
         if (other != null && this.waitingOnItemsGroup != other.waitingOnItemsGroup && this.waitingOnItemsGroup.contains(itemID)) {
            other.waitingOnItemsGroup.addAll(this.waitingOnItemsGroup);
            this.waitingOnItemsGroup = other.waitingOnItemsGroup;
         }

         this.waitingOnItemsGroup.add(itemID);
      }
   }
}
