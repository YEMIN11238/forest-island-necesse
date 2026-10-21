package necesse.inventory.recipe;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import necesse.engine.localization.Localization;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.HashMapSet;
import necesse.engine.util.MapIterator;
import necesse.engine.util.ObjectValue;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryItemsRemoved;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.level.maps.Level;

public class Recipe {
   public final Ingredient[] ingredients;
   public final int resultID;
   public final String resultStringID;
   public final int resultAmount;
   public final Tech tech;
   public final boolean isHidden;
   public final InventoryItem resultItem;
   private final GNDItemMap gndData;
   private InventoryItem drawItem;
   private int drawAmount;
   protected String sortResultID;
   protected boolean sortBefore;
   protected ItemCategory craftingCategoryOverride;
   protected boolean hasAnyGlobalIngredientAsResult = false;
   protected boolean canGlobalIngredientsUseResult = false;
   protected ArrayList<Consumer<RecipeCraftedEvent>> craftedListeners = new ArrayList<>();
   private int calculatedHash;

   public Recipe(String resultStringID, Tech tech, Ingredient[] ingredients) {
      this(resultStringID, 1, tech, ingredients);
   }

   public Recipe(String resultStringID, int resultAmount, Tech tech, Ingredient[] ingredients) {
      this(resultStringID, resultAmount, tech, ingredients, false);
   }

   public Recipe(String resultStringID, int resultAmount, Tech tech, Ingredient[] ingredients, boolean isHidden) {
      this(resultStringID, resultAmount, tech, ingredients, isHidden, null);
   }

   public Recipe(String resultStringID, int resultAmount, Tech tech, Ingredient[] ingredients, boolean isHidden, GNDItemMap gndData) {
      this.resultStringID = resultStringID;
      Item resultItem = ItemRegistry.getItem(resultStringID);
      if (resultItem == null) {
         throw new NullPointerException("Could not find recipe result item: " + resultStringID);
      }

      this.resultID = resultItem.getID();
      if (resultAmount <= 0) {
         resultAmount = 1;
      }

      this.resultAmount = resultAmount;
      HashSet<String> ingTypes = new HashSet<>();

      for (Ingredient ing : ingredients) {
         if (ingTypes.contains(ing.ingredientStringID)) {
            throw new IllegalArgumentException("Duplicate ingredient '" + ing.ingredientStringID + "'");
         }

         ingTypes.add(ing.ingredientStringID);
         if (ing.isGlobalIngredient() && ing.matchesItem(resultItem)) {
            this.hasAnyGlobalIngredientAsResult = true;
         }
      }

      this.ingredients = ingredients;
      this.tech = tech;
      this.isHidden = isHidden;
      this.resultItem = new InventoryItem(resultItem, resultAmount);
      this.drawItem = new InventoryItem(resultItem);
      this.drawAmount = resultAmount;
      if (gndData == null) {
         gndData = new GNDItemMap();
      }

      this.gndData = gndData;
      this.resultItem.setGndData(gndData.copy());
      this.drawItem.setGndData(gndData.copy());
   }

   public static Recipe fromScript(String script) {
      return new RecipeData(new LoadData(script)).validate();
   }

   public Recipe showBefore(String itemStringID) {
      if (itemStringID != null && ItemRegistry.getItemID(itemStringID) == -1) {
         throw new IllegalArgumentException("No item found with stringID " + itemStringID);
      }

      this.sortResultID = itemStringID;
      this.sortBefore = true;
      return this;
   }

   public Recipe showAfter(String itemStringID) {
      if (itemStringID != null && ItemRegistry.getItemID(itemStringID) == -1) {
         throw new IllegalArgumentException("No item found with stringID " + itemStringID);
      }

      this.sortResultID = itemStringID;
      this.sortBefore = false;
      return this;
   }

   public boolean shouldShowBefore(Recipe other) {
      return this.sortBefore && other.resultStringID.equals(this.sortResultID);
   }

   public boolean shouldShowAfter(Recipe other) {
      return !this.sortBefore && other.resultStringID.equals(this.sortResultID);
   }

   public boolean shouldBeSorted() {
      return this.sortResultID != null;
   }

