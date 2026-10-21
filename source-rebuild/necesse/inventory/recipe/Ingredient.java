package necesse.inventory.recipe;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.GlobalIngredientRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapSet;
import necesse.engine.util.MapIterator;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.fairType.FairCharacterGlyph;
import necesse.gfx.fairType.FairColorChangeGlyph;
import necesse.gfx.fairType.FairGlyph;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.gfx.fairType.FairSpacerGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.FairTypeTooltip;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemSearchTester;
import necesse.level.maps.Level;

public class Ingredient {
   public final String ingredientStringID;
   private int ingredientID;
   private final int itemAmount;
   private final boolean requiredToShow;
   private final boolean isGlobalIngredient;

   public Ingredient(String ingredientStringID, int itemAmount, boolean requiredToShow) {
      this.ingredientStringID = ingredientStringID;
      this.itemAmount = itemAmount;
      this.requiredToShow = requiredToShow;
      int itemID = ItemRegistry.getItemID(ingredientStringID);
      this.ingredientID = -1;
      if (itemID == -1) {
         this.isGlobalIngredient = true;
         this.ingredientID = GlobalIngredientRegistry.getGlobalIngredientID(ingredientStringID);
      } else {
         this.isGlobalIngredient = false;
         this.ingredientID = itemID;
      }
   }

   public Ingredient(String ingredientStringID, int itemAmount) {
      this(ingredientStringID, itemAmount, false);
   }

   public Ingredient(PacketReader reader) {
      this.isGlobalIngredient = reader.getNextBoolean();
      this.ingredientID = reader.getNextShort();
      if (this.isGlobalIngredient) {
         this.ingredientStringID = GlobalIngredientRegistry.getGlobalIngredient(this.ingredientID).getStringID();
      } else {
         this.ingredientStringID = ItemRegistry.getItem(this.ingredientID).getStringID();
      }

      this.itemAmount = reader.getNextShort();
      this.requiredToShow = reader.getNextBoolean();
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextBoolean(this.isGlobalIngredient());
      writer.putNextShortUnsigned(this.getIngredientID());
      writer.putNextShortUnsigned(this.getIngredientAmount());
      writer.putNextBoolean(this.requiredToShow());
   }

   public int getIngredientID() {
      return this.ingredientID;
   }

   public String getDisplayName() {
      return this.isGlobalIngredient() ? this.getGlobalIngredient().displayName.translate() : ItemRegistry.getDisplayName(this.getIngredientID());
   }

   public int getIngredientAmount() {
      return this.itemAmount;
   }

   public boolean requiredToShow() {
      return this.requiredToShow;
   }

   public boolean isGlobalIngredient() {
      return this.isGlobalIngredient;
   }

   public boolean matchesItem(Item item) {
      return this.isGlobalIngredient() ? item.isGlobalIngredient(this.getGlobalIngredient()) : item.getID() == this.getIngredientID();
   }

   public boolean equals(Ingredient other) {
      if (other == this) {
         return true;
      } else if (other == null) {
         return false;
      } else if (this.isGlobalIngredient != other.isGlobalIngredient) {
         return false;
      } else {
         return this.ingredientID != other.ingredientID ? false : this.itemAmount == other.itemAmount;
      }
   }

   public GlobalIngredient getGlobalIngredient() {
      return !this.isGlobalIngredient() ? null : GlobalIngredientRegistry.getGlobalIngredient(this.getIngredientID());
   }

   public boolean matchesSearch(PlayerMob player, ItemSearchTester tester) {
      if (this.isGlobalIngredient()) {
         GlobalIngredient globalIngredient = this.getGlobalIngredient();
         return globalIngredient.getRegisteredItemIDs()
            .stream()
            .map(ItemRegistry::getItem)
            .filter(Objects::nonNull)
            .anyMatch(itemx -> tester.matches(itemx.getDefaultItem(player, 1), player, new GameBlackboard()));
      } else {
         Item item = ItemRegistry.getItem(this.getIngredientID());
         return item == null ? false : tester.matches(item.getDefaultItem(player, 1), player, new GameBlackboard());
      }
   }

   public boolean hasIngredient(Level level, PlayerMob player, Inventory inv) {
      return this.hasIngredient(level, player, Collections.singletonList(inv));
   }

   public boolean hasIngredient(Level level, PlayerMob player, Iterable<Inventory> invList) {
      return this.hasIngredientRange(level, player, () -> new MapIterator<>(invList.iterator(), InventoryRange::new));
   }

   public boolean hasIngredientRange(Level level, PlayerMob player, InventoryRange inv) {
      return this.hasIngredientRange(level, player, Collections.singletonList(inv));
   }

   public boolean hasIngredientRange(Level level, PlayerMob player, Iterable<InventoryRange> invList) {
      AtomicInteger totalFound = new AtomicInteger();
      AtomicBoolean foundAnyItem = new AtomicBoolean();
      boolean useItem = this.itemAmount > 0;
      HashMapSet<Inventory, Integer> usedSlots = new HashMapSet<>();

      for (InventoryRange range : invList) {
         if (range.inventory.canBeUsedForCrafting()) {
            IngredientStep result = range.inventory
               .countIngredientAmount(level, player, range.startSlot, range.endSlot, "crafting", (inventory, slot, item) -> {
                  if (inventory != null && !inventory.canBeUsedForCrafting()) {
                     return IngredientStep.CONTINUE;
                  }

                  if (usedSlots.get(inventory).contains(slot)) {
                     return IngredientStep.CONTINUE;
                  }

                  if (this.matchesItem(item.item)) {
                     foundAnyItem.set(true);
                     if (!useItem) {
                        return IngredientStep.BREAK;
                     }

                     if (totalFound.addAndGet(item.getAmount()) >= this.getIngredientAmount()) {
                        usedSlots.add(inventory, slot);
                        return IngredientStep.BREAK;
                     }
                  }

                  usedSlots.add(inventory, slot);
                  return IngredientStep.CONTINUE;
               });
            if (result == IngredientStep.BREAK) {
               break;
            }
         }
      }

      return !useItem ? foundAnyItem.get() : totalFound.get() >= this.getIngredientAmount();
   }

