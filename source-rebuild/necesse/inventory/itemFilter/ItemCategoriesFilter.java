package necesse.inventory.itemFilter;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.VersionMigration;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.level.maps.Level;

public class ItemCategoriesFilter {
   private final HashMap<Integer, ItemCategoriesFilter.ItemCategoryFilter> itemIDCategories = new HashMap<>();
   private final HashMap<Integer, ItemCategoriesFilter.ItemCategoryFilter> categoryIDs = new HashMap<>();
   public final ItemCategoriesFilter.ItemCategoryFilter master;
   private final boolean initializedCategories;
   public ItemCategoriesFilter.ItemLimitMode limitMode = ItemCategoriesFilter.ItemLimitMode.TOTAL_ITEMS;
   public int minAmount;
   public int maxAmount;

   public ItemCategoriesFilter(ItemCategory itemCategory, int minAmount, int maxAmount, boolean allowAll) {
      this.minAmount = minAmount;
      this.maxAmount = maxAmount;
      this.master = new ItemCategoriesFilter.ItemCategoryFilter(itemCategory, allowAll, null);
      this.initializedCategories = true;
   }

   public ItemCategoriesFilter(int minAmount, int maxAmount, boolean allowAll) {
      this(ItemCategory.masterCategory, minAmount, maxAmount, allowAll);
   }

   public ItemCategoriesFilter(ItemCategory itemCategory, boolean allowAll) {
      this(itemCategory, Integer.MAX_VALUE, Integer.MAX_VALUE, allowAll);
   }

   public ItemCategoriesFilter(boolean allowAll) {
      this(ItemCategory.masterCategory, allowAll);
   }

   public ItemCategoriesFilter(ItemCategory itemCategory, int minAmount, int maxAmount) {
      this(itemCategory, minAmount, maxAmount, true);
   }

   public ItemCategoriesFilter(int minAmount, int maxAmount) {
      this(ItemCategory.masterCategory, minAmount, maxAmount);
   }

   public void addSaveData(SaveData save) {
      save.addEnum("limitMode", this.limitMode);
      if (this.minAmount != Integer.MAX_VALUE) {
         save.addInt("minAmount", this.minAmount);
      }

      if (this.maxAmount != Integer.MAX_VALUE) {
         save.addInt("maxAmount", this.maxAmount);
      }

      SaveData filter = new SaveData("categories");
      LinkedList<String> defaultAllowedItemStringIDs = new LinkedList<>();
      SaveData allowedItems = new SaveData("items");
      this.master.addSaveData(filter, defaultAllowedItemStringIDs, allowedItems);
      save.addSaveData(filter);
      save.addStringList("itemsAllowed", defaultAllowedItemStringIDs);
      if (!allowedItems.isEmpty()) {
         save.addSaveData(allowedItems);
      }
   }

   public void applyLoadData(LoadData save) {
      this.limitMode = save.getEnum(ItemCategoriesFilter.ItemLimitMode.class, "limitMode", this.limitMode, false);
      this.minAmount = save.getInt("minAmount", Integer.MAX_VALUE, 0, Integer.MAX_VALUE, false);
      this.maxAmount = save.getInt("maxAmount", Integer.MAX_VALUE, 0, Integer.MAX_VALUE, false);
      boolean allowUnfilteredItems = save.getBoolean("allowUnfilteredItems", true, false);
      if (!allowUnfilteredItems) {
         this.master.setAllowed(false);
      }

      LoadData categoriesSave = save.getFirstLoadDataByName("categories");
      if (categoriesSave != null) {
         this.master.applyLoadData(categoriesSave);
      }

      for (String itemStringID : save.getStringList("itemsAllowed", new LinkedList<>(), false)) {
         itemStringID = VersionMigration.tryFixStringID(itemStringID, VersionMigration.oldItemStringIDs);
         int itemID = ItemRegistry.getItemID(itemStringID);
         if (itemID != -1) {
            Item item = ItemRegistry.getItem(itemID);
            if (item != null && !this.isItemDisabled(item)) {
               this.setItemAllowed(item, true);
            }
         }
      }

      LoadData itemsSave = save.getFirstLoadDataByName("items");
      if (itemsSave != null) {
         for (LoadData itemSave : itemsSave.getLoadData()) {
            String itemStringID = itemSave.getUnsafeString("itemStringID", null);
            if (itemStringID != null) {
               itemStringID = VersionMigration.tryFixStringID(itemStringID, VersionMigration.oldItemStringIDs);
               int itemID = ItemRegistry.getItemID(itemStringID);
               if (itemID != -1) {
                  Item item = ItemRegistry.getItem(itemID);
                  if (item != null && !this.isItemDisabled(item)) {
                     ItemCategoriesFilter.ItemLimits limits = new ItemCategoriesFilter.ItemLimits();
                     limits.applyLoadData(itemSave);
                     this.setItemAllowed(item, limits);
                  }
               }
            }
         }
      }

      if (save.getFirstLoadDataByName("filter") == null && categoriesSave == null && !allowUnfilteredItems) {
         throw new LoadDataException("Missing categories");
      }

      for (LoadData filterSave : save.getLoadDataByName("filter")) {
         try {
            ItemFilter itemFilter = new ItemFilter(filterSave);
            Item item = ItemRegistry.getItem(itemFilter.itemID);
            if (item != null) {
               ItemCategoriesFilter.ItemCategoryFilter category = this.getItemCategory(item);
               category.setItemAllowed(item.getID(), itemFilter.maxAmount > 0, itemFilter.maxAmount);
            }
         } catch (LoadDataException e) {
            GameLog.warn.println("Could not load item filter: " + e.getMessage());
         }
      }

      this.master.fixAllowedVariablesChildren();
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextEnum(this.limitMode);
      boolean customAmounts = this.minAmount != Integer.MAX_VALUE || this.maxAmount != Integer.MAX_VALUE;
      writer.putNextBoolean(customAmounts);
      if (customAmounts) {
         writer.putNextInt(this.minAmount);
         writer.putNextInt(this.maxAmount);
      }

      this.master.writePacket(writer);
   }