   public Recipe setCraftingCategory(String... categoryTree) {
      this.craftingCategoryOverride = ItemCategory.craftingManager.getCategory(categoryTree);
      return this;
   }

   public ItemCategory getCraftingCategory() {
      return this.craftingCategoryOverride;
   }

   protected void setShowItem(String showItem, int drawAmount) {
      this.drawItem = new InventoryItem(showItem != null && !showItem.equals("") ? showItem : this.resultStringID);
      this.drawAmount = drawAmount;
   }

   public Recipe setCanGlobalIngredientsUseResult() {
      this.canGlobalIngredientsUseResult = true;
      return this;
   }

   public Predicate<Item> getIngredientItemFilter() {
      return this.hasAnyGlobalIngredientAsResult && !this.canGlobalIngredientsUseResult ? item -> item.getID() != this.resultID : null;
   }

   public boolean matchTech(Tech tech) {
      return tech == RecipeTechRegistry.ALL || this.tech == tech;
   }

   public boolean matchesTechs(Tech... techs) {
      return Arrays.stream(techs).anyMatch(this::matchTech);
   }

   public GNDItemMap getGndData() {
      return this.gndData;
   }

   public int getRecipeHash() {
      if (this.calculatedHash != 0) {
         return this.calculatedHash;
      }

      int hash = 1;
      hash = hash * 19 + this.resultStringID.hashCode();
      hash = hash * 37 + this.tech.getStringID().hashCode();
      hash = hash * 29 + this.resultAmount;

      for (Ingredient in : this.ingredients) {
         hash = hash * 13 + in.getIngredientHash();
      }

      for (int gndKey : this.gndData.getKeySet()) {
         hash = hash * 43 + gndKey;
         hash = hash * 23 + this.gndData.getItem(gndKey).toString().hashCode();
      }

      if (this.sortResultID != null) {
         hash = hash * 31 + this.sortResultID.hashCode();
         hash = hash * 43 + (this.sortBefore ? 1 : 2);
      } else {
         hash *= 41;
      }

      this.calculatedHash = hash;
      return this.calculatedHash;
   }

   public static boolean doesShow(
      Ingredient[] ingredients, boolean isHidden, Level level, PlayerMob player, Inventory inv, Predicate<Item> ingredientItemFilter
   ) {
      return doesShow(ingredients, isHidden, level, player, Collections.singletonList(inv), ingredientItemFilter);
   }

   public static boolean doesShow(
      Ingredient[] ingredients, boolean isHidden, Level level, PlayerMob player, Iterable<Inventory> invList, Predicate<Item> ingredientItemFilter
   ) {
      return doesShowRange(ingredients, isHidden, level, player, () -> new MapIterator<>(invList.iterator(), InventoryRange::new), ingredientItemFilter);
   }

   public static boolean doesShowRange(
      Ingredient[] ingredients, boolean isHidden, Level level, PlayerMob player, InventoryRange inv, Predicate<Item> ingredientItemFilter
   ) {
      return doesShowRange(ingredients, isHidden, level, player, Collections.singletonList(inv), ingredientItemFilter);
   }

   public static boolean doesShowRange(
      Ingredient[] ingredients, boolean isHidden, Level level, PlayerMob player, Iterable<InventoryRange> invList, Predicate<Item> ingredientItemFilter
   ) {
      if (isHidden) {
         return canCraftRange(ingredients, level, player, invList, false, ingredientItemFilter).canCraft();
      }

      for (Ingredient i : ingredients) {
         if (i.requiredToShow() && !i.hasIngredientRange(level, player, invList)) {
            return false;
         }
      }

      return true;
   }

   public static CanCraft canCraft(
      Ingredient[] ingredients, Level level, PlayerMob player, Inventory inv, boolean countAllIngredients, Predicate<Item> ingredientItemFilter
   ) {
      return canCraft(ingredients, level, player, Collections.singletonList(inv), countAllIngredients, ingredientItemFilter);
   }

   public static CanCraft canCraft(
      Ingredient[] ingredients, Level level, PlayerMob player, Iterable<Inventory> invList, boolean countAllIngredients, Predicate<Item> ingredientItemFilter
   ) {
      return canCraftRange(
         ingredients, level, player, () -> new MapIterator<>(invList.iterator(), InventoryRange::new), countAllIngredients, ingredientItemFilter
      );
   }

