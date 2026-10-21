package necesse.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.GameState;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.world.GameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.Entity;
import necesse.entity.TileEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.item.Item;
import necesse.inventory.item.TickItem;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.IngredientCounter;
import necesse.inventory.recipe.IngredientStep;
import necesse.inventory.recipe.IngredientUser;
import necesse.level.maps.Level;

public class Inventory {
   private int size;
   private InventoryItem[] items;
   private final HashSet<Integer> tickSlots = new HashSet<>();
   private float lastSpoilRateModifier = 1.0F;
   private final Inventory.NextSpoilSlotList nextSpoilSlots = new Inventory.NextSpoilSlotList();
   private int totalDirty;
   private boolean[] dirtySlots;
   public InventoryFilter filter;
   public float spoilRateModifier = 1.0F;
   private final GameLinkedList<InventoryUpdateListener> slotUpdateListeners = new GameLinkedList<>();

   public Inventory(int size) {
      this.size = size;
      this.items = new InventoryItem[size];
      this.dirtySlots = new boolean[size];
   }

   public Inventory copy() {
      Inventory inventory = new Inventory(this.size);

      for (int i = 0; i < this.size; i++) {
         inventory.items[i] = this.items[i] == null ? null : this.items[i].copy();
         inventory.dirtySlots[i] = this.dirtySlots[i];
      }

      inventory.totalDirty = this.totalDirty;
      inventory.filter = this.filter;
      return inventory;
   }

   public Inventory copy(final int startSlot, int endSlot) {
      Inventory inventory = new Inventory(endSlot - startSlot + 1);
      int totalDirty = 0;

      for (int i = startSlot; i < endSlot; i++) {
         inventory.items[i - startSlot] = this.items[i] == null ? null : this.items[i].copy();
         if (this.dirtySlots[i]) {
            inventory.dirtySlots[i - startSlot] = true;
            totalDirty++;
         }
      }

      inventory.totalDirty = totalDirty;
      final InventoryFilter filter = this.filter;
      inventory.filter = new InventoryFilter() {
         @Override
         public boolean isItemValid(int slot, InventoryItem item) {
            return filter.isItemValid(slot + startSlot, item);
         }

         @Override
         public int getItemStackLimit(int slot, InventoryItem item) {
            return filter.getItemStackLimit(slot + startSlot, item);
         }
      };
      return inventory;
   }

   public void changeSize(int size) {
      if (size != this.getSize()) {
         InventoryItem[] newItems = new InventoryItem[size];
         boolean[] newDirty = new boolean[size];
         this.totalDirty = 0;

         for (int i = 0; i < this.items.length && i < newItems.length; i++) {
            newItems[i] = this.items[i];
            newDirty[i] = this.dirtySlots[i];
            if (this.dirtySlots[i]) {
               this.totalDirty++;
            }
         }

         this.items = newItems;
         this.dirtySlots = newDirty;
         this.size = size;
      }
   }

   public void override(Inventory newInventory) {
      this.override(newInventory, false, true);
   }

   public void override(Inventory newInventory, boolean overrideSize, boolean overrideIsNew) {
      if (overrideSize && this.getSize() != newInventory.getSize()) {
         this.changeSize(newInventory.getSize());
      }

      for (int i = 0; i < this.getSize() && newInventory.getSize() > i; i++) {
         this.setItem(i, newInventory.getItem(i), overrideIsNew);
      }
   }

   public boolean adjustSize(int minSize, int maxSize, int emptySpaces) {
      if (maxSize < 0) {
         maxSize = Integer.MAX_VALUE;
      }

      if (maxSize < minSize) {
         maxSize = minSize;
      }

      int currentEmptySpaces = 0;
      int maxItemIndex = 0;

      for (int i = 0; i < this.getSize(); i++) {
         if (this.isSlotClear(i)) {
            currentEmptySpaces++;
         } else {
            maxItemIndex = i;
         }
      }

      minSize = Math.max(minSize, maxItemIndex + 1);
      maxSize = Math.max(maxSize, maxItemIndex + 1);
      int nextSize = minSize;
      if (currentEmptySpaces < emptySpaces) {
         nextSize = Math.min(this.getSize() + emptySpaces - currentEmptySpaces, maxSize);
      } else {
         int canRemove = currentEmptySpaces - emptySpaces;
         nextSize = Math.min(Math.max(nextSize, this.getSize() - canRemove), maxSize);
      }

      if (nextSize != this.getSize()) {
         this.changeSize(nextSize);
         return true;
      } else {
         return false;
      }
   }

   public int getSize() {
      return this.size;
   }

   public int getUsedSlots() {
      return (int)Arrays.stream(this.items).filter(Objects::nonNull).count();
   }

   public float getFillPercentage() {
      return GameMath.limit((float)this.getUsedSlots() / this.getSize(), 0.0F, 1.0F);
   }

   public void tickItems(Entity entity) {
      this.tickItems(entity, entity, entity, null, entity == null ? null : entity.getWorldSettings());
   }

   public void tickItems(TileEntity tileEntity) {
      this.tickItems(tileEntity, tileEntity, null, tileEntity, tileEntity == null ? null : tileEntity.getWorldSettings());
   }

