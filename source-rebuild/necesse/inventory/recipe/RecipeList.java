package necesse.inventory.recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.HashMapArrayList;
import necesse.inventory.item.Item;

public class RecipeList {
   private ArrayList<Recipe> recipes;
   private HashMap<Integer, Integer> hashToRecipeIndexMap;
   private HashMapArrayList<String, Recipe> techRecipes;
   private boolean[] itemIsCraftingMat;
   private HashSet<Tech>[] ingredientTechs;
   private ArrayList<Recipe>[] resultItemRecipes;
   private ArrayList<Recipe>[] ingredientItemRecipes;
   private int hash = 0;

   public RecipeList() {
      this.loadDefaultRecipes();
   }

   RecipeList(RecipeList copy) {
      this.recipes = new ArrayList<>(copy.recipes);
      this.hashToRecipeIndexMap = new HashMap<>(copy.hashToRecipeIndexMap);
      this.techRecipes = new HashMapArrayList<>();

      for (Entry<String, ArrayList<Recipe>> entry : copy.techRecipes.entrySet()) {
         this.techRecipes.addAll(entry.getKey(), entry.getValue());
      }

      this.itemIsCraftingMat = Arrays.copyOf(copy.itemIsCraftingMat, copy.itemIsCraftingMat.length);
      this.ingredientTechs = new HashSet[copy.ingredientTechs.length];

      for (int i = 0; i < this.ingredientTechs.length; i++) {
         if (copy.ingredientTechs[i] == null) {
            this.ingredientTechs[i] = null;
         } else {
            this.ingredientTechs[i] = new HashSet<>(copy.ingredientTechs[i]);
         }
      }

      this.resultItemRecipes = new ArrayList[copy.resultItemRecipes.length];

      for (int i = 0; i < this.resultItemRecipes.length; i++) {
         if (copy.resultItemRecipes[i] == null) {
            this.resultItemRecipes[i] = null;
         } else {
            this.resultItemRecipes[i] = new ArrayList<>(copy.resultItemRecipes[i]);
         }
      }

      this.ingredientItemRecipes = new ArrayList[copy.ingredientItemRecipes.length];

      for (int i = 0; i < this.ingredientItemRecipes.length; i++) {
         if (copy.ingredientItemRecipes[i] == null) {
            this.ingredientItemRecipes[i] = null;
         } else {
            this.ingredientItemRecipes[i] = new ArrayList<>(copy.ingredientItemRecipes[i]);
         }
      }

      this.hash = copy.hash;
   }

   RecipeList copy() {
      return new RecipeList(this);
   }

   private void loadDefaultRecipes() {
      this.applyList(Recipes.getDefaultRecipes());
   }

   void addModRecipes(List<Recipe> modRecipes) {
      this.recipes.ensureCapacity(this.recipes.size() + modRecipes.size());

      for (Recipe recipe : modRecipes) {
         this.addRecipe(recipe);
      }

      this.updateStatic();
      this.resetHash();
   }

   private void applyList(List<Recipe> recipes) {
      this.recipes = new ArrayList<>(recipes.size());
      this.techRecipes = new HashMapArrayList<>();

      for (Recipe recipe : recipes) {
         this.addRecipe(recipe);
      }

      this.updateStatic();
      this.resetHash();
   }

   private void addRecipe(Recipe recipe) {
      int addedIndex = -1;
      if (recipe.shouldBeSorted()) {
         for (int i = 0; i < this.recipes.size(); i++) {
            Recipe o = this.recipes.get(i);
            if (recipe.shouldShowBefore(o)) {
               addedIndex = i;
               this.recipes.add(i, recipe);
               break;
            }

            if (recipe.shouldShowAfter(o)) {
               addedIndex = i + 1;
               this.recipes.add(i + 1, recipe);
               break;
            }
         }
      }

      if (addedIndex == -1) {
         addedIndex = this.recipes.size();
         this.recipes.add(recipe);
      }

      for (int i = 0; i < this.recipes.size(); i++) {
         Recipe r = this.recipes.get(i);
         if (r.shouldBeSorted()) {
            if (r.shouldShowBefore(recipe)) {
               int adjustedIndex = i < addedIndex ? -1 : 0;
               this.recipes.remove(i);
               this.recipes.add(addedIndex + adjustedIndex, r);
               break;
            }

            if (r.shouldShowAfter(recipe)) {
               int adjustedIndex = i < addedIndex ? -1 : 0;
               this.recipes.remove(i);
               this.recipes.add(addedIndex + adjustedIndex + 1, r);
               break;
            }
         }
      }

      this.techRecipes.add(recipe.tech.getStringID(), recipe);
   }