   public void readPacket(PacketReader reader) {
      this.limitMode = reader.getNextEnum(ItemCategoriesFilter.ItemLimitMode.class);
      if (reader.getNextBoolean()) {
         this.minAmount = reader.getNextInt();
         this.maxAmount = reader.getNextInt();
      } else {
         this.minAmount = Integer.MAX_VALUE;
         this.maxAmount = Integer.MAX_VALUE;
      }

      this.master.readPacket(reader);
   }

   public boolean isEqualsFilter(ItemCategoriesFilter other) {
      if (other == this) {
         return true;
      } else if (this.limitMode != other.limitMode) {
         return false;
      } else if (this.maxAmount != other.maxAmount) {
         return false;
      } else {
         return this.minAmount != other.minAmount ? false : this.isEqualsFilter(this.master, other.master);
      }
   }

   private boolean isEqualsFilter(ItemCategoriesFilter.ItemCategoryFilter current, ItemCategoriesFilter.ItemCategoryFilter other) {
      if (current == other) {
         return true;
      }

      if (current.category != other.category) {
         return false;
      }

      if (current.maxItems != other.maxItems) {
         return false;
      }

      if (current.allChildrenAllowed != other.allChildrenAllowed) {
         return false;
      }

      if (current.allItemsAllowed != other.allItemsAllowed) {
         return false;
      }

      if (current.anyChildrenAllowed != other.anyChildrenAllowed) {
         return false;
      }

      if (current.anyItemsAllowed != other.anyItemsAllowed) {
         return false;
      }

      if (current.allChildrenDefault != other.allChildrenDefault) {
         return false;
      }

      if (current.allItemsDefault != other.allItemsDefault) {
         return false;
      }

      if (current.streamItems().anyMatch(i -> {
         ItemCategoriesFilter.ItemLimits currentLimits = current.itemIDsAllowed.get(i.getID());
         ItemCategoriesFilter.ItemLimits otherLimits = other.itemIDsAllowed.get(i.getID());
         if (currentLimits == otherLimits) {
            return false;
         } else {
            return currentLimits != null && otherLimits != null ? !currentLimits.isSame(otherLimits) : true;
         }
      })) {
         return false;
      }

      for (Entry<String, ItemCategoriesFilter.ItemCategoryFilter> e : current.children.entrySet()) {
         ItemCategoriesFilter.ItemCategoryFilter currentChild = e.getValue();
         ItemCategoriesFilter.ItemCategoryFilter otherChild = other.children.get(e.getKey());
         if (!this.isEqualsFilter(currentChild, otherChild)) {
            return false;
         }
      }

      return true;
   }

   public ItemCategoriesFilter copy() {
      ItemCategoriesFilter newFilter = new ItemCategoriesFilter(this.master.category, true);
      newFilter.loadFromCopy(this);
      return newFilter;
   }

   public boolean loadFromCopy(ItemCategoriesFilter copy) {
      boolean out = this.limitMode != copy.limitMode;
      this.limitMode = copy.limitMode;
      if (this.maxAmount != copy.maxAmount || this.minAmount != copy.minAmount) {
         out = true;
      }

      this.maxAmount = copy.maxAmount;
      this.minAmount = copy.minAmount;
      return this.applyFromCopyCategory(this.master, copy.master) || out;
   }

   private boolean applyFromCopyCategory(ItemCategoriesFilter.ItemCategoryFilter current, ItemCategoriesFilter.ItemCategoryFilter copy) {
      AtomicBoolean out = new AtomicBoolean();
      if (current.maxItems != copy.maxItems) {
         current.maxItems = copy.maxItems;
         out.set(true);
      }

      current.allChildrenAllowed = copy.allChildrenAllowed;
      current.anyChildrenAllowed = copy.anyChildrenAllowed;
      current.allItemsAllowed = copy.allItemsAllowed;
      current.anyItemsAllowed = copy.anyItemsAllowed;
      current.allChildrenDefault = copy.allChildrenDefault;
      current.allItemsDefault = copy.allItemsDefault;

      for (Entry<String, ItemCategoriesFilter.ItemCategoryFilter> entry : copy.children.entrySet()) {
         ItemCategoriesFilter.ItemCategoryFilter me = current.children.get(entry.getKey());
         ItemCategoriesFilter.ItemCategoryFilter him = entry.getValue();
         if (me != null && this.applyFromCopyCategory(me, him)) {
            out.set(true);
         }
      }

      current.streamItems().forEach(i -> {
         ItemCategoriesFilter.ItemLimits himx = copy.itemIDsAllowed.get(i.getID());
         if (current.setItemAllowed(i.getID(), himx)) {
            out.set(true);
         }
      });
      return out.get();
   }

   public boolean isItemDisabled(Item item) {
      return this.initializedCategories ? this.getItemCategory(item) == null : !ItemRegistry.isObtainable(item.getID());
   }

   public ItemCategoriesFilter.ItemCategoryFilter getItemCategory(Item item) {
      return this.itemIDCategories.get(item.getID());
   }