   public void tickItems(GameClock clock, GameState state, Entity entity, TileEntity tileEntity, WorldSettings worldSettings) {
      LinkedList<Integer> tickSlotRemoves = new LinkedList<>();
      Integer[] clockWorldTime = this.tickSlots.toArray(new Integer[0]);
      int slot = clockWorldTime.length;

      for (int current = 0; current < slot; current++) {
         int slotx = clockWorldTime[current];
         InventoryItem item = this.getItem(slotx);
         if (item != null && item.item.isTickItem()) {
            ((TickItem)item.item).tick(this, slotx, item, clock, state, entity, tileEntity, worldSettings, newItem -> this.setItem(slot, newItem));
         } else {
            tickSlotRemoves.add(slotx);
         }
      }

      for (int slotx : tickSlotRemoves) {
         if (this.tickSlots.remove(slotx)) {
            GameLog.debug.println("Had to remove slot " + slotx + " from tickSlots: " + this.getItem(slotx) + ", " + (entity == null ? tileEntity : entity));
         }
      }

      if ((worldSettings == null || worldSettings.survivalMode) && !this.nextSpoilSlots.isEmpty()) {
         if (this.spoilRateModifier != this.lastSpoilRateModifier) {
            this.lastSpoilRateModifier = this.spoilRateModifier;
            ArrayList<Integer> slotsToHandle = new ArrayList<>(this.nextSpoilSlots.size());

            for (Inventory.SlotSpoilTime slotSpoilTime : this.nextSpoilSlots) {
               slotsToHandle.add(slotSpoilTime.slot);
            }

            this.nextSpoilSlots.clear();

            for (int slotx : slotsToHandle) {
               InventoryItem item = this.getItem(slotx);
               if (item != null && item.item.shouldSpoilTick(item)) {
                  long nextWorldTickTime = item.item.tickSpoilTime(item, clock, this.spoilRateModifier, newItem -> this.setItem(slot, newItem));
                  this.addSpoilSlotSorted(slotx, nextWorldTickTime);
               }
            }
         } else {
            long clockWorldTimex = clock == null ? 0L : clock.getWorldTime();
            GameLinkedList<Inventory.SlotSpoilTime>.Element current = this.nextSpoilSlots.getFirstElement();

            while (current != null) {
               int currentSlot = ((Inventory.SlotSpoilTime)current.object).slot;
               GameLinkedList<Inventory.SlotSpoilTime>.Element next = current.next();
               if (((Inventory.SlotSpoilTime)current.object).tickWorldTime <= clockWorldTimex) {
                  InventoryItem item = this.getItem(currentSlot);
                  if (item != null && item.item.shouldSpoilTick(item)) {
                     long nextWorldTickTime = item.item.tickSpoilTime(item, clock, this.spoilRateModifier, newItem -> this.setItem(currentSlot, newItem));
                     if (!current.isRemoved()) {
                        current.remove();
                     }

                     item = this.getItem(currentSlot);
                     if (item != null && item.item.shouldSpoilTick(item)) {
                        this.addSpoilSlotSorted(currentSlot, nextWorldTickTime);
                     }
                  } else {
                     this.nextSpoilSlots.removeFirst();
                     GameLog.debug.println("Had to remove slot " + currentSlot + " from spoilSlots: " + item + ", " + (entity == null ? tileEntity : entity));
                  }
               }

               if (next == null || ((Inventory.SlotSpoilTime)next.object).tickWorldTime != ((Inventory.SlotSpoilTime)current.object).tickWorldTime) {
                  break;
               }

               current = next;
            }
         }
      }
   }

   private void addSpoilSlotSorted(int slot, long nextWorldTickTime) {
      Iterator<GameLinkedList<Inventory.SlotSpoilTime>.Element> li = this.nextSpoilSlots.elementIterator();

      while (li.hasNext()) {
         GameLinkedList<Inventory.SlotSpoilTime>.Element current = li.next();
         if (((Inventory.SlotSpoilTime)current.object).tickWorldTime >= nextWorldTickTime) {
            current.insertBefore(new Inventory.SlotSpoilTime(slot, nextWorldTickTime));
            return;
         }
      }

      this.nextSpoilSlots.addLast(new Inventory.SlotSpoilTime(slot, nextWorldTickTime));
   }

   public void clean(int slot) {
      if (slot >= 0 && slot < this.dirtySlots.length) {
         if (this.dirtySlots[slot]) {
            this.totalDirty--;
         }

         this.dirtySlots[slot] = false;
      }
   }

   public void clean() {
      this.dirtySlots = new boolean[this.size];
      this.totalDirty = 0;
   }

   public void markDirty(int slot) {
      if (slot >= 0 && slot < this.dirtySlots.length) {
         if (!this.isDirty(slot)) {
            this.totalDirty++;
         }

         this.dirtySlots[slot] = true;
      }
   }

   public void markFullDirty() {
      Arrays.fill(this.dirtySlots, true);
      this.totalDirty = this.getSize();
   }

   public boolean isDirty(int slot) {
      return slot >= 0 && slot < this.dirtySlots.length ? this.dirtySlots[slot] : false;
   }

   public boolean isDirty() {
      return this.totalDirty > 0;
   }

   public boolean isFullDirty() {
      return this.getSize() > 0 && this.totalDirty >= this.getSize();
   }

   public void clearSlot(int slot) {
      this.setItem(slot, null);
   }

   public boolean isSlotClear(int slot) {
      return slot >= 0 && slot < this.items.length ? this.items[slot] == null : true;
   }

   public void clearInventory() {
      for (int i = 0; i < this.size; i++) {
         this.clearSlot(i);
      }
   }

   public void setItem(int slot, InventoryItem item) {
      this.setItem(slot, item, true);
   }

   public void setItem(int slot, InventoryItem item, boolean overrideIsNew) {
      if (slot >= 0 && slot < this.items.length) {
         if (item != null && item.getAmount() <= 0) {
            item = null;
         }

         if (item != null && !this.canLockItem(slot)) {
            item.setLocked(false);
         }

         if (!overrideIsNew && item != null && !this.isSlotClear(slot) && this.getItemSlot(slot).getID() == item.item.getID()) {
            item.setNew(this.getItem(slot).isNew());
         }

         this.items[slot] = item;
         this.updateSlot(slot);
      }
   }

   public InventoryItem getItem(int slot) {
      return slot >= 0 && slot < this.items.length ? this.items[slot] : null;
   }

   public String getItemStringID(int slot) {
      return this.getItemSlot(slot).getStringID();
   }

   public int getItemID(int slot) {
      return this.getItemSlot(slot) == null ? -1 : this.getItemSlot(slot).getID();
   }