   private void updateStatic() {
      this.hashToRecipeIndexMap = new HashMap<>();
      List<Item> itemList = ItemRegistry.getItems();
      this.itemIsCraftingMat = new boolean[itemList.size()];
      this.ingredientTechs = new HashSet[itemList.size()];
      this.resultItemRecipes = new ArrayList[itemList.size()];
      this.ingredientItemRecipes = new ArrayList[itemList.size()];

      for (int i = 0; i < this.recipes.size(); i++) {
         Recipe recipe = this.recipes.get(i);
         this.hashToRecipeIndexMap.put(recipe.getRecipeHash(), i);
         ArrayList<Recipe> list = this.resultItemRecipes[recipe.resultID];
         if (list == null) {
            list = new ArrayList<>();
            this.resultItemRecipes[recipe.resultID] = list;
         }

         list.add(recipe);

         for (Ingredient ingredient : recipe.ingredients) {
            if (ingredient.isGlobalIngredient()) {
               for (int id : ingredient.getGlobalIngredient().getRegisteredItemIDs()) {
                  this.itemIsCraftingMat[id] = true;
                  this.addResultItem(id, recipe);
               }
            } else {
               int id = ingredient.getIngredientID();
               this.itemIsCraftingMat[id] = true;
               this.addResultItem(id, recipe);
            }
         }
      }
   }

   private void addResultItem(int itemID, Recipe recipe) {
      HashSet<Tech> techs = this.ingredientTechs[itemID];
      if (techs == null) {
         techs = new HashSet<>();
         this.ingredientTechs[itemID] = techs;
      }

      techs.add(recipe.tech);
      ArrayList<Recipe> list = this.ingredientItemRecipes[itemID];
      if (list == null) {
         list = new ArrayList<>();
         this.ingredientItemRecipes[itemID] = list;
      }

      list.add(recipe);
   }

   public boolean isCraftingMat(int itemID) {
      return this.itemIsCraftingMat[itemID];
   }

   public HashSet<Tech> getCraftingMatTechs(int itemID) {
      HashSet<Tech> techs = this.ingredientTechs[itemID];
      return techs == null ? new HashSet<>() : techs;
   }

   private static int compileHash(List<Recipe> recipes) {
      int hash = 1;

      for (Recipe recipe : recipes) {
         hash = hash * 23 + recipe.getRecipeHash();
      }

      return hash;
   }

   private void resetHash() {
      this.hash = 0;
   }

   public int getHash() {
      if (this.hash == 0) {
         this.hash = compileHash(this.recipes);
      }

      return this.hash;
   }

   public Recipe getRecipe(int index) {
      return this.recipes.get(index);
   }

   public Stream<Recipe> streamRecipes() {
      return this.recipes.stream();
   }

   public Iterable<Recipe> getRecipes() {
      return this.recipes;
   }

   public boolean hasRecipeHash(int recipeHash) {
      return this.hashToRecipeIndexMap.containsKey(recipeHash);
   }

   public int getRecipeIndexFromHash(int recipeHash) {
      return this.hashToRecipeIndexMap.getOrDefault(recipeHash, -1);
   }

   public Recipe getRecipeFromHash(int recipeHash) {
      int index = this.getRecipeIndexFromHash(recipeHash);
      return index == -1 ? null : this.recipes.get(index);
   }

   public Stream<Recipe> streamRecipes(Tech... techs) {
      return Arrays.stream(techs).flatMap(t -> this.techRecipes.stream(t.getStringID()));
   }

   public Iterable<Recipe> getRecipes(Tech tech) {
      return this.techRecipes.get(tech.getStringID());
   }

   public Iterable<Recipe> getRecipes(Tech... techs) {
      return () -> Arrays.stream(techs).flatMap(t -> this.techRecipes.stream(t.getStringID())).iterator();
   }

   public int getTotalRecipes() {
      return this.recipes.size();
   }

   public ArrayList<Recipe> getRecipesFromResult(int itemID) {
      ArrayList<Recipe> out = this.resultItemRecipes[itemID];
      if (out == null) {
         out = new ArrayList<>();
      }

      return new ArrayList<>(out);
   }

   public ArrayList<Recipe> getRecipesFromIngredient(int itemID) {
      ArrayList<Recipe> out = this.ingredientItemRecipes[itemID];
      if (out == null) {
         out = new ArrayList<>();
      }

      return new ArrayList<>(out);
   }

   public ArrayList<Recipe> getRecipesFromResultAndIngredient(int itemID) {
      ArrayList<Recipe> out = this.resultItemRecipes[itemID];
      if (out == null) {
         out = new ArrayList<>();
      }

      ArrayList<Recipe> ingredientRecipes = this.ingredientItemRecipes[itemID];
      if (ingredientRecipes != null) {
         out.ensureCapacity(out.size() + ingredientRecipes.size());

         for (Recipe recipe : ingredientRecipes) {
            boolean valid = true;
            int hash = recipe.getRecipeHash();

            for (Recipe r : out) {
               if (r.getRecipeHash() == hash) {
                  valid = false;
                  break;
               }
            }

            if (valid) {
               out.add(recipe);
            }
         }
      }

      return out;
   }
}
