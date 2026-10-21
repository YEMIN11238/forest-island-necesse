package necesse.entity.objectEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import necesse.engine.Settings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.presets.containerComponent.object.ProcessingHelp;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.CanCraft;
import necesse.inventory.recipe.FueledProcessingTechRecipeCraftedEvent;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;

public abstract class FueledProcessingTechInventoryObjectEntity extends FueledProcessingInventoryObjectEntity {
   public Tech[] techs;
   private int expectedCrafts;
   private ArrayList<InventoryItem> expectedResultItems;
   private FueledProcessingTechInventoryObjectEntity.TechProcessingHelp help = new FueledProcessingTechInventoryObjectEntity.TechProcessingHelp();

   public FueledProcessingTechInventoryObjectEntity(
      Level level,
      String type,
      int x,
      int y,
      int fuelSlots,
      int inputSlots,
      int outputSlots,
      boolean fuelAlwaysOn,
      boolean fuelRunsOutWhenNotProcessing,
      boolean runningOutOfFuelResetsProcessingTime,
      Tech... techs
   ) {
      super(level, type, x, y, fuelSlots, inputSlots, outputSlots, fuelAlwaysOn, fuelRunsOutWhenNotProcessing, runningOutOfFuelResetsProcessingTime);
      this.techs = techs;
   }

   @Override
   public boolean isValidInputItem(InventoryItem item) {
      return item == null ? false : Recipes.streamRecipes(this.techs).anyMatch(r -> {
         for (Ingredient ingredient : r.ingredients) {
            if (ingredient.matchesItem(item.item)) {
               return true;
            }
         }

         return false;
      });
   }

   @Override
   protected void onSlotUpdate(int slot) {
      super.onSlotUpdate(slot);
      this.help.forceUpdate = true;
      this.expectedResultItems = null;
   }

   public FueledProcessingTechInventoryObjectEntity.FutureCrafts getExpectedResults() {
      if (this.expectedResultItems == null) {
         this.expectedCrafts = 0;
         this.expectedResultItems = new ArrayList<>();
         Inventory copy = this.inventory.copy();

         for (int j = 0; j < 1000; j++) {
            boolean success = false;

            for (int i = this.fuelSlots + this.inputSlots - 1; i >= this.fuelSlots; i--) {
               InventoryRange useRange = new InventoryRange(copy, i, this.fuelSlots + this.inputSlots - 1);

               for (Recipe recipe : Recipes.getRecipes(this.techs)) {
                  if (recipe.canCraftRange(this.getLevel(), null, useRange, false).canCraft()) {
                     InventoryItem resultItem = recipe.resultItem.copy(recipe.resultAmount);
                     resultItem.combineOrAddToList(this.getLevel(), null, this.expectedResultItems, "add");
                     recipe.craftRange(this.getLevel(), null, useRange);
                     this.expectedCrafts++;
                     success = true;
                     break;
                  }
               }

               if (success) {
                  break;
               }
            }

            if (!success) {
               break;
            }
         }
      }

      ArrayList<InventoryItem> out = new ArrayList<>(this.expectedResultItems.size());

      for (InventoryItem expectedResult : this.expectedResultItems) {
         out.add(expectedResult.copy());
      }

      return new FueledProcessingTechInventoryObjectEntity.FutureCrafts(this.expectedCrafts, out);
   }

   public FueledProcessingTechInventoryObjectEntity.FutureCrafts getCurrentAndExpectedResults() {
      FueledProcessingTechInventoryObjectEntity.FutureCrafts combined = this.getExpectedResults();

      for (int i = this.fuelSlots + this.inputSlots; i < this.inventory.getSize(); i++) {
         InventoryItem item = this.inventory.getItem(i);
         if (item != null) {
            item.copy().combineOrAddToList(this.getLevel(), null, combined.items, "add");
         }
      }

      return combined;
   }

   @Override
   public FueledProcessingInventoryObjectEntity.NextProcessTask getNextProcessTask() {
      InventoryRange invRange = new InventoryRange(this.inventory, this.fuelSlots, this.fuelSlots + this.inputSlots - 1);
      Recipe recipe = Recipes.streamRecipes(this.techs)
         .filter(r -> r.canCraftRange(this.getLevel(), null, invRange, false).canCraft())
         .findFirst()
         .orElse(null);
      return recipe != null ? new FueledProcessingInventoryObjectEntity.NextProcessTask(recipe.getRecipeHash(), this.getProcessTime(recipe)) : null;
   }