   public static CanCraft canCraftRange(
      Ingredient[] ingredients, Level level, PlayerMob player, InventoryRange inv, boolean countAllIngredients, Predicate<Item> ingredientItemFilter
   ) {
      return canCraftRange(ingredients, level, player, Collections.singletonList(inv), countAllIngredients, ingredientItemFilter);
   }

   public static CanCraft canCraftRange(
      Ingredient[] ingredients,
      Level level,
      PlayerMob player,
      Iterable<InventoryRange> invList,
      boolean countAllIngredients,
      Predicate<Item> ingredientItemFilter
   ) {
      CanCraft out = new CanCraft(ingredients, countAllIngredients);
      HashMapSet<Inventory, Integer> usedSlots = new HashMapSet<>();
      LinkedList<ObjectValue<Integer, Ingredient>> sortedIngredients = new LinkedList<>();
      LinkedList<ObjectValue<Integer, Ingredient>> mustHaveIngredients = new LinkedList<>();

      for (int i = ingredients.length - 1; i >= 0; i--) {
         Ingredient ingredient = ingredients[i];
         if (ingredient.getIngredientAmount() == 0) {
            mustHaveIngredients.add(new ObjectValue<>(i, ingredient));
         } else if (ingredient.isGlobalIngredient()) {
            sortedIngredients.addLast(new ObjectValue<>(i, ingredient));
         } else {
            sortedIngredients.addFirst(new ObjectValue<>(i, ingredient));
         }
      }

      for (ObjectValue<Integer, Ingredient> e : mustHaveIngredients) {
         sortedIngredients.addFirst(e);
      }

      for (InventoryRange range : invList) {
         if (range.inventory.canBeUsedForCrafting()) {
            range.inventory.countIngredientAmount(level, player, range.startSlot, range.endSlot, "crafting", (inventory, slot, item) -> {
               if (inventory != null && !inventory.canBeUsedForCrafting()) {
                  return IngredientStep.CONTINUE;
               }

               if (usedSlots.get(inventory).contains(slot)) {
                  return IngredientStep.CONTINUE;
               }

               int usedItems = 0;

               for (ObjectValue<Integer, Ingredient> e : sortedIngredients) {
                  int index = e.object;
                  Ingredient ingredient = e.value;
                  if (ingredient.matchesItem(item.item) && (ingredientItemFilter == null || ingredientItemFilter.test(item.item))) {
                     int itemsRemaining = item.getAmount() - usedItems;
                     int foundItems = out.countAllIngredients ? itemsRemaining : Math.min(itemsRemaining, ingredient.getIngredientAmount());
                     out.addIngredient(index, foundItems);
                     usedItems += foundItems;
                     if (!out.countAllIngredients && usedItems >= item.getAmount()) {
                        break;
                     }
                  }
               }

               usedSlots.add(inventory, slot);
               return IngredientStep.CONTINUE;
            });
         }
      }

      return out;
   }

   public static ArrayList<InventoryItemsRemoved> craft(
      Ingredient[] ingredients, Level level, PlayerMob player, Inventory inv, Predicate<Item> ingredientItemFilter
   ) {
      return craft(ingredients, level, player, Collections.singletonList(inv), ingredientItemFilter);
   }

   public static ArrayList<InventoryItemsRemoved> craft(
      Ingredient[] ingredients, Level level, PlayerMob player, Iterable<Inventory> invList, Predicate<Item> ingredientItemFilter
   ) {
      return craftRange(ingredients, level, player, () -> new MapIterator<>(invList.iterator(), InventoryRange::new), ingredientItemFilter);
   }

   public static ArrayList<InventoryItemsRemoved> craftRange(
      Ingredient[] ingredients, Level level, PlayerMob player, InventoryRange inv, Predicate<Item> ingredientItemFilter
   ) {
      return craftRange(ingredients, level, player, Collections.singletonList(inv), ingredientItemFilter);
   }