   public ItemCategoriesFilter.ItemCategoryFilter getItemCategory(int categoryID) {
      return this.categoryIDs.get(categoryID);
   }

   public ItemCategoriesFilter.ItemLimits getItemLimits(Item item) {
      if (this.isItemDisabled(item)) {
         return null;
      }

      ItemCategoriesFilter.ItemCategoryFilter category = this.getItemCategory(item);
      return category != null ? category.itemIDsAllowed.get(item.getID()) : null;
   }

   public boolean isItemAllowed(Item item) {
      return this.isItemDisabled(item) ? false : this.getItemLimits(item) != null;
   }

   public boolean setItemAllowed(Item item, boolean allowed) {
      return this.setItemAllowed(item, allowed, 0);
   }

   public boolean setItemAllowed(Item item, boolean allowed, int maxItems) {
      return this.setItemAllowed(item, allowed ? new ItemCategoriesFilter.ItemLimits(maxItems) : null);
   }

   public boolean setItemAllowed(Item item, ItemCategoriesFilter.ItemLimits limit) {
      if (this.isItemDisabled(item)) {
         return false;
      }

      ItemCategoriesFilter.ItemCategoryFilter category = this.getItemCategory(item);
      return category != null ? category.setItemAllowed(item.getID(), limit) : false;
   }

   @Deprecated
   public int getAddAmount(Level level, InventoryItem item, InventoryRange range) {
      return this.getAddAmount(level, item, range, false);
   }

   public int getAddAmount(Level level, InventoryItem item, InventoryRange range, boolean allowMaxAmount) {
      if (this.isItemDisabled(item.item)) {
         return 0;
      }

      ItemCategoriesFilter.ItemCategoryFilter category = this.getItemCategory(item.item);
      if (category == null) {
         return 0;
      }

      ItemCategoriesFilter.ItemLimits limits = category.itemIDsAllowed.get(item.item.getID());
      if (limits == null) {
         return 0;
      }

      LinkedList<ItemCategoriesFilter.LimitCounter> counters = new LinkedList<>();
      if (!limits.isDefault()) {
         counters.add(new ItemCategoriesFilter.TotalLimitCounter("limits", i -> i.item.getID() == item.item.getID(), limits.getMaxItems()));
      }

      for (; category != null; category = category.parent) {
         if (!category.isDefault()) {
            ItemCategoriesFilter.ItemCategoryFilter finalCategory = category;
            counters.addFirst(
               new ItemCategoriesFilter.TotalLimitCounter(
                  category.category.stringID, i -> finalCategory.category.containsItemOrInChildren(i.item), category.getMaxItems()
               )
            );
         }
      }

      ItemCategoriesFilter.LimitCounter maxAmountCounter = null;
      if (this.maxAmount != Integer.MAX_VALUE) {
         switch (this.limitMode) {
            case TOTAL_ITEMS:
               counters.addFirst(new ItemCategoriesFilter.TotalLimitCounter("maxTotal", i -> true, this.maxAmount));
               break;
            case TOTAL_STACKS:
               counters.addFirst(new ItemCategoriesFilter.StackLimitCounter("maxTotalStacks", i -> true, item, this.maxAmount));
               break;
            case TOTAL_EACH_ITEM:
               maxAmountCounter = new ItemCategoriesFilter.TotalLimitCounter("maxTotalEach", i -> i.item.getID() == item.item.getID(), this.maxAmount);
               break;
            case TOTAL_STACKS_EACH_ITEM:
               maxAmountCounter = new ItemCategoriesFilter.StackLimitCounter(
                  "maxTotalStacksEach", i -> i.item.getID() == item.item.getID(), item, this.maxAmount
               );
         }
      }

      int fullAmount = 0;
      int lowestMaxAdd = item.getAmount();
      if (this.minAmount != Integer.MAX_VALUE || !counters.isEmpty() || maxAmountCounter != null) {
         for (int slot = range.startSlot; slot <= range.endSlot; slot++) {
            InventoryItem slotItem = range.inventory.getItem(slot);
            if (slotItem != null) {
               fullAmount += slotItem.getAmount();
               if (fullAmount >= this.minAmount && !allowMaxAmount) {
                  return 0;
               }

               for (ItemCategoriesFilter.LimitCounter counter : counters) {
                  if (!counter.shouldHandleItem(level, range.inventory, slot, slotItem)) {
                     break;
                  }

                  int counterAddAmount = counter.handleAndGetAddAmount(level, range.inventory, slot, slotItem);
                  if (counterAddAmount <= 0) {
                     return 0;
                  }

                  lowestMaxAdd = Math.min(counterAddAmount, lowestMaxAdd);
               }

               if (maxAmountCounter != null && maxAmountCounter.shouldHandleItem(level, range.inventory, slot, slotItem)) {
                  int counterAddAmount = maxAmountCounter.handleAndGetAddAmount(level, range.inventory, slot, slotItem);
                  if (counterAddAmount <= 0) {
                     return 0;
                  }

                  lowestMaxAdd = Math.min(counterAddAmount, lowestMaxAdd);
               }
            }
         }

         for (ItemCategoriesFilter.LimitCounter counter : counters) {
            int counterAddAmount = counter.getFinalAddAmount(level, range);
            if (counterAddAmount <= 0) {
               return 0;
            }

            lowestMaxAdd = Math.min(counterAddAmount, lowestMaxAdd);
         }

         if (maxAmountCounter != null) {
            int counterAddAmount = maxAmountCounter.getFinalAddAmount(level, range);
            if (counterAddAmount <= 0) {
               return 0;
            }

            lowestMaxAdd = Math.min(counterAddAmount, lowestMaxAdd);
         }
      }

      return lowestMaxAdd;
   }

