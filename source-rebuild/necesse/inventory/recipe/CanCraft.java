package necesse.inventory.recipe;

import necesse.engine.Settings;

public class CanCraft {
   private Ingredient[] ingredients;
   private int canCraft;
   private int hasAnyItems;
   public final int[] haveIngredients;
   public final boolean countAllIngredients;

   public CanCraft(Ingredient[] ingredients, boolean countAllIngredients) {
      this.ingredients = ingredients;
      this.haveIngredients = new int[ingredients.length];
      this.countAllIngredients = countAllIngredients && Settings.showIngredientsAvailable;
   }

   public CanCraft(Recipe recipe, boolean countAllIngredients) {
      this(recipe.ingredients, countAllIngredients);
   }

   public void addIngredient(int ingredientIndex, int amount) {
      Ingredient ingredient = this.ingredients[ingredientIndex];
      if (ingredient.getIngredientAmount() == 0) {
         if (this.haveIngredients[ingredientIndex] == 0) {
            this.haveIngredients[ingredientIndex] = -1;
            this.canCraft++;
            this.hasAnyItems++;
         }
      } else if (amount > 0) {
         if (this.haveIngredients[ingredientIndex] == 0) {
            this.hasAnyItems++;
         }

         boolean haveEnoughBefore = this.haveIngredients[ingredientIndex] >= ingredient.getIngredientAmount();
         this.haveIngredients[ingredientIndex] = this.haveIngredients[ingredientIndex] + amount;
         boolean haveEnoughAfter = this.haveIngredients[ingredientIndex] >= ingredient.getIngredientAmount();
         if (!haveEnoughBefore && haveEnoughAfter) {
            this.canCraft++;
         }
      }
   }

   public boolean hasAnyIngredients(int ingredientIndex) {
      Ingredient ingredient = this.ingredients[ingredientIndex];
      return ingredient.getIngredientAmount() == 0 ? this.haveIngredients[ingredientIndex] == -1 : this.haveIngredients[ingredientIndex] > 0;
   }

   public boolean canCraft() {
      return this.canCraft >= this.haveIngredients.length;
   }

   public boolean hasAnyItems() {
      return this.hasAnyItems > 0;
   }

   public boolean hasAnyOfAllItems() {
      return this.hasAnyItems >= this.haveIngredients.length;
   }

   public static CanCraft allTrue(Ingredient... ingredients) {
      CanCraft out = new CanCraft(ingredients, false);

      for (int i = 0; i < ingredients.length; i++) {
         out.addIngredient(i, ingredients[i].getIngredientAmount());
      }

      return out;
   }

   public static CanCraft allTrue(Recipe r) {
      return allTrue(r.ingredients);
   }
}