   public abstract int getProcessTime(Recipe var1);

   @Override
   public boolean processInput() {
      for (int i = this.fuelSlots + this.inputSlots - 1; i >= this.fuelSlots; i--) {
         InventoryRange invRange = new InventoryRange(this.inventory, i, this.fuelSlots + this.inputSlots - 1);

         for (Recipe recipe : Recipes.getRecipes(this.techs)) {
            if (recipe.canCraftRange(this.getLevel(), null, invRange, false).canCraft() && this.canAddOutput(recipe.resultItem.copy(recipe.resultAmount))) {
               FueledProcessingTechRecipeCraftedEvent event = new FueledProcessingTechRecipeCraftedEvent(
                  recipe, recipe.craftRange(this.getLevel(), null, invRange), this
               );
               recipe.submitCraftedEvent(event);
               if (event.resultItem != null) {
                  this.addOutput(event.resultItem);
               }

               return true;
            }
         }
      }

      return false;
   }

   @Override
   public ProcessingHelp getProcessingHelp() {
      return this.help;
   }

   @Override
   public void onMouseHover(PlayerMob perspective, boolean debug) {
      super.onMouseHover(perspective, debug);
      if (debug) {
         FueledProcessingTechInventoryObjectEntity.FutureCrafts results = this.getCurrentAndExpectedResults();
         if (results.crafts > 0 || !results.items.isEmpty()) {
            StringTooltips tooltips = new StringTooltips("Expected results from " + results.crafts + " crafts:");

            for (InventoryItem result : results.items) {
               tooltips.add("  " + result.getAmount() + "x " + result.getItemDisplayName());
            }

            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   public static class FutureCrafts {
      public final int crafts;
      public final ArrayList<InventoryItem> items;

      public FutureCrafts(int crafts, ArrayList<InventoryItem> items) {
         this.crafts = crafts;
         this.items = items;
      }
   }

   private class TechProcessingHelp extends ProcessingHelp {
      public boolean forceUpdate = true;
      public boolean lastShowIngredientsAvailable = Settings.showIngredientsAvailable;
      public Recipe currentRecipe;
      public CanCraft currentRecipeCanCraft;
      public HashSet<Integer> showRecipeTooltip = new HashSet<>();
      public HashMap<Integer, Ingredient> inputGhostItems = new HashMap<>();
      public HashMap<Integer, InventoryItem> outputGhostItems = new HashMap<>();

      private TechProcessingHelp() {
      }

      public void update() {
         this.forceUpdate = false;
         this.lastShowIngredientsAvailable = Settings.showIngredientsAvailable;
         this.currentRecipe = null;
         this.currentRecipeCanCraft = null;
         this.showRecipeTooltip.clear();
         this.inputGhostItems.clear();
         this.outputGhostItems.clear();
         InventoryRange inputRange = FueledProcessingTechInventoryObjectEntity.this.getInputInventoryRange();

         for (int i = inputRange.endSlot; i >= inputRange.startSlot; i--) {
            InventoryRange invRange = new InventoryRange(FueledProcessingTechInventoryObjectEntity.this.inventory, i, inputRange.endSlot);

            for (Recipe recipe : Recipes.getRecipes(FueledProcessingTechInventoryObjectEntity.this.techs)) {
               CanCraft canCraft = recipe.canCraftRange(
                  FueledProcessingTechInventoryObjectEntity.this.getLevel(), null, invRange, FueledProcessingTechInventoryObjectEntity.this.isClient()
               );
               if (canCraft.canCraft()) {
                  this.currentRecipe = recipe;
                  this.currentRecipeCanCraft = canCraft;
                  break;
               }
            }

            if (this.currentRecipe != null) {
               break;
            }
         }

         if (this.currentRecipe == null) {
            for (Recipe recipe : Recipes.getRecipes(FueledProcessingTechInventoryObjectEntity.this.techs)) {
               CanCraft canCraft = recipe.canCraftRange(
                  FueledProcessingTechInventoryObjectEntity.this.getLevel(), null, inputRange, FueledProcessingTechInventoryObjectEntity.this.isClient()
               );
               if (canCraft.hasAnyItems()) {
                  this.currentRecipe = recipe;
                  this.currentRecipeCanCraft = canCraft;
                  break;
               }
            }
         }

         if (this.currentRecipe != null) {
            if (!this.currentRecipeCanCraft.hasAnyOfAllItems()) {
               for (int i = 0; i < this.currentRecipe.ingredients.length; i++) {
                  if (!this.currentRecipeCanCraft.hasAnyIngredients(i)) {
                     Ingredient ingredient = this.currentRecipe.ingredients[i];
                     boolean found = false;

                     for (int j = inputRange.startSlot; j <= inputRange.endSlot; j++) {
                        InventoryItem item = inputRange.inventory.getItem(j);
                        if (item != null && ingredient.matchesItem(item.item)) {
                           this.inputGhostItems.put(j, ingredient);
                           found = true;
                           break;
                        }
                     }

                     if (!found) {
                        for (int j = inputRange.startSlot; j <= inputRange.endSlot; j++) {
                           if (inputRange.inventory.isSlotClear(j)) {
                              this.inputGhostItems.put(j, ingredient);
                              this.showRecipeTooltip.add(j);
                              break;
                           }
                        }
                     }
                  }
               }
            }

            if (this.currentRecipe != null) {
               InventoryItem resultItem = this.currentRecipe.resultItem.copy(this.currentRecipe.resultAmount);
               InventoryRange outputRange = FueledProcessingTechInventoryObjectEntity.this.getOutputInventoryRange();
               Inventory inv = FueledProcessingTechInventoryObjectEntity.this.inventory.copy();
               inv.addItem(
                  FueledProcessingTechInventoryObjectEntity.this.getLevel(),
                  null,
                  resultItem,
                  outputRange.startSlot,
                  outputRange.endSlot,
                  false,
                  "add",
                  true,
                  false,
                  null
               );

               for (int i = outputRange.startSlot; i <= outputRange.endSlot; i++) {
                  InventoryItem item = inv.getItem(i);
                  if (item != null
                     && item.equals(
                        FueledProcessingTechInventoryObjectEntity.this.getLevel(),
                        this.currentRecipe.resultItem.copy(this.currentRecipe.resultAmount),
                        true,
                        false,
                        "equals"
                     )) {
                     this.outputGhostItems.put(i, this.currentRecipe.resultItem);
                     this.showRecipeTooltip.add(i);
                  }
               }
            }
         }
      }

      @Override
      public boolean isProcessing() {
         return FueledProcessingTechInventoryObjectEntity.this.isProcessingRunning();
      }

      @Override
      public float getProcessingProgress() {
         return FueledProcessingTechInventoryObjectEntity.this.getProcessingProgress();
      }

      @Override
      public boolean needsFuel() {
         return !FueledProcessingTechInventoryObjectEntity.this.hasFuel();
      }

      @Override
      public InventoryItem getGhostItem(int slot) {
         if (this.shouldUpdate()) {
            this.update();
         }

         InventoryItem outputGhost = this.outputGhostItems.get(slot);
         if (outputGhost != null) {
            return outputGhost;
         }

         Ingredient ingredient = this.inputGhostItems.get(slot);
         if (ingredient != null) {
            Item displayItem = ingredient.getDisplayItem(null);
            if (displayItem != null) {
               return displayItem.getDefaultItem(null, 1);
            }
         }

         return null;
      }

      @Override
      public GameTooltips getTooltip(int slot, PlayerMob perspective) {
         if (this.shouldUpdate()) {
            this.update();
         }

         return this.showRecipeTooltip.contains(slot) && this.currentRecipe != null
            ? this.currentRecipe.getTooltip(this.currentRecipeCanCraft, perspective, new GameBlackboard())
            : null;
      }

      @Override
      public GameTooltips getCurrentRecipeTooltip(PlayerMob perspective) {
         if (this.shouldUpdate()) {
            this.update();
         }

         return this.currentRecipe != null ? this.currentRecipe.getTooltip(this.currentRecipeCanCraft, perspective, new GameBlackboard()) : null;
      }

      public boolean shouldUpdate() {
         return this.forceUpdate ? true : Settings.showIngredientsAvailable != this.lastShowIngredientsAvailable;
      }
   }
}