   public int getRemoveAmount(Level level, InventoryItem item, InventoryRange range) {
      if (this.isItemDisabled(item.item)) {
         return item.getAmount();
      }

      ItemCategoriesFilter.ItemCategoryFilter category = this.getItemCategory(item.item);
      if (category == null) {
         return 0;
      }

      ItemCategoriesFilter.ItemLimits limits = category.itemIDsAllowed.get(item.item.getID());
      if (limits == null) {
         return item.getAmount();
      }

      LinkedList<ItemCategoriesFilter.LimitCounter> counters = new LinkedList<>();
      if (!limits.isDefault()) {
         counters.add(new ItemCategoriesFilter.TotalLimitCounter("limits", i -> i.item.getID() == item.item.getID(), limits.getMaxItems()));
      }

      for (; category != null; category = category.parent) {
         if (!category.isDefault()) {
            ItemCategoriesFilter.ItemCategoryFilter finalCategory = category;
            counters.addFirst(
               new ItemCategoriesFilter.TotalLimitCounter(
                  category.category.stringID, i -> finalCategory.category.containsItemOrInChildren(i.item), category.getMaxItems()
               )
            );
         }
      }

      ItemCategoriesFilter.LimitCounter maxAmountCounter = null;
      if (this.maxAmount != Integer.MAX_VALUE) {
         switch (this.limitMode) {
            case TOTAL_ITEMS:
               counters.addFirst(new ItemCategoriesFilter.TotalLimitCounter("maxTotal", i -> true, this.maxAmount));
               break;
            case TOTAL_STACKS:
               counters.addFirst(new ItemCategoriesFilter.StackLimitCounter("maxTotalStacks", i -> true, item, this.maxAmount));
               break;
            case TOTAL_EACH_ITEM:
               maxAmountCounter = new ItemCategoriesFilter.TotalLimitCounter("maxTotalEach", i -> i.item.getID() == item.item.getID(), this.maxAmount);
               break;
            case TOTAL_STACKS_EACH_ITEM:
               maxAmountCounter = new ItemCategoriesFilter.StackLimitCounter(
                  "maxTotalStacksEach", i -> i.item.getID() == item.item.getID(), item, this.maxAmount
               );
         }
      }

      int highestRemove = 0;
      if (!counters.isEmpty() || maxAmountCounter != null) {
         for (int slot = range.startSlot; slot <= range.endSlot; slot++) {
            InventoryItem slotItem = range.inventory.getItem(slot);
            if (slotItem != null) {
               for (ItemCategoriesFilter.LimitCounter counter : counters) {
                  if (!counter.shouldHandleItem(level, range.inventory, slot, slotItem)) {
                     break;
                  }

                  int counterRemoveAmount = counter.handleAndGetRemoveAmount(level, range.inventory, slot, slotItem);
                  int minRemove = Math.min(counterRemoveAmount, item.getAmount());
                  highestRemove = Math.max(minRemove, highestRemove);
               }

               if (maxAmountCounter != null && maxAmountCounter.shouldHandleItem(level, range.inventory, slot, slotItem)) {
                  int counterRemoveAmount = maxAmountCounter.handleAndGetRemoveAmount(level, range.inventory, slot, slotItem);
                  int minRemove = Math.min(counterRemoveAmount, item.getAmount());
                  highestRemove = Math.max(minRemove, highestRemove);
               }
            }
         }
      }

      return highestRemove;
   }

   public boolean matchesItem(InventoryItem item) {
      return this.isItemAllowed(item.item);
   }

   public class ItemCategoryFilter implements Comparable<ItemCategoriesFilter.ItemCategoryFilter> {
      public final ItemCategory category;
      public final ItemCategoriesFilter.ItemCategoryFilter parent;
      public final boolean hasAnyItems;
      private int maxItems = Integer.MAX_VALUE;
      private final HashMap<String, ItemCategoriesFilter.ItemCategoryFilter> children = new HashMap<>();
      private boolean allItemsAllowed;
      private boolean allChildrenAllowed;
      private boolean anyItemsAllowed;
      private boolean anyChildrenAllowed;
      private boolean allItemsDefault;
      private boolean allChildrenDefault;
      private final HashMap<Integer, ItemCategoriesFilter.ItemLimits> itemIDsAllowed = new HashMap<>();

      public ItemCategoryFilter(ItemCategory category, boolean allowAll, ItemCategoriesFilter.ItemCategoryFilter parent) {
         this.parent = parent;
         this.category = category;
         ItemCategoriesFilter.this.categoryIDs.put(category.id, this);
         this.hasAnyItems = this.streamItems().findAny().isPresent();
         this.allItemsAllowed = allowAll || !this.hasAnyItems;
         this.anyItemsAllowed = allowAll && this.hasAnyItems;
         this.allItemsDefault = allowAll || !this.hasAnyItems;
         if (this.anyItemsAllowed) {
            for (ItemCategoriesFilter.ItemCategoryFilter current = parent; current != null && !current.anyChildrenAllowed; current = current.parent) {
               current.anyChildrenAllowed = true;
            }
         }

         this.anyChildrenAllowed = false;

         for (ItemCategory child : category.getChildren()) {
            this.children.put(child.stringID, ItemCategoriesFilter.this.new ItemCategoryFilter(child, allowAll, this));
         }

         this.allChildrenAllowed = allowAll || this.children.isEmpty();
         this.allChildrenDefault = allowAll || this.children.isEmpty();
         this.streamItems().forEach(item -> {
            ItemCategoriesFilter.this.itemIDCategories.put(item.getID(), this);
            if (allowAll) {
               this.itemIDsAllowed.put(item.getID(), new ItemCategoriesFilter.ItemLimits());
            }
         });
      }