   public static ArrayList<InventoryItemsRemoved> craftRange(
      Ingredient[] ingredients, Level level, PlayerMob player, Iterable<InventoryRange> invList, Predicate<Item> ingredientItemFilter
   ) {
      ArrayList<InventoryItemsRemoved> usedItems = new ArrayList<>();

      for (Ingredient in : ingredients) {
         if (in.getIngredientAmount() > 0) {
            int amountRemaining = in.getIngredientAmount();

            for (InventoryRange invRange : invList) {
               if (invRange.inventory.canBeUsedForCrafting()) {
                  if (amountRemaining <= 0) {
                     break;
                  }

                  int removed = invRange.inventory
                     .removeItems(level, player, in, ingredientItemFilter, amountRemaining, invRange.startSlot, invRange.endSlot, usedItems);
                  amountRemaining -= removed;
               }
            }
         }
      }

      return usedItems;
   }

   public boolean doesShow(Level level, PlayerMob player, Inventory inv) {
      return doesShow(this.ingredients, this.isHidden, level, player, inv, this.getIngredientItemFilter());
   }

   public boolean doesShow(Level level, PlayerMob player, Iterable<Inventory> invList) {
      return doesShow(this.ingredients, this.isHidden, level, player, invList, this.getIngredientItemFilter());
   }

   public boolean doesShowRange(Level level, PlayerMob player, InventoryRange inv) {
      return doesShowRange(this.ingredients, this.isHidden, level, player, inv, this.getIngredientItemFilter());
   }

   public boolean doesShowRange(Level level, PlayerMob player, Iterable<InventoryRange> invList) {
      return doesShowRange(this.ingredients, this.isHidden, level, player, invList, this.getIngredientItemFilter());
   }

   public CanCraft canCraft(Level level, PlayerMob player, Inventory inv, boolean countAllIngredients) {
      return canCraft(this.ingredients, level, player, inv, countAllIngredients, this.getIngredientItemFilter());
   }

   public CanCraft canCraft(Level level, PlayerMob player, Iterable<Inventory> invList, boolean countAllIngredients) {
      return canCraft(this.ingredients, level, player, invList, countAllIngredients, this.getIngredientItemFilter());
   }

   public CanCraft canCraftRange(Level level, PlayerMob player, InventoryRange inv, boolean countAllIngredients) {
      return canCraftRange(this.ingredients, level, player, inv, countAllIngredients, this.getIngredientItemFilter());
   }

   public CanCraft canCraftRange(Level level, PlayerMob player, Iterable<InventoryRange> invList, boolean countAllIngredients) {
      return canCraftRange(this.ingredients, level, player, invList, countAllIngredients, this.getIngredientItemFilter());
   }

   public ArrayList<InventoryItemsRemoved> craft(Level level, PlayerMob player, Inventory inv) {
      return craft(this.ingredients, level, player, inv, this.getIngredientItemFilter());
   }

   public ArrayList<InventoryItemsRemoved> craft(Level level, PlayerMob player, Iterable<Inventory> invList) {
      return craft(this.ingredients, level, player, invList, this.getIngredientItemFilter());
   }

   public ArrayList<InventoryItemsRemoved> craftRange(Level level, PlayerMob player, InventoryRange inv) {
      return craftRange(this.ingredients, level, player, inv, this.getIngredientItemFilter());
   }

   public ArrayList<InventoryItemsRemoved> craftRange(Level level, PlayerMob player, Iterable<InventoryRange> invList) {
      return craftRange(this.ingredients, level, player, invList, this.getIngredientItemFilter());
   }

   public void submitCraftedEvent(RecipeCraftedEvent event) {
      for (int i = 0; i < this.craftedListeners.size(); i++) {
         this.craftedListeners.get(i).accept(event);
      }
   }

   public Recipe onCrafted(Consumer<RecipeCraftedEvent> listener) {
      this.craftedListeners.add(listener);
      return this;
   }

   @Deprecated
   public void draw(int x, int y, PlayerMob perspective) {
      this.draw(x, y, perspective, true);
   }