   public void setAmount(int slot, int amount) {
      if (slot >= 0 && slot < this.items.length && this.items[slot] != null) {
         this.items[slot].setAmount(amount);
         if (this.items[slot].getAmount() <= 0) {
            this.items[slot] = null;
         }

         this.updateSlot(slot);
      }
   }

   public void addAmount(int slot, int amount) {
      if (slot >= 0 && slot < this.items.length && this.items[slot] != null) {
         this.items[slot].setAmount(this.items[slot].getAmount() + amount);
         if (this.getAmount(slot) > this.getItem(slot).itemStackSize()) {
            this.setAmount(slot, this.getItem(slot).itemStackSize());
         }
      }
   }

   public int getAmount(int slot) {
      return slot >= 0 && slot < this.items.length && this.items[slot] != null ? this.items[slot].getAmount() : 0;
   }

   public Item getItemSlot(int slot) {
      return slot >= 0 && slot < this.items.length && this.items[slot] != null ? this.items[slot].item : null;
   }

   public IntStream streamSlotIndexes() {
      return IntStream.range(0, this.size);
   }

   public Stream<InventorySlot> streamSlots() {
      return this.streamSlotIndexes().mapToObj(slot -> new InventorySlot(this, slot));
   }

   public int getAmount(Level level, PlayerMob player, Item item, String purpose) {
      return this.getAmount(level, player, item, 0, this.getSize() - 1, purpose);
   }

   public int getAmount(Level level, PlayerMob player, Item item, int startSlot, int endSlot, String purpose) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      int count = 0;

      for (int i = startSlot; i <= endSlot; i++) {
         if (!this.isSlotClear(i)) {
            InventoryItem invItem = this.getItem(i);
            count += invItem.item.getInventoryAmount(level, player, invItem, item, purpose);
         }
      }