      public void addSaveData(SaveData save, List<String> defaultAllowedItemStringIDs, SaveData allowedItemsSave) {
         if (!this.isDefault()) {
            save.addInt("maxItems", this.maxItems);
         }

         save.addBoolean("allChildrenAllowed", this.allChildrenAllowed && !this.children.isEmpty());
         save.addBoolean("allChildrenDefault", this.allChildrenDefault);
         if ((!this.allChildrenAllowed || !this.allChildrenDefault) && !this.children.isEmpty()) {
            SaveData childrenSave = new SaveData("children");

            for (Entry<String, ItemCategoriesFilter.ItemCategoryFilter> entry : this.children.entrySet()) {
               SaveData childSave = new SaveData(entry.getKey());
               entry.getValue().addSaveData(childSave, defaultAllowedItemStringIDs, allowedItemsSave);
               childrenSave.addSaveData(childSave);
            }

            save.addSaveData(childrenSave);
         }

         save.addBoolean("allItemsAllowed", this.allItemsAllowed && this.hasAnyItems);
         save.addBoolean("allItemsDefault", this.allItemsDefault);
         if (!this.allItemsAllowed || !this.allItemsDefault) {
            for (Entry<Integer, ItemCategoriesFilter.ItemLimits> entry : this.itemIDsAllowed.entrySet()) {
               Item item = ItemRegistry.getItem(entry.getKey());
               if (item != null && !ItemCategoriesFilter.this.isItemDisabled(item)) {
                  if (entry.getValue().isDefault()) {
                     defaultAllowedItemStringIDs.add(item.getStringID());
                  } else {
                     SaveData itemSave = new SaveData("item");
                     itemSave.addUnsafeString("itemStringID", item.getStringID());
                     entry.getValue().addSaveData(itemSave);
                     allowedItemsSave.addSaveData(itemSave);
                  }
               }
            }
         }
      }

      public void applyLoadData(LoadData save) {
         this.allChildrenAllowed = save.getBoolean("allChildrenAllowed", this.allChildrenAllowed);
         if (this.allChildrenAllowed) {
            this.setAllowed(true);
         }

         this.allChildrenDefault = save.getBoolean("allChildrenDefault", true, false);
         if (!this.children.isEmpty() && !this.allChildrenAllowed || !this.allChildrenDefault) {
            LoadData childrenSave = save.getFirstLoadDataByName("children");
            if (childrenSave != null) {
               for (Entry<String, ItemCategoriesFilter.ItemCategoryFilter> entry : this.children.entrySet()) {
                  LoadData childSave = childrenSave.getFirstLoadDataByName(entry.getKey());
                  if (childSave != null && childSave.isArray()) {
                     entry.getValue().applyLoadData(childSave);
                  } else {
                     entry.getValue().setAllowed(false);
                  }
               }
            }
         }

         this.maxItems = save.getInt("maxItems", Integer.MAX_VALUE, false);
         this.fixAnyChildrenAllowed();
         this.allItemsAllowed = save.getBoolean("allItemsAllowed", this.allItemsAllowed);
         this.allItemsDefault = save.getBoolean("allItemsDefault", true, false);
         if (this.allItemsAllowed) {
            this.anyItemsAllowed = this.hasAnyItems;
            this.streamItems().forEach(i -> this.itemIDsAllowed.put(i.getID(), new ItemCategoriesFilter.ItemLimits()));
         } else {
            this.anyItemsAllowed = false;
            this.streamItems().forEach(i -> this.itemIDsAllowed.remove(i.getID()));
         }
      }

      public void writePacket(PacketWriter writer) {
         writer.putNextBoolean(this.isDefault());
         if (!this.isDefault()) {
            writer.putNextInt(this.maxItems);
         }

         writer.putNextBoolean(this.allChildrenAllowed);
         writer.putNextBoolean(this.allChildrenDefault);
         if (!this.allChildrenAllowed || !this.allChildrenDefault) {
            writer.putNextBoolean(this.anyChildrenAllowed);
            if (this.anyChildrenAllowed) {
               for (ItemCategoriesFilter.ItemCategoryFilter child : this.getChildrenTree()) {
                  child.writePacket(writer);
               }
            }
         }

         writer.putNextBoolean(this.allItemsAllowed);
         writer.putNextBoolean(this.allItemsDefault);
         if (!this.allItemsAllowed || !this.allItemsDefault) {
            writer.putNextBoolean(this.anyItemsAllowed);
            if (this.anyItemsAllowed) {
               for (Item item : this.getItemsTree()) {
                  ItemCategoriesFilter.ItemLimits limits = this.itemIDsAllowed.get(item.getID());
                  writer.putNextBoolean(limits != null);
                  if (limits != null) {
                     limits.writePacket(writer);
                  }
               }
            }
         }
      }