   public int getIngredientHash() {
      int hash = 1;
      hash = hash * 17 + this.ingredientStringID.hashCode();
      return hash * 31 + this.itemAmount;
   }

   public Item getDisplayItem(Predicate<Item> itemFilter) {
      if (!this.isGlobalIngredient()) {
         return ItemRegistry.getItem(this.getIngredientID());
      }

      GlobalIngredient globalIngredient = this.getGlobalIngredient();
      ArrayList<Integer> registeredItemIDs = globalIngredient.getObtainableRegisteredItemIDs();
      if (itemFilter != null) {
         ArrayList<Integer> registeredItemIDsFiltered = new ArrayList<>(registeredItemIDs.size());

         for (Integer itemID : registeredItemIDs) {
            Item item = ItemRegistry.getItem(itemID);
            if (itemFilter.test(item)) {
               registeredItemIDsFiltered.add(itemID);
            }
         }

         if (!registeredItemIDsFiltered.isEmpty()) {
            registeredItemIDs = registeredItemIDsFiltered;
         }
      }

      if (registeredItemIDs.isEmpty()) {
         registeredItemIDs = globalIngredient.getRegisteredItemIDs();
      }

      int index = (int)(System.currentTimeMillis() / 1000L % registeredItemIDs.size());
      return ItemRegistry.getItem(registeredItemIDs.get(index));
   }

   public GameColor getCanCraftColor(int haveAmount, Predicate<Item> itemFilter) {
      boolean canCraft = this.getIngredientAmount() == 0 ? haveAmount == -1 : haveAmount >= this.getIngredientAmount();
      if (canCraft) {
         if (this.isGlobalIngredient()) {
            return GameColor.WHITE;
         }

         Item displayItem = this.getDisplayItem(itemFilter);
         return displayItem == null ? GameColor.WHITE : displayItem.getRarityColor(displayItem.getDefaultItem(null, 1));
      } else {
         return GameColor.RED;
      }
   }

   public GameTooltips getTooltips() {
      return this.getTooltips(this.getIngredientAmount(), false, null);
   }

   public GameTooltips getTooltips(int haveAmount, boolean countAllIngredients, Predicate<Item> itemFilter) {
      return new FairTypeTooltip(
         this.getTooltipText(
            new FontOptions(Settings.tooltipTextSize).outline(),
            haveAmount,
            new FairColorChangeGlyph(this.getCanCraftColor(haveAmount, itemFilter)),
            countAllIngredients,
            itemFilter
         ),
         10
      );
   }

   public FairType getTooltipText(
      FontOptions fontOptions, int haveAmount, Color canCraftColor, Color cannotCraftColor, boolean countAllIngredients, Predicate<Item> itemFilter
   ) {
      boolean canCraft = this.getIngredientAmount() == 0 ? haveAmount == -1 : haveAmount >= this.getIngredientAmount();
      Color color;
      if (canCraft) {
         color = canCraftColor;
      } else {
         color = cannotCraftColor;
      }

      return this.getTooltipText(fontOptions, haveAmount, new FairColorChangeGlyph(color), countAllIngredients, itemFilter);
   }

   public FairType getTooltipText(
      FontOptions fontOptions, int haveAmount, FairColorChangeGlyph colorGlyph, boolean countAllIngredients, Predicate<Item> itemFilter
   ) {
      FairType fairType = new FairType();
      Item displayItem = this.getDisplayItem(itemFilter);
      if (colorGlyph != null) {
         fairType.append(colorGlyph);
      }

      if (this.getIngredientAmount() > 0) {
         if (displayItem != null) {
            fairType.append(new FairItemGlyph(fontOptions.getSize(), displayItem.getDefaultItem(null, 1)));
            fairType.append(new FairSpacerGlyph(5.0F, 2.0F));
         }

         fairType.append(fontOptions, GameUtils.formatNumber(this.getIngredientAmount()));
         if (countAllIngredients) {
            FontOptions haveFontOptions = fontOptions.copy().size(fontOptions.getSize() - 4);
            fairType.append("/" + GameUtils.metricNumber(haveAmount), c -> {
               FairCharacterGlyph glyph = new FairCharacterGlyph(haveFontOptions, c);
               glyph.drawYOffset = -1;
               return glyph;
            });
         }

         fairType.append(fontOptions, " " + this.getDisplayName());
      } else if (displayItem != null) {
         FairGlyph[] displayName = FairCharacterGlyph.fromString(fontOptions, this.getDisplayName());
         FairGlyph[] total = GameUtils.concat(
            new FairGlyph[]{new FairItemGlyph(fontOptions.getSize(), displayItem.getDefaultItem(null, 1)), new FairSpacerGlyph(5.0F, 2.0F)}, displayName
         );
         fairType.append(fontOptions, Localization.translate("misc", "ingredientmusthave"));
         fairType.replaceAll("<ingredient>", total);
      } else {
         fairType.append(fontOptions, Localization.translate("misc", "ingredientmusthave", "ingredient", this.getDisplayName()));
      }

      if (this.requiredToShow()) {
         fairType.append(fontOptions, "*");
      }

      return fairType;
   }
}