      return count;
   }

   public int getAmount(Level level, PlayerMob player, Item.Type type, String purpose) {
      return this.getAmount(level, player, type, 0, this.getSize() - 1, purpose);
   }

   public int getAmount(Level level, PlayerMob player, Item.Type type, int startSlot, int endSlot, String purpose) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      int count = 0;

      for (int i = startSlot; i <= endSlot; i++) {
         if (!this.isSlotClear(i)) {
            InventoryItem invItem = this.getItem(i);
            count += invItem.item.getInventoryAmount(level, player, invItem, type, purpose);
         }
      }

      return count;
   }

   public IngredientStep countIngredientAmount(Level level, PlayerMob player, String purpose, IngredientCounter handler) {
      return this.countIngredientAmount(level, player, 0, this.getSize() - 1, purpose, handler);
   }

   public IngredientStep countIngredientAmount(Level level, PlayerMob player, int startSlot, int endSlot, String purpose, IngredientCounter handler) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      for (int i = startSlot; i <= endSlot; i++) {
         if (!this.isSlotClear(i)) {
            InventoryItem invItem = this.getItem(i);
            IngredientStep result = invItem.item.countIngredientAmount(level, player, this, i, invItem, purpose, handler);
            if (result == IngredientStep.BREAK) {
               return IngredientStep.BREAK;
            }
         }
      }

      return IngredientStep.CONTINUE;
   }

   public void useIngredientAmount(Level level, PlayerMob player, String purpose, IngredientUser handler) {
      this.useIngredientAmount(level, player, 0, this.getSize() - 1, purpose, handler);
   }

   public void useIngredientAmount(Level level, PlayerMob player, int startSlot, int endSlot, String purpose, IngredientUser handler) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      for (int i = startSlot; i <= endSlot; i++) {
         if (!this.isSlotClear(i)) {
            InventoryItem invItem = this.getItem(i);
            invItem.item.useIngredientAmount(level, player, this, i, invItem, purpose, handler);
         }
      }
   }

   public ArrayList<SlotPriority> getPriorityList(Level level, PlayerMob player, int startSlot, int endSlot, String purpose) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      ArrayList<SlotPriority> items = new ArrayList<>();

      for (int i = startSlot; i <= endSlot; i++) {
         if (!this.isSlotClear(i)) {
            InventoryItem invItem = this.getItem(i);
            items.add(new SlotPriority(i, invItem.item.getInventoryPriority(level, player, this, i, invItem, purpose)));
         }
      }

      Comparator<SlotPriority> comparator = Comparator.comparing(i -> i.comparable);
      items.sort(comparator);
      return items;
   }

   public ArrayList<SlotPriority> getPriorityList(Level level, PlayerMob player, Iterable<Integer> slots, String purpose) {
      ArrayList<SlotPriority> items = new ArrayList<>();

      for (int slot : slots) {
         if (slot >= 0 && slot < this.items.length && !this.isSlotClear(slot)) {
            InventoryItem invItem = this.getItem(slot);
            items.add(new SlotPriority(slot, invItem.item.getInventoryPriority(level, player, this, slot, invItem, purpose)));
         }
      }

      Comparator<SlotPriority> comparator = Comparator.comparing(i -> i.comparable);
      items.sort(comparator);
      return items;
   }

   public ArrayList<SlotPriority> getPriorityAddList(Level level, PlayerMob player, InventoryItem input, Iterable<Integer> slots, String purpose) {
      ArrayList<SlotPriority> items = new ArrayList<>();

      for (int slot : slots) {
         if (!this.isSlotClear(slot)) {
            InventoryItem invItem = this.getItem(slot);
            items.add(new SlotPriority(slot, invItem.item.getInventoryAddPriority(level, player, this, slot, invItem, input, purpose)));
         }
      }

      Comparator<SlotPriority> comparator = Comparator.comparing(i -> i.comparable);
      items.sort(comparator);
      return items;
   }

   public ArrayList<SlotPriority> getPriorityAddList(Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      ArrayList<SlotPriority> items = new ArrayList<>();

      for (int i = startSlot; i <= endSlot; i++) {
         if (!this.isSlotClear(i)) {
            InventoryItem invItem = this.getItem(i);
            items.add(new SlotPriority(i, invItem.item.getInventoryAddPriority(level, player, this, i, invItem, input, purpose)));
         }
      }

      Comparator<SlotPriority> comparator = Comparator.comparing(i -> i.comparable);
      items.sort(comparator);
      return items;
   }

   public Item getFirstItem(Level level, PlayerMob player, Item[] items, String purpose) {
      return this.getFirstItem(level, player, items, 0, this.getSize() - 1, purpose);
   }

   public Item getFirstItem(Level level, PlayerMob player, Item[] items, int startSlot, int endSlot, String purpose) {
      for (SlotPriority slotPriority : this.getPriorityList(level, player, startSlot, endSlot, purpose)) {
         InventoryItem invItem = this.getItem(slotPriority.slot);
         Item first = invItem.item.getInventoryFirstItem(level, player, invItem, items, purpose);
         if (first != null) {
            return first;
         }
      }

      return null;
   }

   public Item getFirstItem(Level level, PlayerMob player, Item.Type type, String purpose) {
      return this.getFirstItem(level, player, type, 0, this.getSize() - 1, purpose);
   }

   public Item getFirstItem(Level level, PlayerMob player, Item.Type type, int startSlot, int endSlot, String purpose) {
      for (SlotPriority slotPriority : this.getPriorityList(level, player, startSlot, endSlot, purpose)) {
         InventoryItem invItem = this.getItem(slotPriority.slot);
         Item first = invItem.item.getInventoryFirstItem(level, player, invItem, type, purpose);
         if (first != null) {
            return first;
         }
      }

      return null;
   }

   public InventoryItem getFirstInventoryItem(Level level, PlayerMob player, Item item, String purpose) {
      for (SlotPriority slotPriority : this.getPriorityList(level, player, 0, this.getSize() - 1, purpose)) {
         InventoryItem invItem = this.getItem(slotPriority.slot);
         if (invItem != null && item != null && invItem.item.getStringID().equals(item.getStringID())) {
            return invItem;
         }
      }

      return null;
   }

   public int removeItems(Level level, PlayerMob player, Item item, int amount, String purpose) {
      return this.removeItems(level, player, item, amount, 0, this.getSize() - 1, purpose);
   }

   public int removeItems(Level level, PlayerMob player, Item item, int amount, int startSlot, int endSlot, String purpose) {
      int remaining = amount;

      for (SlotPriority slotPriority : this.getPriorityList(level, player, startSlot, endSlot, purpose)) {
         if (remaining == 0) {
            return amount;
         }

         InventoryItem invItem = this.getItem(slotPriority.slot);
         int removed = invItem.item.removeInventoryAmount(level, player, invItem, item, remaining, purpose);
         remaining -= removed;
         if (invItem.getAmount() <= 0) {
            this.setItem(slotPriority.slot, null);
         }

         if (removed > 0) {
            this.updateSlot(slotPriority.slot);
         }
      }

      return amount - remaining;
   }

   public int removeItems(Level level, PlayerMob player, Item.Type type, int amount, String purpose) {
      return this.removeItems(level, player, type, amount, 0, this.getSize() - 1, purpose);
   }

   public int removeItems(Level level, PlayerMob player, Item.Type type, int amount, int startSlot, int endSlot, String purpose) {
      int remaining = amount;

      for (SlotPriority slotPriority : this.getPriorityList(level, player, startSlot, endSlot, purpose)) {
         if (remaining == 0) {
            return amount;
         }

         InventoryItem invItem = this.getItem(slotPriority.slot);
         int removed = invItem.item.removeInventoryAmount(level, player, invItem, type, remaining, purpose);
         remaining -= removed;
         if (invItem.getAmount() <= 0) {
            this.setItem(slotPriority.slot, null);
         }

         if (removed > 0) {
            this.updateSlot(slotPriority.slot);
         }
      }

      return amount - remaining;
   }

   public Item removeItem(Level level, PlayerMob player, Item.Type type, String purpose) {
      return this.removeItem(level, player, type, 0, this.getSize() - 1, purpose);
   }

   public Item removeItem(Level level, PlayerMob player, Item.Type type, int startSlot, int endSlot, String purpose) {
      for (SlotPriority slotPriority : this.getPriorityList(level, player, startSlot, endSlot, purpose)) {
         InventoryItem invItem = this.getItem(slotPriority.slot);
         int removed = invItem.item.removeInventoryAmount(level, player, invItem, type, 1, purpose);
         if (invItem.getAmount() <= 0) {
            this.setItem(slotPriority.slot, null);
         }

         if (removed > 0) {
            this.updateSlot(slotPriority.slot);
            return invItem.item;
         }
      }

      return null;
   }

   public int removeItems(
      Level level, PlayerMob player, Ingredient ingredient, Predicate<Item> itemFilter, int amount, Collection<InventoryItemsRemoved> collect
   ) {
      return this.removeItems(level, player, ingredient, itemFilter, amount, 0, this.getSize() - 1, collect);
   }

   public int removeItems(
      Level level,
      PlayerMob player,
      Ingredient ingredient,
      Predicate<Item> itemFilter,
      int amount,
      int startSlot,
      int endSlot,
      Collection<InventoryItemsRemoved> collect
   ) {
      int remaining = amount;

      for (SlotPriority slotPriority : this.getPriorityList(level, player, startSlot, endSlot, "crafting")) {
         if (remaining == 0) {
            return amount;
         }

         if (!this.isSlotClear(slotPriority.slot)) {
            InventoryItem invItem = this.getItem(slotPriority.slot);
            int removed = invItem.item.removeInventoryAmount(level, player, invItem, this, slotPriority.slot, ingredient, itemFilter, remaining, collect);
            remaining -= removed;
            if (invItem.getAmount() <= 0) {
               this.setItem(slotPriority.slot, null);
            }

            if (removed > 0) {
               this.updateSlot(slotPriority.slot);
            }
         }
      }

      return amount - remaining;
   }

   @Deprecated
   public boolean addItem(Level level, PlayerMob player, InventoryItem input, String purpose) {
      return this.addItem(level, player, input, purpose, null);
   }

   public boolean addItem(Level level, PlayerMob player, InventoryItem input, String purpose, InventoryAddConsumer addConsumer) {
      return this.addItem(level, player, input, 0, this.getSize() - 1, purpose, addConsumer);
   }

   @Deprecated
   public boolean addItem(Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose) {
      return this.addItem(level, player, input, startSlot, endSlot, purpose, null);
   }

   public boolean addItem(Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose, InventoryAddConsumer addConsumer) {
      return this.addItem(level, player, input, startSlot, endSlot, false, purpose, false, false, addConsumer);
   }

   @Deprecated
   public boolean addItem(
      Level level, PlayerMob player, InventoryItem input, int preferredSlot, boolean isLocked, String purpose, Inventory previousMoveToInventory
   ) {
      return this.addItem(level, player, input, preferredSlot, isLocked, purpose, null, previousMoveToInventory);
   }

   public boolean addItem(
      Level level,
      PlayerMob player,
      InventoryItem input,
      int preferredSlot,
      boolean isLocked,
      String purpose,
      InventoryAddConsumer addConsumer,
      Inventory previousMoveToInventory
   ) {
      if (preferredSlot < 0 || preferredSlot >= this.getSize() || !this.isItemValid(preferredSlot, input)) {
         return this.addItem(level, player, input, purpose, addConsumer);
      }

      if (this.isSlotClear(preferredSlot)) {
         int stackLimit = this.getItemStackLimit(preferredSlot, input);
         int amount = Math.min(input.getAmount(), stackLimit);
         InventoryItem insert = input.copy(amount);
         this.setItem(preferredSlot, insert);
         if (addConsumer != null) {
            addConsumer.add(this, preferredSlot, amount);
         }

         if (this.canLockItem(preferredSlot)) {
            insert.setLocked(isLocked);
         }

         input.setAmount(input.getAmount() - amount);
         return true;
      } else {
         boolean out = false;
         InventoryItem slotItem = this.getItem(preferredSlot);
         if (slotItem.equals(level, input, true, false, "equals")) {
            int stackLimit = this.getItemStackLimit(preferredSlot, slotItem);
            int amountToFillLimit = stackLimit - slotItem.getAmount();
            int amount = Math.min(input.getAmount(), amountToFillLimit);
            if (amount > 0) {
               ItemCombineResult combine = slotItem.combine(level, player, this, preferredSlot, input, amount, false, "add", addConsumer);
               if (combine.success) {
                  if (this.canLockItem(preferredSlot)) {
                     slotItem.setLocked(isLocked);
                  }

                  this.updateSlot(preferredSlot);
                  out = true;
               }
            }
         } else if (previousMoveToInventory == this) {
            int move = previousMoveToInventory.canAddItem(level, player, slotItem, 0, preferredSlot - 1, "move")
               + previousMoveToInventory.canAddItem(level, player, slotItem, preferredSlot + 1, previousMoveToInventory.getSize() - 1, "move");
            if (move >= slotItem.getAmount()) {
               previousMoveToInventory.addItem(level, player, slotItem, 0, preferredSlot - 1, "move", addConsumer);
               previousMoveToInventory.addItem(level, player, slotItem, preferredSlot + 1, previousMoveToInventory.getSize() - 1, "move", addConsumer);
               if (slotItem.getAmount() <= 0) {
                  this.clearSlot(preferredSlot);
                  int stackLimit = this.getItemStackLimit(preferredSlot, input);
                  int amount = Math.min(input.getAmount(), stackLimit);
                  InventoryItem insert = input.copy(amount);
                  this.setItem(preferredSlot, insert);
                  if (this.canLockItem(preferredSlot)) {
                     insert.setLocked(isLocked);
                  }

                  input.setAmount(input.getAmount() - amount);
                  if (addConsumer != null) {
                     addConsumer.add(this, preferredSlot, amount);
                  }
               }
            }
         } else {
            int move = previousMoveToInventory.canAddItem(level, player, slotItem, "move");
            if (move >= slotItem.getAmount()) {
               previousMoveToInventory.addItem(level, player, slotItem, "move", addConsumer);
               if (slotItem.getAmount() <= 0) {
                  this.clearSlot(preferredSlot);
                  int stackLimit = this.getItemStackLimit(preferredSlot, input);
                  int amount = Math.min(input.getAmount(), stackLimit);
                  InventoryItem insert = input.copy(amount);
                  this.setItem(preferredSlot, insert);
                  if (this.canLockItem(preferredSlot)) {
                     insert.setLocked(isLocked);
                  }

                  input.setAmount(input.getAmount() - amount);
                  if (addConsumer != null) {
                     addConsumer.add(this, preferredSlot, amount);
                  }
               }
            }
         }

         if (input.getAmount() > 0) {
            if (previousMoveToInventory != null) {
               out = previousMoveToInventory.addItem(level, player, input, purpose, addConsumer) || out;
            } else {
               out = this.addItem(level, player, input, purpose, addConsumer) || out;
            }
         }

         return out;
      }
   }

   @Deprecated
   public boolean addItem(
      Level level,
      PlayerMob player,
      InventoryItem input,
      int startSlot,
      int endSlot,
      boolean combineIsNew,
      String purpose,
      boolean ignoreValid,
      boolean ignoreStackLimit
   ) {
      return this.addItem(level, player, input, startSlot, endSlot, combineIsNew, purpose, ignoreValid, ignoreStackLimit, null);
   }

   public boolean addItem(
      Level level,
      PlayerMob player,
      InventoryItem input,
      int startSlot,
      int endSlot,
      boolean combineIsNew,
      String purpose,
      boolean ignoreValid,
      boolean ignoreStackLimit,
      InventoryAddConsumer addConsumer
   ) {
      boolean out = this.addItemOnlyCombine(level, player, input, startSlot, endSlot, combineIsNew, purpose, ignoreValid, ignoreStackLimit, addConsumer);
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      for (int i = startSlot; i <= endSlot && input.getAmount() > 0; i++) {
         if (this.isSlotClear(i) && (ignoreValid || this.isItemValid(i, input))) {
            int combineAmount = ignoreStackLimit ? input.itemStackSize() : this.getItemStackLimit(i, input);
            int amount = Math.min(input.getAmount(), combineAmount);
            if (amount > 0) {
               InventoryItem insert = input.copy(amount);
               this.setItem(i, insert);
               input.setAmount(input.getAmount() - amount);
               if (addConsumer != null) {
                  addConsumer.add(this, i, amount);
               }

               out = true;
            }
         }
      }

      return out;
   }

   @Deprecated
   public boolean addItemOnlyCombine(
      Level level,
      PlayerMob player,
      InventoryItem input,
      int startSlot,
      int endSlot,
      boolean combineIsNew,
      String purpose,
      boolean ignoreValid,
      boolean ignoreStackLimit
   ) {
      return this.addItemOnlyCombine(level, player, input, startSlot, endSlot, combineIsNew, purpose, ignoreValid, ignoreStackLimit, null);
   }

   public boolean addItemOnlyCombine(
      Level level,
      PlayerMob player,
      InventoryItem input,
      int startSlot,
      int endSlot,
      boolean combineIsNew,
      String purpose,
      boolean ignoreValid,
      boolean ignoreStackLimit,
      InventoryAddConsumer addConsumer
   ) {
      boolean out = false;

      for (SlotPriority slotPriority : this.getPriorityAddList(level, player, input, startSlot, endSlot, purpose)) {
         if (input.getAmount() <= 0) {
            break;
         }

         boolean isValid = ignoreValid || this.isItemValid(slotPriority.slot, input);
         int stackLimit = ignoreStackLimit ? input.itemStackSize() : this.getItemStackLimit(slotPriority.slot, input);
         InventoryItem invItem = this.getItem(slotPriority.slot);
         if (invItem.item.inventoryAddItem(level, player, this, slotPriority.slot, invItem, input, purpose, isValid, stackLimit, combineIsNew, addConsumer)) {
            out = true;
            this.updateSlot(slotPriority.slot);
         }
      }

      return out;
   }

   public int canAddItem(Level level, PlayerMob player, InventoryItem input, String purpose) {
      return this.canAddItem(level, player, input, 0, this.size - 1, purpose);
   }

   public int canAddItem(Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose) {
      return this.canAddItem(level, player, input, startSlot, endSlot, purpose, false, false);
   }

   public int canAddItem(
      Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose, boolean ignoreValid, boolean ignoreStackLimit
   ) {
      int addedItems = 0;
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      for (int i = startSlot; i <= endSlot && addedItems < input.getAmount(); i++) {
         if (this.isSlotClear(i)) {
            boolean isValid = ignoreValid || this.isItemValid(i, input);
            if (isValid) {
               addedItems += ignoreStackLimit ? input.itemStackSize() : this.getItemStackLimit(i, input);
            }
         } else {
            boolean isValid = ignoreValid || this.isItemValid(i, input);
            int stackLimit = ignoreStackLimit ? input.itemStackSize() : this.getItemStackLimit(i, input);
            InventoryItem invItem = this.getItem(i);
            addedItems += invItem.item.inventoryCanAddItem(level, player, invItem, input, purpose, isValid, stackLimit);
         }
      }

      return Math.min(input.getAmount(), addedItems);
   }

   public int canAddItemOnlyCombine(
      Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose, boolean ignoreValid, boolean ignoreStackLimit
   ) {
      int addedItems = 0;
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      for (int i = startSlot; i <= endSlot && addedItems < input.getAmount(); i++) {
         if (!this.isSlotClear(i)) {
            boolean isValid = ignoreValid || this.isItemValid(i, input);
            int stackLimit = ignoreStackLimit ? input.itemStackSize() : this.getItemStackLimit(i, input);
            InventoryItem invItem = this.getItem(i);
            addedItems += invItem.item.inventoryCanAddItem(level, player, invItem, input, purpose, isValid, stackLimit);
         }
      }

      return Math.min(input.getAmount(), addedItems);
   }

   @Deprecated
   public boolean restockFrom(Level level, PlayerMob player, InventoryItem input, int startSlot, int endSlot, String purpose, boolean ignoreStackLimit) {
      return this.restockFrom(level, player, input, startSlot, endSlot, purpose, ignoreStackLimit, null);
   }

   public boolean restockFrom(
      Level level,
      PlayerMob player,
      InventoryItem input,
      int startSlot,
      int endSlot,
      String purpose,
      boolean ignoreStackLimit,
      InventoryAddConsumer addConsumer
   ) {
      boolean out = false;

      for (SlotPriority slotPriority : this.getPriorityAddList(level, player, input, startSlot, endSlot, purpose)) {
         if (input.getAmount() <= 0) {
            break;
         }

         int stackLimit = ignoreStackLimit ? input.itemStackSize() : this.getItemStackLimit(slotPriority.slot, input);
         InventoryItem item = this.getItem(slotPriority.slot);
         if (input != item
            && item.canCombine(level, player, input, purpose)
            && item.item.onCombine(level, player, this, slotPriority.slot, item, input, stackLimit, input.getAmount(), false, purpose, addConsumer)) {
            this.updateSlot(slotPriority.slot);
            out = true;
         }
      }

      return out;
   }

   public void swapItems(int slot1, int slot2) {
      InventoryItem tempItem = this.getItem(slot1);
      this.setItem(slot1, this.getItem(slot2));
      this.setItem(slot2, tempItem);
      this.updateSlot(slot1);
      this.updateSlot(slot2);
   }

   @Deprecated
   public ItemCombineResult combineItems(Level level, PlayerMob player, int staySlot, int combineSlot, int amount, boolean combineIsNew, String purpose) {
      return this.combineItems(level, player, staySlot, combineSlot, amount, combineIsNew, purpose, null);
   }

   public ItemCombineResult combineItems(
      Level level, PlayerMob player, int staySlot, int combineSlot, int amount, boolean combineIsNew, String purpose, InventoryAddConsumer addConsumer
   ) {
      if (!this.isSlotClear(staySlot) && !this.isSlotClear(combineSlot)) {
         ItemCombineResult out = this.getItem(staySlot)
            .combine(level, player, this, staySlot, this.getItem(combineSlot), amount, combineIsNew, purpose, addConsumer);
         if (out.success) {
            this.updateSlot(staySlot);
            this.updateSlot(combineSlot);
         }

         return out;
      } else {
         return ItemCombineResult.failure();
      }
   }

   @Deprecated
   public ItemCombineResult combineItem(
      Level level, PlayerMob player, int staySlot, InventoryItem combineItem, int amount, boolean combineIsNew, String purpose
   ) {
      return this.combineItem(level, player, staySlot, combineItem, amount, combineIsNew, purpose, null);
   }

   public ItemCombineResult combineItem(
      Level level,
      PlayerMob player,
      int staySlot,
      InventoryItem combineItem,
      int amount,
      boolean combineIsNew,
      String purpose,
      InventoryAddConsumer addConsumer
   ) {
      if (!this.isSlotClear(staySlot) && combineItem != null) {
         ItemCombineResult out = this.getItem(staySlot).combine(level, player, this, staySlot, combineItem, amount, combineIsNew, purpose, addConsumer);
         if (out.success) {
            this.updateSlot(staySlot);
         }

         return out;
      } else {
         return ItemCombineResult.failure();
      }
   }

   @Deprecated
   public ItemCombineResult combineItem(
      Level level, PlayerMob player, InventoryItem stayItem, int combineSlot, int amount, boolean combineIsNew, String purpose
   ) {
      return this.combineItem(level, player, stayItem, combineSlot, amount, combineIsNew, purpose, null);
   }

   public ItemCombineResult combineItem(
      Level level,
      PlayerMob player,
      InventoryItem stayItem,
      int combineSlot,
      int amount,
      boolean combineIsNew,
      String purpose,
      InventoryAddConsumer addConsumer
   ) {
      if (stayItem != null && !this.isSlotClear(combineSlot)) {
         ItemCombineResult out = stayItem.combine(level, player, this, combineSlot, this.getItem(combineSlot), amount, combineIsNew, purpose, addConsumer);
         if (out.success) {
            this.updateSlot(combineSlot);
         }

         return out;
      } else {
         return ItemCombineResult.failure();
      }
   }

   public void updateSlot(int slot) {
      InventoryItem item = this.getItem(slot);
      if (item == null) {
         this.tickSlots.remove(slot);
      } else if (item.item.isTickItem()) {
         this.tickSlots.add(slot);
      } else {
         this.tickSlots.remove(slot);
      }

      if (item == null) {
         this.nextSpoilSlots.removeSlot(slot);
      } else if (item.item.shouldSpoilTick(item)) {
         this.nextSpoilSlots.addFirst(new Inventory.SlotSpoilTime(slot, Long.MIN_VALUE));
      } else {
         this.nextSpoilSlots.removeSlot(slot);
      }

      ListIterator<InventoryUpdateListener> updateIterator = this.slotUpdateListeners.listIterator();
      if (updateIterator.hasNext()) {
         InventoryUpdateListener next = updateIterator.next();
         if (next.isDisposed()) {
            updateIterator.remove();
         } else {
            next.onSlotUpdate(slot);
         }
      }

      this.markDirty(slot);
   }

   public InventoryUpdateListener addSlotUpdateListener(InventoryUpdateListener listener) {
      GameLinkedList<InventoryUpdateListener>.Element element = this.slotUpdateListeners.addLast(listener);
      listener.init(() -> {
         if (!element.isRemoved()) {
            element.remove();
         }
      });
      return listener;
   }

   public int getSlotUpdateListenersSize() {
      return this.slotUpdateListeners.size();
   }

   public void cleanSlotUpdateListeners() {
      this.slotUpdateListeners.removeIf(InventoryUpdateListener::isDisposed);
   }

   public boolean canBeUsedForCrafting() {
      return true;
   }

   public boolean canLockItem(int slot) {
      return false;
   }

   public boolean isItemLocked(int slot) {
      return !this.isSlotClear(slot) && this.canLockItem(slot) && this.getItem(slot).isLocked();
   }

   public void setItemLocked(int slot, boolean locked) {
      if (!this.isSlotClear(slot)) {
         if (!this.canLockItem(slot)) {
            this.getItem(slot).setLocked(false);
         } else {
            this.getItem(slot).setLocked(locked);
         }
      }
   }

   public void sortItems(Level level, PlayerMob player, int startSlot, int endSlot) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      if (endSlot < startSlot) {
         throw new IllegalArgumentException("End slot parameter cannot lower than start slot");
      }

      if (endSlot - startSlot != 0) {
         Inventory tempInv = new Inventory(endSlot - startSlot + 1);
         boolean[] locked = new boolean[this.getSize()];

         for (int i = startSlot; i <= endSlot; i++) {
            locked[i] = this.isItemLocked(i);
            if (!this.isSlotClear(i) && !this.isItemLocked(i)) {
               if (this.getAmount(i) > this.getItemSlot(i).getStackSize()) {
                  this.setAmount(i, this.getItemSlot(i).getStackSize());
               }

               tempInv.addItem(level, player, this.getItem(i), "sort", null);
            }
         }

         ArrayList<InventoryItem> sorted = new ArrayList<>(tempInv.size);

         for (int i = 0; i < tempInv.getSize(); i++) {
            if (!tempInv.isSlotClear(i)) {
               sorted.add(tempInv.getItem(i));
            }
         }

         Collections.sort(sorted);
         int sortedIndex = 0;

         for (int i = startSlot; i <= endSlot; i++) {
            if (!locked[i]) {
               if (sortedIndex < sorted.size()) {
                  this.setItem(i, sorted.get(sortedIndex++), false);
               } else {
                  this.setItem(i, null);
               }
            }
         }
      }
   }

   public void sortItems(Level level, PlayerMob player) {
      this.sortItems(level, player, 0, this.getSize() - 1);
   }

   public void compressItems(int startSlot, int endSlot) {
      if (startSlot < 0) {
         startSlot = 0;
      }

      if (endSlot > this.items.length - 1) {
         endSlot = this.items.length - 1;
      }

      if (endSlot < startSlot) {
         throw new IllegalArgumentException("End slot parameter cannot lower than start slot");
      }

      if (endSlot - startSlot != 0) {
         Inventory tempInv = new Inventory(endSlot - startSlot + 1);
         boolean[] locked = new boolean[this.getSize()];

         for (int i = startSlot; i <= endSlot; i++) {
            locked[i] = this.isItemLocked(i);
            if (!this.isSlotClear(i) && !this.isItemLocked(i)) {
               if (this.getAmount(i) > this.getItemSlot(i).getStackSize()) {
                  this.setAmount(i, this.getItemSlot(i).getStackSize());
               }

               tempInv.addItem(null, null, this.getItem(i), "sort", null);
            }
         }

         int tempInvIndex = 0;

         for (int i = startSlot; i <= endSlot; i++) {
            if (!locked[i]) {
               InventoryItem tempInvItem = tempInv.getItem(tempInvIndex++);
               if (tempInvItem != null) {
                  this.setItem(i, tempInvItem, false);
               } else {
                  this.setItem(i, null);
               }
            }
         }
      }
   }

   public void compressItems() {
      if (this.getSize() != 0) {
         this.compressItems(0, this.getSize() - 1);
      }
   }

   public final boolean isItemValid(int slot, InventoryItem item) {
      return this.filter == null || this.filter.isItemValid(slot, item);
   }

   public final int getItemStackLimit(int slot, InventoryItem item) {
      if (item == null) {
         return Integer.MAX_VALUE;
      } else {
         return this.filter == null ? item.itemStackSize() : this.filter.getItemStackLimit(slot, item);
      }
   }

   public Inventory getTempClone() {
      Inventory clone = new Inventory(this.getSize()) {
         @Override
         public boolean canLockItem(int slot) {
            return true;
         }
      };

      for (int i = 0; i < this.getSize(); i++) {
         if (this.isSlotClear(i)) {
            clone.items[i] = null;
         } else {
            clone.items[i] = this.getItem(i).copy(this.getAmount(i), this.isItemLocked(i));
         }
      }

      return clone;
   }

   public Packet getContentPacket() {
      Packet content = new Packet();
      this.writeContent(new PacketWriter(content));
      return content;
   }

   public void writeContent(PacketWriter writer) {
      writer.putNextShortUnsigned(this.getSize());

      for (int i = 0; i < this.getSize(); i++) {
         boolean hasItem = !this.isSlotClear(i);
         writer.putNextBoolean(hasItem);
         if (hasItem) {
            InventoryItem.addPacketContent(this.getItem(i), writer);
         }
      }
   }

   public static Inventory getInventory(Packet contentPacket) {
      return getInventory(new PacketReader(contentPacket));
   }

   public static Inventory getInventory(PacketReader reader) {
      int size = reader.getNextShortUnsigned();
      Inventory out = new Inventory(size) {
         @Override
         public boolean canLockItem(int slot) {
            return true;
         }
      };

      for (int i = 0; i < out.getSize(); i++) {
         if (reader.getNextBoolean()) {
            out.setItem(i, InventoryItem.fromContentPacket(reader));
         }
      }

      return out;
   }

   private static class NextSpoilSlotList extends GameLinkedList<Inventory.SlotSpoilTime> {
      private final HashMap<Integer, GameLinkedList<Inventory.SlotSpoilTime>.Element> nextSpoilSlotsElements = new HashMap<>();

      public NextSpoilSlotList() {
      }

      @Override
      public void onAdded(GameLinkedList<Inventory.SlotSpoilTime>.Element element) {
         super.onAdded(element);
         GameLinkedList<Inventory.SlotSpoilTime>.Element lastElement = this.nextSpoilSlotsElements.remove(((Inventory.SlotSpoilTime)element.object).slot);
         if (lastElement != null && !lastElement.isRemoved()) {
            lastElement.remove();
         }

         this.nextSpoilSlotsElements.put(((Inventory.SlotSpoilTime)element.object).slot, element);
      }

      @Override
      public void onRemoved(GameLinkedList<Inventory.SlotSpoilTime>.Element element) {
         super.onRemoved(element);
         this.nextSpoilSlotsElements.remove(((Inventory.SlotSpoilTime)element.object).slot);
      }

      public void removeSlot(int slot) {
         GameLinkedList<Inventory.SlotSpoilTime>.Element lastElement = this.nextSpoilSlotsElements.remove(slot);
         if (lastElement != null && !lastElement.isRemoved()) {
            lastElement.remove();
         }
      }

      @Override
      public void clear() {
         super.clear();
         this.nextSpoilSlotsElements.clear();
      }
   }

   private static class SlotSpoilTime {
      public final int slot;
      public final long tickWorldTime;

      public SlotSpoilTime(int slot, long tickWorldTime) {
         this.slot = slot;
         this.tickWorldTime = tickWorldTime;
      }
   }
}