      public void readPacket(PacketReader reader) {
         boolean isDefault = reader.getNextBoolean();
         if (isDefault) {
            this.clearMaxItems();
         } else {
            this.maxItems = reader.getNextInt();
         }

         this.allChildrenAllowed = reader.getNextBoolean();
         this.allChildrenDefault = reader.getNextBoolean();
         if (this.allChildrenAllowed && this.allChildrenDefault) {
            for (ItemCategoriesFilter.ItemCategoryFilter child : this.getChildren()) {
               child.setAllowed(true, false);
            }

            this.fixAnyChildrenAllowed();
         } else {
            this.anyChildrenAllowed = reader.getNextBoolean();
            if (this.anyChildrenAllowed) {
               for (ItemCategoriesFilter.ItemCategoryFilter child : this.getChildrenTree()) {
                  child.readPacket(reader);
               }
            } else {
               for (ItemCategoriesFilter.ItemCategoryFilter child : this.getChildren()) {
                  child.setAllowed(false, false);
               }
            }
         }

         this.allItemsAllowed = reader.getNextBoolean();
         this.allItemsDefault = reader.getNextBoolean();
         if (this.allItemsAllowed && this.allItemsDefault) {
            this.anyItemsAllowed = this.hasAnyItems;
            this.streamItems().forEach(itemx -> this.itemIDsAllowed.put(itemx.getID(), new ItemCategoriesFilter.ItemLimits()));
         } else {
            this.anyItemsAllowed = reader.getNextBoolean();
            if (this.anyItemsAllowed) {
               for (Item item : this.getItemsTree()) {
                  boolean allowed = reader.getNextBoolean();
                  if (allowed) {
                     ItemCategoriesFilter.ItemLimits limits = new ItemCategoriesFilter.ItemLimits();
                     limits.readPacket(reader);
                     this.itemIDsAllowed.put(item.getID(), limits);
                  } else {
                     this.itemIDsAllowed.remove(item.getID());
                  }
               }
            } else {
               this.streamItems().forEach(itemx -> this.itemIDsAllowed.remove(itemx.getID()));
            }
         }
      }

      public void fixAllowedVariablesChildren() {
         this.fixAllowedVariables(true);

         for (ItemCategoriesFilter.ItemCategoryFilter child : this.children.values()) {
            child.fixAllowedVariablesChildren();
         }
      }

      protected void fixAllChildrenAllowed() {
         if (!this.children.isEmpty()) {
            this.allChildrenAllowed = this.children.values().stream().allMatch(c -> c.allItemsAllowed && c.allChildrenAllowed);
         } else {
            this.allChildrenAllowed = true;
         }
      }

      protected void fixAnyChildrenAllowed() {
         if (this.children.isEmpty()) {
            this.anyChildrenAllowed = false;
         } else {
            this.anyChildrenAllowed = this.children.values().stream().anyMatch(c -> c.anyChildrenAllowed || c.anyItemsAllowed);
         }
      }

      protected void fixAllChildrenDefault() {
         if (!this.children.isEmpty()) {
            this.allChildrenDefault = this.children.values().stream().allMatch(c -> c.allItemsDefault && c.allChildrenDefault && c.isDefault());
         } else {
            this.allChildrenDefault = true;
         }
      }

      public void fixAllowedVariables(boolean updateParents) {
         this.fixAllChildrenAllowed();
         this.fixAnyChildrenAllowed();
         this.allItemsAllowed = true;
         this.anyItemsAllowed = false;
         this.fixAllChildrenDefault();
         this.allItemsDefault = true;
         this.streamItems().forEach(i -> {
            ItemCategoriesFilter.ItemLimits limits = this.itemIDsAllowed.get(i.getID());
            if (limits != null) {
               this.anyItemsAllowed = true;
               if (!limits.isDefault()) {
                  this.allItemsDefault = false;
               }
            } else {
               this.allItemsAllowed = false;
               this.allItemsDefault = false;
            }
         });
         if (updateParents) {
            for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
               current.fixAllowedVariables(updateParents);
            }
         }
      }

      public boolean isAllAllowed() {
         return this.allItemsAllowed && this.allChildrenAllowed;
      }

      public boolean isAnyAllowed() {
         return this.anyItemsAllowed || this.anyChildrenAllowed;
      }

      public boolean isAllDefault() {
         return this.allItemsDefault && this.allChildrenDefault;
      }

      public boolean isDefault() {
         return this.maxItems == Integer.MAX_VALUE;
      }

      public int getMaxItems() {
         return this.maxItems;
      }