   public void draw(int x, int y, PlayerMob perspective, boolean canCraft) {
      Color drawColor = null;
      if (!canCraft) {
         drawColor = new Color(150, 120, 120, 200);
      }

      this.drawItem.drawIcon(perspective, x, y, 32, drawColor);
      int drawAmount = this.drawAmount >= 1 ? this.drawAmount : this.resultAmount;
      if (drawAmount > 1) {
         String amountString = "" + drawAmount;
         if (drawAmount > 999) {
            amountString = "999+";
         }

         int width = FontManager.bit.getWidthCeil(amountString, Item.tipFontOptions);
         FontManager.bit.drawString(x + 28 - width, y + 2, amountString, Item.tipFontOptions);
      }
   }

   public GameTooltips getTooltip(PlayerMob perspective, GameBlackboard blackboard) {
      return this.getTooltip(null, false, perspective, blackboard);
   }

   public GameTooltips getTooltip(CanCraft canCraft, PlayerMob perspective, GameBlackboard blackboard) {
      if (canCraft == null) {
         canCraft = CanCraft.allTrue(this);
      }

      return this.getTooltip(canCraft.haveIngredients, canCraft.countAllIngredients, perspective, blackboard);
   }

   public GameTooltips getTooltip(int[] haveIngredientsAmount, boolean countAllIngredients, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(this.getResultItemTooltip(perspective, blackboard));
      tooltips.add(new SpacerGameTooltip(10));
      if (this.resultAmount == 1) {
         tooltips.add(Localization.translate("misc", "recipecostsing"));
      } else {
         tooltips.add(Localization.translate("misc", "recipecostmult", "amount", this.resultAmount));
      }

      for (int i = 0; i < this.ingredients.length; i++) {
         Ingredient ingredient = this.ingredients[i];
         tooltips.add(
            ingredient.getTooltips(
               haveIngredientsAmount == null ? ingredient.getIngredientAmount() : haveIngredientsAmount[i], countAllIngredients, this.getIngredientItemFilter()
            )
         );
      }

      return tooltips;
   }

   public GameTooltips getResultItemTooltip(PlayerMob perspective, GameBlackboard blackboard) {
      return this.resultItem.getTooltip(perspective, blackboard);
   }

   public Packet getContentPacket() {
      Packet p = new Packet();
      PacketWriter writer = new PacketWriter(p);
      writer.putNextShortUnsigned(ItemRegistry.getItemID(this.resultStringID));
      writer.putNextShortUnsigned(this.resultAmount);
      Packet gndContent = this.gndData == null ? new Packet() : this.gndData.getContentPacket();
      writer.putNextContentPacket(gndContent);
      writer.putNextByteUnsigned(this.tech.getID());
      writer.putNextBoolean(this.isHidden);
      writer.putNextBoolean(this.sortResultID != null);
      if (this.sortResultID != null) {
         writer.putNextShortUnsigned(ItemRegistry.getItemID(this.sortResultID));
         writer.putNextBoolean(this.sortBefore);
      }

      writer.putNextByteUnsigned(this.ingredients.length);

      for (Ingredient in : this.ingredients) {
         in.writePacket(writer);
      }

      return p;
   }

   public static Recipe getRecipeFromContentPacket(Packet contentPacket) {
      PacketReader reader = new PacketReader(contentPacket);
      String resultID = ItemRegistry.getItem(reader.getNextShortUnsigned()).getStringID();
      int resultAmount = reader.getNextShortUnsigned();
      GNDItemMap gndData = new GNDItemMap(reader.getNextContentPacket());
      Tech tech = RecipeTechRegistry.getTech(reader.getNextByteUnsigned());
      boolean isHidden = reader.getNextBoolean();
      String sortResultID = null;
      boolean sortBefore = false;
      if (reader.getNextBoolean()) {
         sortResultID = ItemRegistry.getItem(reader.getNextShortUnsigned()).getStringID();
         sortBefore = reader.getNextBoolean();
      }

      int totalIngredients = reader.getNextByteUnsigned();
      Ingredient[] ingredients = new Ingredient[totalIngredients];

      for (int i = 0; i < totalIngredients; i++) {
         ingredients[i] = new Ingredient(reader);
      }

      Recipe recipe = new Recipe(resultID, resultAmount, tech, ingredients, isHidden, gndData);
      recipe.sortResultID = sortResultID;
      recipe.sortBefore = sortBefore;
      return recipe;
   }
}