      public void clearMaxItems() {
         this.maxItems = Integer.MAX_VALUE;
         if (this.allChildrenDefault && this.allItemsDefault) {
            for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
               current.fixAllChildrenDefault();
            }
         }
      }

      public void setMaxItems(int maxItems) {
         if (!this.isAnyAllowed()) {
            this.setAllowed(true);
         }

         this.maxItems = maxItems <= 0 ? Integer.MAX_VALUE : maxItems;
         if (this.allChildrenDefault && this.allItemsDefault) {
            for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
               current.fixAllChildrenDefault();
            }
         }
      }

      public void setAllowed(boolean allowed) {
         this.setAllowed(allowed, true);
      }

      protected void setAllowed(boolean allowed, boolean updateParents) {
         if (allowed) {
            this.maxItems = Integer.MAX_VALUE;
            this.allItemsAllowed = true;
            this.allChildrenAllowed = true;
            this.anyItemsAllowed = this.hasAnyItems;
            this.anyChildrenAllowed = false;
            this.allChildrenDefault = true;
            this.allItemsDefault = true;
            if (this.anyItemsAllowed) {
               for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null && !current.anyChildrenAllowed; current = current.parent) {
                  current.anyChildrenAllowed = true;
               }
            }

            for (ItemCategoriesFilter.ItemCategoryFilter child : this.children.values()) {
               child.setAllowed(true, false);
            }

            this.streamItems().forEach(item -> this.itemIDsAllowed.put(item.getID(), new ItemCategoriesFilter.ItemLimits()));
            if (updateParents) {
               for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                  current.fixAllChildrenAllowed();
                  current.fixAllChildrenDefault();
               }
            }
         } else {
            this.maxItems = Integer.MAX_VALUE;
            this.itemIDsAllowed.clear();
            this.allItemsAllowed = !this.hasAnyItems;
            this.allChildrenAllowed = this.children.isEmpty();
            this.anyItemsAllowed = false;
            this.anyChildrenAllowed = false;
            this.allChildrenDefault = this.children.isEmpty();
            this.allItemsDefault = !this.hasAnyItems;

            for (ItemCategoriesFilter.ItemCategoryFilter child : this.children.values()) {
               child.setAllowed(false, false);
            }

            if (updateParents) {
               for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                  current.allChildrenAllowed = current.children.isEmpty();
                  current.fixAnyChildrenAllowed();
                  current.allChildrenDefault = current.children.isEmpty();
               }
            }
         }
      }

      public boolean setItemAllowed(int itemID, boolean allowed) {
         return this.setItemAllowed(itemID, allowed, 0);
      }

      public boolean setItemAllowed(int itemID, boolean allowed, int maxItems) {
         return this.setItemAllowed(itemID, allowed ? new ItemCategoriesFilter.ItemLimits(maxItems) : null);
      }

      public boolean setItemAllowed(int itemID, ItemCategoriesFilter.ItemLimits limit) {
         Item item = ItemRegistry.getItem(itemID);
         if (item != null && this.category.containsItem(item) && !ItemCategoriesFilter.this.isItemDisabled(item)) {
            ItemCategoriesFilter.ItemLimits prev = this.itemIDsAllowed.get(itemID);
            if (limit != prev && (limit == null || prev == null || !limit.isSame(prev))) {
               if (limit != null) {
                  this.itemIDsAllowed.put(itemID, limit);
                  this.anyItemsAllowed = true;
                  this.allItemsAllowed = this.streamItems().allMatch(i -> this.itemIDsAllowed.containsKey(i.getID()));
                  if (this.allItemsAllowed && this.allChildrenAllowed) {
                     for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                        current.fixAllChildrenAllowed();
                        if (!current.allChildrenAllowed) {
                           break;
                        }
                     }
                  }

                  if (limit.isDefault()) {
                     this.allItemsDefault = this.streamItems().allMatch(i -> {
                        ItemCategoriesFilter.ItemLimits itemLimits = this.itemIDsAllowed.get(i.getID());
                        return itemLimits != null && itemLimits.isDefault();
                     });
                     if (this.allItemsDefault && this.allChildrenDefault) {
                        for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                           current.fixAllChildrenDefault();
                        }
                     }
                  } else {
                     this.allItemsDefault = false;

                     for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                        current.fixAllChildrenDefault();
                     }
                  }

                  for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                     current.anyChildrenAllowed = true;
                  }
               } else {
                  this.itemIDsAllowed.remove(itemID);
                  this.allItemsAllowed = false;
                  this.anyItemsAllowed = this.streamItems().anyMatch(i -> this.itemIDsAllowed.containsKey(i.getID()));
                  this.allItemsDefault = false;
                  this.fixAnyChildrenAllowed();

                  for (ItemCategoriesFilter.ItemCategoryFilter current = this.parent; current != null; current = current.parent) {
                     current.allChildrenAllowed = false;
                     current.fixAnyChildrenAllowed();
                  }
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      public Collection<ItemCategoriesFilter.ItemCategoryFilter> getChildren() {
         return this.children.values();
      }

      public TreeSet<ItemCategoriesFilter.ItemCategoryFilter> getChildrenTree() {
         return this.children.values().stream().collect(() -> new TreeSet<>(Comparator.comparingInt(c -> c.category.id)), TreeSet::add, TreeSet::addAll);
      }

      public Stream<Item> streamItems() {
         return this.category.streamItems().filter(i -> !ItemCategoriesFilter.this.isItemDisabled(i));
      }

      public TreeSet<Item> getItemsTree() {
         return this.streamItems().collect(() -> new TreeSet<>(Comparator.comparingInt(Item::getID)), TreeSet::add, TreeSet::addAll);
      }

      public int compareTo(ItemCategoriesFilter.ItemCategoryFilter o) {
         return this.category.compareTo(o.category);
      }

      public GameTooltips getDebugTooltip() {
         ListGameTooltips tooltips = new ListGameTooltips();
         tooltips.add("HasAnyItems: " + this.hasAnyItems);
         tooltips.add("allItemsAllowed: " + this.allItemsAllowed);
         tooltips.add("allChildrenAllowed: " + this.allChildrenAllowed);
         tooltips.add("anyItemsAllowed: " + this.anyItemsAllowed);
         tooltips.add("anyChildrenAllowed: " + this.anyChildrenAllowed);
         tooltips.add("allChildrenDefault: " + this.allChildrenDefault);
         tooltips.add("allItemsDefault: " + this.allItemsDefault);
         return tooltips;
      }
   }

   public enum ItemLimitMode {
      TOTAL_ITEMS(new LocalMessage("ui", "storagelimittotal"), null, new LocalMessage("ui", "storagelimit")),
      TOTAL_STACKS(new LocalMessage("ui", "storagelimittotalstacks"), null, new LocalMessage("ui", "storagelimitstacks")),
      TOTAL_EACH_ITEM(new LocalMessage("ui", "storagelimitperitem"), null, new LocalMessage("ui", "storagelimit")),
      TOTAL_STACKS_EACH_ITEM(new LocalMessage("ui", "storagelimitperitemstacks"), null, new LocalMessage("ui", "storagelimitstacks"));

      public GameMessage displayName;
      public GameMessage tooltip;
      public GameMessage inputPlaceholder;

      ItemLimitMode(GameMessage displayName, GameMessage tooltip, GameMessage inputPlaceholder) {
         this.displayName = displayName;
         this.tooltip = tooltip;
         this.inputPlaceholder = inputPlaceholder;
      }
   }

   public static class ItemLimits {
      private int maxItems;

      public ItemLimits() {
         this.maxItems = Integer.MAX_VALUE;
      }

      public ItemLimits(int maxItems) {
         this.maxItems = maxItems <= 0 ? Integer.MAX_VALUE : maxItems;
      }

      public void addSaveData(SaveData save) {
         save.addInt("maxItems", this.maxItems);
      }

      public void applyLoadData(LoadData save) {
         this.maxItems = save.getInt("maxItems", Integer.MAX_VALUE);
         if (this.maxItems <= 0) {
            this.maxItems = Integer.MAX_VALUE;
         }
      }

      public void writePacket(PacketWriter writer) {
         boolean isDefault = this.isDefault();
         writer.putNextBoolean(isDefault);
         if (!isDefault) {
            writer.putNextInt(this.maxItems);
         }
      }

      public void readPacket(PacketReader reader) {
         boolean isDefault = reader.getNextBoolean();
         if (isDefault) {
            this.maxItems = Integer.MAX_VALUE;
         } else {
            this.maxItems = reader.getNextInt();
         }
      }

      public boolean isDefault() {
         return this.maxItems == Integer.MAX_VALUE;
      }

      public int getMaxItems() {
         return this.maxItems;
      }

      public boolean isSame(ItemCategoriesFilter.ItemLimits other) {
         return other.maxItems == this.maxItems;
      }
   }

   private abstract static class LimitCounter {
      public final String debugName;

      public LimitCounter(String debugName) {
         this.debugName = debugName;
      }

      public abstract boolean shouldHandleItem(Level var1, Inventory var2, int var3, InventoryItem var4);

      public abstract int handleAndGetAddAmount(Level var1, Inventory var2, int var3, InventoryItem var4);

      public abstract int getFinalAddAmount(Level var1, InventoryRange var2);

      public abstract int handleAndGetRemoveAmount(Level var1, Inventory var2, int var3, InventoryItem var4);
   }

   private static class StackLimitCounter extends ItemCategoriesFilter.LimitCounter {
      public final Predicate<InventoryItem> isValidItem;
      public final int maxStacks;
      public final InventoryItem inputItem;
      public int stacks;
      public int inputItemsAddedOrRemoved;

      public StackLimitCounter(String debugName, Predicate<InventoryItem> isValidItem, InventoryItem inputItem, int maxStacks) {
         super(debugName);
         this.isValidItem = isValidItem;
         this.maxStacks = maxStacks;
         this.inputItem = inputItem;
      }

      @Override
      public boolean shouldHandleItem(Level level, Inventory inventory, int slot, InventoryItem slotItem) {
         return this.isValidItem.test(slotItem);
      }

      @Override
      public int handleAndGetAddAmount(Level level, Inventory inventory, int slot, InventoryItem slotItem) {
         if (this.stacks < this.maxStacks && slotItem.canCombine(level, null, this.inputItem, "hauljob")) {
            int stackLimit = inventory.getItemStackLimit(slot, slotItem);
            int possibleAdds = stackLimit - slotItem.getAmount();
            if (possibleAdds > 0) {
               this.inputItemsAddedOrRemoved += possibleAdds;
            }
         }

         this.stacks++;
         return Integer.MAX_VALUE;
      }

      @Override
      public int getFinalAddAmount(Level level, InventoryRange range) {
         if (this.stacks < this.maxStacks) {
            for (int slot = range.startSlot; slot <= range.endSlot; slot++) {
               if (range.inventory.isSlotClear(slot)) {
                  this.inputItemsAddedOrRemoved = this.inputItemsAddedOrRemoved + range.inventory.getItemStackLimit(slot, this.inputItem);
                  this.stacks++;
                  if (this.stacks >= this.maxStacks) {
                     break;
                  }
               }
            }
         }

         return this.inputItemsAddedOrRemoved;
      }

      @Override
      public int handleAndGetRemoveAmount(Level level, Inventory inventory, int slot, InventoryItem slotItem) {
         this.stacks++;
         return this.stacks > this.maxStacks ? slotItem.getAmount() : 0;
      }
   }

   private static class TotalLimitCounter extends ItemCategoriesFilter.LimitCounter {
      public final Predicate<InventoryItem> isValidItem;
      public final int maxItems;
      public int items;

      public TotalLimitCounter(String debugName, Predicate<InventoryItem> isValidItem, int maxItems) {
         super(debugName);
         this.isValidItem = isValidItem;
         this.maxItems = maxItems;
      }

      @Override
      public boolean shouldHandleItem(Level level, Inventory inventory, int slot, InventoryItem slotItem) {
         return this.isValidItem.test(slotItem);
      }

      @Override
      public int handleAndGetAddAmount(Level level, Inventory inventory, int slot, InventoryItem slotItem) {
         this.items = this.items + slotItem.getAmount();
         return this.maxItems - this.items;
      }

      @Override
      public int getFinalAddAmount(Level level, InventoryRange range) {
         return this.maxItems - this.items;
      }

      @Override
      public int handleAndGetRemoveAmount(Level level, Inventory inventory, int slot, InventoryItem slotItem) {
         this.items = this.items + slotItem.getAmount();
         return this.items - this.maxItems;
      }
   }
}
