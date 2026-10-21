package necesse.inventory.container;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.GameTileRange;
import necesse.engine.GlobalData;
import necesse.engine.journal.listeners.CraftedRecipeJournalChallengeListener;
import necesse.engine.localization.Localization;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketContainerAction;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.HashMapGameLinkedList;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.forms.ContainerComponent;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryItemsRemoved;
import necesse.inventory.InventoryRange;
import necesse.inventory.ItemCombineResult;
import necesse.inventory.PlayerInventory;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.container.customAction.ContainerCustomAction;
import necesse.inventory.container.customAction.ContainerCustomActionRegistry;
import necesse.inventory.container.events.ContainerEvent;
import necesse.inventory.container.events.ContainerEventHandler;
import necesse.inventory.container.events.ContainerEventSubscription;
import necesse.inventory.container.slots.ArmorContainerSlot;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.container.slots.MountContainerSlot;
import necesse.inventory.container.slots.TrashContainerSlot;
import necesse.inventory.container.slots.TrinketAbilityContainerSlot;
import necesse.inventory.container.slots.TrinketContainerSlot;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.recipe.CanCraft;
import necesse.inventory.recipe.ContainerRecipeCraftedEvent;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class Container {
   public static final int DROP_ITEM_SLOT = -1;
   public static final int SORT_INVENTORY_SLOT = -2;
   public static final int QUICK_STACK_SLOT = -3;
   public static final int RESTOCK_SLOT = -4;
   protected boolean shouldClose;
   protected boolean isClosed;
   private final ArrayList<ContainerSlot> slots;
   private final HashSet<Integer> lockedSlots;
   private final ArrayList<ContainerRecipe> recipes;
   private final ContainerCustomActionRegistry actionRegistry;
   protected LinkedHashSet<Inventory> craftInventories;
   protected LinkedList<Container.QuickTransferOption> quickTransferOptions;
   protected LinkedList<ContainerEventSubscription<?>> eventSubscriptions;
   protected HashMapGameLinkedList<Class<?>, ContainerEventHandler<?>> eventHandlers;
   public final NetworkClient client;
   public final int uniqueSeed;
   public int CLIENT_DRAGGING_SLOT = -1;
   public int CLIENT_HOTBAR_START = -1;
   public int CLIENT_HOTBAR_END = -1;
   public int CLIENT_INVENTORY_START = -1;
   public int CLIENT_INVENTORY_END = -1;
   public int CLIENT_HELMET_SLOT = -1;
   public int CLIENT_CHEST_SLOT = -1;
   public int CLIENT_FEET_SLOT = -1;
   public int CLIENT_COSM_HELMET_SLOT = -1;
   public int CLIENT_COSM_CHEST_SLOT = -1;
   public int CLIENT_COSM_FEET_SLOT = -1;
   public int CLIENT_MOUNT_SLOT = -1;
   public int CLIENT_TRINKET_ABILITY_SLOT = -1;
   public int CLIENT_TRINKET_START = -1;
   public int CLIENT_TRINKET_END = -1;
   public int CLIENT_TRASH_SLOT = -1;
   public ContainerComponent<?> form;

   public Container(final NetworkClient client, int uniqueSeed) {
      this.slots = new ArrayList<>();
      this.lockedSlots = new HashSet<>();
      this.client = client;
      this.uniqueSeed = uniqueSeed;
      this.recipes = new ArrayList<>();
      this.craftInventories = new LinkedHashSet<>();
      this.quickTransferOptions = new LinkedList<>();
      this.eventSubscriptions = new LinkedList<>();
      this.eventHandlers = new HashMapGameLinkedList<>();
      this.actionRegistry = new ContainerCustomActionRegistry(this);
      this.isClosed = false;
      this.CLIENT_DRAGGING_SLOT = this.addSlot(new ContainerSlot(client.playerMob.getInv().drag, 0));
      PlayerInventory main = client.playerMob.getInv().main;

      for (int i = 0; i < 10; i++) {
         int index = this.addSlot(new ContainerSlot(main, i));
         if (this.CLIENT_HOTBAR_START == -1) {
            this.CLIENT_HOTBAR_START = index;
         }

         if (this.CLIENT_HOTBAR_END == -1) {
            this.CLIENT_HOTBAR_END = index;
         }

         this.CLIENT_HOTBAR_START = Math.min(this.CLIENT_HOTBAR_START, index);
         this.CLIENT_HOTBAR_END = Math.max(this.CLIENT_HOTBAR_END, index);
      }

      for (int i = 10; i < main.getSize(); i++) {
         int index = this.addSlot(new ContainerSlot(main, i));
         if (this.CLIENT_INVENTORY_START == -1) {
            this.CLIENT_INVENTORY_START = index;
         }

         if (this.CLIENT_INVENTORY_END == -1) {
            this.CLIENT_INVENTORY_END = index;
         }

         this.CLIENT_INVENTORY_START = Math.min(this.CLIENT_INVENTORY_START, index);
         this.CLIENT_INVENTORY_END = Math.max(this.CLIENT_INVENTORY_END, index);
      }

      this.CLIENT_HELMET_SLOT = this.addSlot(new ArmorContainerSlot(null, 0, ArmorItem.ArmorType.HEAD) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedArmorInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_CHEST_SLOT = this.addSlot(new ArmorContainerSlot(null, 1, ArmorItem.ArmorType.CHEST) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedArmorInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_FEET_SLOT = this.addSlot(new ArmorContainerSlot(null, 2, ArmorItem.ArmorType.FEET) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedArmorInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_COSM_HELMET_SLOT = this.addSlot(new ArmorContainerSlot(null, 0, ArmorItem.ArmorType.HEAD) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedCosmeticInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_COSM_CHEST_SLOT = this.addSlot(new ArmorContainerSlot(null, 1, ArmorItem.ArmorType.CHEST) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedCosmeticInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_COSM_FEET_SLOT = this.addSlot(new ArmorContainerSlot(null, 2, ArmorItem.ArmorType.FEET) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedCosmeticInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_MOUNT_SLOT = this.addSlot(new MountContainerSlot(null, 0) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedEquipmentInventory(this.getInventorySlot());
         }
      });
      this.CLIENT_TRINKET_ABILITY_SLOT = this.addSlot(new TrinketAbilityContainerSlot(null, 1) {
         @Override
         public Inventory getInventory() {
            return client.playerMob.getInv().equipment.getSelectedEquipmentInventory(this.getInventorySlot());
         }
      });
      int trinketSlotCount = client.playerMob.getInv().equipment.getTrinketSlotsSize();

      for (int i = 0; i < trinketSlotCount; i++) {
         int index = this.addSlot(new TrinketContainerSlot(null, i) {
            @Override
            public Inventory getInventory() {
               return client.playerMob.getInv().equipment.getSelectedTrinketsInventory(this.getInventorySlot());
            }
         });
         if (this.CLIENT_TRINKET_START == -1) {
            this.CLIENT_TRINKET_START = index;
         }

         if (this.CLIENT_TRINKET_END == -1) {
            this.CLIENT_TRINKET_END = index;
         }

         this.CLIENT_TRINKET_START = Math.min(this.CLIENT_TRINKET_START, index);
         this.CLIENT_TRINKET_END = Math.max(this.CLIENT_TRINKET_END, index);
      }

      this.CLIENT_TRASH_SLOT = this.addSlot(new TrashContainerSlot(client.playerMob.getInv().trash, 0));
      Recipes.streamRecipes().filter(r -> r.matchTech(RecipeTechRegistry.NONE)).forEach(r -> this.addRecipe(r, true));
      this.quickTransferOptions
         .addFirst(
            new Container.QuickTransferOption(
               null,
               true,
               new SlotIndexRange(this.CLIENT_HOTBAR_START, this.CLIENT_HOTBAR_END),
               new SlotIndexRange(this.CLIENT_INVENTORY_START, this.CLIENT_INVENTORY_END)
            )
         );
      this.quickTransferOptions
         .addFirst(
            new Container.QuickTransferOption(
               null,
               true,
               new SlotIndexRange(this.CLIENT_INVENTORY_START, this.CLIENT_INVENTORY_END),
               new SlotIndexRange(this.CLIENT_HOTBAR_START, this.CLIENT_HOTBAR_END)
            )
         );
   }

   public void init() {
      this.actionRegistry.closeRegistry();
   }

   public void tick() {
      if (this.client.isClient()) {
         boolean updateCraftable = false;

         for (Inventory inv : this.craftInventories) {
            if (inv.isDirty()) {
               updateCraftable = true;
            }

            inv.clean();
         }

         if (updateCraftable) {
            GlobalData.updateCraftable();
         }
      }
   }

   public void lootAllControlPressed() {
   }

   public void sortInventoryControlPressed() {
      if (this.client.isClient()) {
         ClientClient clientClient = this.client.getClientClient();
         if (clientClient.isLocalClient()) {
            clientClient.getClient().network.sendPacket(new PacketContainerAction(-2, ContainerAction.LEFT_CLICK, 1));
         }
      }
   }

   public void quickStackControlPressed() {
      if (this.client.isClient()) {
         ClientClient clientClient = this.client.getClientClient();
         if (clientClient.isLocalClient()) {
            clientClient.getClient().network.sendPacket(new PacketContainerAction(-3, ContainerAction.LEFT_CLICK, 1));
         }
      }
   }

   public ContainerActionResult applyContainerAction(int slotIndex, ContainerAction action) {
      if (this.isSlotLocked(slotIndex)) {
         return new ContainerActionResult(255);
      }

      if (slotIndex == -4) {
         this.restockPlayerInventory();
         return new ContainerActionResult(1);
      }

      if (slotIndex == -3) {
         this.quickStackPlayerInventory();
         return new ContainerActionResult(1);
      }

      if (slotIndex == -2) {
         PlayerInventory main = this.getClientInventory().main;
         main.sortItems(this.client.playerMob.getLevel(), this.client.playerMob, 10, main.getSize() - 1);
         return new ContainerActionResult(1);
      }

      if (slotIndex == -1) {
         ContainerSlot draggingSlot = this.getClientDraggingSlot();
         if (draggingSlot.isClear()) {
            return new ContainerActionResult(null);
         }

         if (draggingSlot.isItemLocked()) {
            return new ContainerActionResult(Localization.translate("misc", "cannotdroplocked"));
         }

         switch (action) {
            case RIGHT_CLICK:
               int itemAmount = draggingSlot.getItemAmount();
               if (this.client.isServer()) {
                  this.client.playerMob.dropDraggingItem(itemAmount);
               }

               return new ContainerActionResult(itemAmount);
            case TAKE_ONE:
               if (this.client.isServer()) {
                  this.client.playerMob.dropDraggingItem(1);
               }

               return new ContainerActionResult(1);
            default:
               return new ContainerActionResult(null);
         }
      } else {
         ContainerSlot slot = this.getSlot(slotIndex);
         if (slot == null) {
            GameLog.warn.println("Tried to apply container action on invalid slot.");
            return new ContainerActionResult(null);
         }

         switch (action) {
            case RIGHT_CLICK:
               return this.applyRightClick(slotIndex, slot);
            case TAKE_ONE:
               return this.applyTakeOne(slotIndex, slot);
            case LEFT_CLICK:
               return this.applyLeftClick(slotIndex, slot);
            case QUICK_MOVE:
               return this.applyQuickMove(slotIndex, slot);
            case QUICK_TRASH:
               return this.applyQuickTrash(slotIndex, slot);
            case QUICK_TRASH_ONE:
               return this.applyQuickTrashOne(slotIndex, slot);
            case QUICK_DROP:
               if (!slot.isClear() && !slot.isItemLocked()) {
                  int itemAmount = slot.getItemAmount();
                  if (this.client.isServer()) {
                     this.client.playerMob.dropItem(slot.getItem());
                  }

                  slot.setItem(null);
                  return new ContainerActionResult(itemAmount);
               }

               return new ContainerActionResult(null);
            case QUICK_DROP_ONE:
               if (!slot.isClear() && !slot.isItemLocked()) {
                  int itemAmount = 1;
                  if (this.client.isServer()) {
                     this.client.playerMob.dropItem(slot.getItem().copy(itemAmount));
                  }

                  slot.setAmount(slot.getItemAmount() - itemAmount);
                  if (slot.getItemAmount() <= 0) {
                     slot.setItem(null);
                  }

                  return new ContainerActionResult(itemAmount);
               }

               return new ContainerActionResult(null);
            case TOGGLE_LOCKED:
               if (!slot.isClear() && slot.canLockItem()) {
                  slot.setItemLocked(!slot.isItemLocked());
                  return new ContainerActionResult(slot.getItem().isLocked() ? 1 : 2);
               }

               return new ContainerActionResult(null);
            case QUICK_MOVE_ONE:
               return this.applyQuickMoveOne(slotIndex, slot);
            case QUICK_GET_ONE:
               return this.applyQuickGetOne(slotIndex, slot);
            case RIGHT_CLICK_ACTION:
               if (!slot.isClear()) {
                  InventoryItem item = slot.getItem();
                  Supplier<ContainerActionResult> rAction = item.item.getInventoryRightClickAction(this, item, slotIndex, slot);
                  if (rAction != null) {
                     return rAction.get();
                  }
               }

               return new ContainerActionResult(null);
            default:
               return new ContainerActionResult(null);
         }
      }
   }

   public ContainerActionResult applyLeftClick(int slotIndex, ContainerSlot slot) {
      if (this.getClientDraggingSlot().isClear()) {
         if (!slot.isClear()) {
            this.getClientDraggingSlot().combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, true, false, "leftclick");
            return new ContainerActionResult(1);
         } else {
            return new ContainerActionResult(null);
         }
      } else {
         if (slot.isClear()) {
            String error = slot.getItemInvalidError(this.getClientDraggingSlot().getItem());
            if (error != null) {
               return new ContainerActionResult(5, error);
            }
         }

         ItemCombineResult combineResult1 = slot.combineSlots(
            this.client.playerMob.getLevel(), this.client.playerMob, this.getClientDraggingSlot(), true, false, "leftclick"
         );
         if (!combineResult1.success) {
            ItemCombineResult combineResult2 = this.getClientDraggingSlot()
               .combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, true, false, "leftclickinv");
            if (!combineResult2.success) {
               ItemCombineResult swapResult = slot.swapItems(this.getClientDraggingSlot());
               return new ContainerActionResult(3, swapResult.error);
            } else {
               return new ContainerActionResult(4, combineResult2.error);
            }
         } else {
            return new ContainerActionResult(2, combineResult1.error);
         }
      }
   }

   public ContainerActionResult applyRightClick(int slotIndex, ContainerSlot slot) {
      if (this.getClientDraggingSlot().isClear()) {
         if (!slot.isClear()) {
            if (slot.getItemAmount() <= 1) {
               this.getClientDraggingSlot().combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, true, false, "leftclick");
               return new ContainerActionResult(1);
            } else {
               int halfItems = (slot.getItemAmount() + 1) / 2;
               this.getClientDraggingSlot().combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, halfItems, false, false, "rightclick");
               return new ContainerActionResult(2);
            }
         } else {
            return new ContainerActionResult(null);
         }
      } else {
         ItemCombineResult combineResult = slot.combineSlots(
            this.client.playerMob.getLevel(), this.client.playerMob, this.getClientDraggingSlot(), 1, false, false, "rightclick"
         );
         return !combineResult.success ? new ContainerActionResult(4, combineResult.error) : new ContainerActionResult(3, combineResult.error);
      }
   }

   public ContainerActionResult transferFromAmount(int slotIndex, ContainerSlot slot, int amount) {
      if (slot.isClear()) {
         return new ContainerActionResult(null);
      }

      LinkedList<SlotIndexRange> toIndexRanges = new LinkedList<>();
      boolean foundAnyValid = false;

      for (Container.QuickTransferOption option : this.quickTransferOptions) {
         if (option.filter == null || option.filter.test(slot)) {
            if (option.onlyIfNoOtherFilterValid) {
               if (foundAnyValid) {
                  continue;
               }
            } else {
               foundAnyValid = true;
            }

            if (slotIndex >= option.input.fromIndex && slotIndex <= option.input.toIndex) {
               toIndexRanges.addAll(Arrays.asList(option.outputs));
            }
         }
      }

      ContainerTransferResult transferResult = this.transferToSlots(slot, toIndexRanges, amount);
      return new ContainerActionResult(amount - transferResult.amount, transferResult.error);
   }

   public ContainerActionResult transferIntoAmount(int slotIndex, ContainerSlot slot, int amount) {
      if (!slot.isClear()) {
         int startAmount = amount;
         boolean foundAnyValid = false;

         for (Container.QuickTransferOption option : this.quickTransferOptions) {
            if (option.filter == null || option.filter.test(slot)) {
               if (option.onlyIfNoOtherFilterValid) {
                  if (foundAnyValid) {
                     continue;
                  }
               } else {
                  foundAnyValid = true;
               }

               if (slotIndex >= option.input.fromIndex && slotIndex <= option.input.toIndex) {
                  String error = null;

                  for (SlotIndexRange output : option.outputs) {
                     ContainerTransferResult transferResult = this.transferFromSlots(slot, output.fromIndex, output.toIndex, amount);
                     amount = transferResult.amount;
                     if (transferResult.error != null) {
                        error = transferResult.error;
                     }
                  }

                  return new ContainerActionResult(startAmount - amount, error);
               }
            }
         }
      }

      return new ContainerActionResult(null);
   }

   public ContainerActionResult applyQuickMove(int slotIndex, ContainerSlot slot) {
      return this.transferFromAmount(slotIndex, slot, Integer.MAX_VALUE);
   }

   public ContainerActionResult applyTakeOne(int slotIndex, ContainerSlot slot) {
      if (!slot.isClear()) {
         ItemCombineResult combineResult = this.getClientDraggingSlot()
            .combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, 1, slot.getItemAmount() == 1, false, "rightclick");
         return new ContainerActionResult(combineResult.success ? 2 : 1, combineResult.error);
      } else {
         return new ContainerActionResult(null);
      }
   }

   public ContainerActionResult applyQuickMoveOne(int slotIndex, ContainerSlot slot) {
      return this.transferFromAmount(slotIndex, slot, 1);
   }

   public ContainerActionResult applyQuickGetOne(int slotIndex, ContainerSlot slot) {
      return this.transferIntoAmount(slotIndex, slot, 1);
   }

   public ContainerActionResult applyQuickTrash(int slotIndex, ContainerSlot slot) {
      if (slot.isClear() || slot.isItemLocked()) {
         return new ContainerActionResult(0);
      } else if (slotIndex == this.CLIENT_TRASH_SLOT) {
         this.transferToSlots(
            slot,
            Arrays.asList(
               new SlotIndexRange(this.CLIENT_HOTBAR_START, this.CLIENT_HOTBAR_END), new SlotIndexRange(this.CLIENT_INVENTORY_START, this.CLIENT_INVENTORY_END)
            )
         );
         return new ContainerActionResult(1);
      } else {
         this.getSlot(this.CLIENT_TRASH_SLOT).combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, true, false, "trash");
         return new ContainerActionResult(2);
      }
   }

   public ContainerActionResult applyQuickTrashOne(int slotIndex, ContainerSlot slot) {
      if (slot.isClear() || slot.isItemLocked()) {
         return new ContainerActionResult(0);
      } else if (slotIndex == this.CLIENT_TRASH_SLOT) {
         this.transferToSlots(
            slot,
            Arrays.asList(
               new SlotIndexRange(this.CLIENT_HOTBAR_START, this.CLIENT_HOTBAR_END), new SlotIndexRange(this.CLIENT_INVENTORY_START, this.CLIENT_INVENTORY_END)
            ),
            1
         );
         return new ContainerActionResult(1);
      } else {
         this.getSlot(this.CLIENT_TRASH_SLOT).combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, slot, 1, true, false, "trash");
         return new ContainerActionResult(2);
      }
   }

   public void addQuickTransferOption(Predicate<ContainerSlot> filter, int inputFromIndex, int inputToIndex, int outputFromIndex, int outputToIndex) {
      this.quickTransferOptions
         .addFirst(
            new Container.QuickTransferOption(
               filter, false, new SlotIndexRange(inputFromIndex, inputToIndex), new SlotIndexRange(outputFromIndex, outputToIndex)
            )
         );
   }

   public void addQuickTransferOption(int inputFromIndex, int inputToIndex, int outputFromIndex, int outputToIndex) {
      this.addQuickTransferOption(null, inputFromIndex, inputToIndex, outputFromIndex, outputToIndex);
   }

   public void addQuickTransferOption(Predicate<ContainerSlot> filter, int inputFromIndex, int inputToIndex, SlotIndexRange... outputs) {
      this.quickTransferOptions.addFirst(new Container.QuickTransferOption(filter, false, new SlotIndexRange(inputFromIndex, inputToIndex), outputs));
   }

   public void addQuickTransferOption(int inputFromIndex, int inputToIndex, SlotIndexRange... outputs) {
      this.addQuickTransferOption(null, inputFromIndex, inputToIndex, outputs);
   }

   public void addInventoryQuickTransfer(Predicate<ContainerSlot> filter, int fromIndex, int toIndex) {
      this.addQuickTransferOption(filter, this.CLIENT_HOTBAR_START, this.CLIENT_HOTBAR_END, fromIndex, toIndex);
      this.addQuickTransferOption(filter, this.CLIENT_INVENTORY_START, this.CLIENT_INVENTORY_END, fromIndex, toIndex);
      this.addQuickTransferOption(
         filter,
         fromIndex,
         toIndex,
         new SlotIndexRange(this.CLIENT_HOTBAR_START, this.CLIENT_HOTBAR_END),
         new SlotIndexRange(this.CLIENT_INVENTORY_START, this.CLIENT_INVENTORY_END)
      );
   }

   public void addInventoryQuickTransfer(int fromIndex, int toIndex) {
      this.addInventoryQuickTransfer(null, fromIndex, toIndex);
   }

   public int addSlot(ContainerSlot slot) {
      int index = this.slots.size();
      this.craftInventories.add(slot.getInventory());
      this.slots.add(slot);
      slot.init(this, index);
      return index;
   }

   public ContainerSlot getSlot(int index) {
      return index >= 0 && index < this.slots.size() ? this.slots.get(index) : null;
   }

   public void lockSlot(int index) {
      this.lockedSlots.add(index);
   }

   public void lockSlot(PlayerInventorySlot slot) {
      for (int i = 0; i < this.slots.size(); i++) {
         ContainerSlot s = this.slots.get(i);
         if (s.getInventory() instanceof PlayerInventory) {
            PlayerInventory inv = (PlayerInventory)s.getInventory();
            if (inv.getInventoryID() == slot.inventoryID && s.getInventorySlot() == slot.slot) {
               this.lockSlot(i);
            }
         }
      }
   }

   public boolean isSlotLocked(int index) {
      return this.lockedSlots.contains(index);
   }

   public boolean isSlotLocked(ContainerSlot slot) {
      return this.isSlotLocked(slot.getContainerIndex());
   }

   public PlayerInventoryManager getClientInventory() {
      return this.client.playerMob.getInv();
   }

   public ContainerSlot getClientDraggingSlot() {
      return this.getSlot(this.CLIENT_DRAGGING_SLOT);
   }

   public ContainerSlot getClientTrashSlot() {
      return this.getSlot(this.CLIENT_TRASH_SLOT);
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, int startIndex, int stopIndex) {
      return this.transferToSlots(slot, startIndex, stopIndex, Integer.MAX_VALUE);
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, int startIndex, int stopIndex, String purpose) {
      return this.transferToSlots(slot, startIndex, stopIndex, Integer.MAX_VALUE, purpose);
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, Iterable<SlotIndexRange> ranges) {
      return this.transferToSlots(slot, ranges, "transfer");
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, Iterable<SlotIndexRange> ranges, String purpose) {
      return this.transferToSlots(slot, ranges, Integer.MAX_VALUE, purpose);
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, int startIndex, int stopIndex, int amount) {
      return this.transferToSlots(slot, startIndex, stopIndex, amount, "transfer");
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, int startIndex, int stopIndex, int amount, String purpose) {
      return this.transferToSlots(slot, Collections.singleton(new SlotIndexRange(startIndex, stopIndex)), amount, purpose);
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, Iterable<SlotIndexRange> ranges, int amount) {
      return this.transferToSlots(slot, ranges, amount, "transfer");
   }

   public ContainerTransferResult transferToSlots(ContainerSlot slot, Iterable<SlotIndexRange> ranges, int amount, String purpose) {
      String error = null;
      boolean success = false;

      for (int j = 0; j < 2; j++) {
         for (SlotIndexRange range : ranges) {
            for (int i = range.fromIndex; i <= range.toIndex; i++) {
               if (slot.isClear() || amount <= 0) {
                  return new ContainerTransferResult(amount, error);
               }

               ContainerSlot toSlot = this.getSlot(i);
               if (j != 0 || !toSlot.isClear()) {
                  int startAmount = slot.getItemAmount();
                  ItemCombineResult combineResult = toSlot.combineSlots(
                     this.client.playerMob.getLevel(), this.client.playerMob, slot, amount, true, false, purpose
                  );
                  if (combineResult.success) {
                     int amountMoved = startAmount - slot.getItemAmount();
                     amount -= amountMoved;
                     success = true;
                     error = null;
                  } else if (!success && combineResult.error != null) {
                     error = combineResult.error;
                  }
               }
            }
         }
      }

      return new ContainerTransferResult(amount, error);
   }

   public ContainerTransferResult transferFromSlots(ContainerSlot slot, int startIndex, int stopIndex, int amount) {
      return this.transferFromSlots(slot, startIndex, stopIndex, amount, "transfer");
   }

   public ContainerTransferResult transferFromSlots(ContainerSlot slot, int startIndex, int stopIndex, int amount, String purpose) {
      String error = null;

      for (int i = stopIndex; i >= startIndex; i--) {
         if (amount <= 0) {
            return new ContainerTransferResult(amount, error);
         }

         ContainerSlot fromSlot = this.getSlot(i);
         if (!fromSlot.isClear()) {
            int startAmount = fromSlot.getItemAmount();
            ItemCombineResult combineResult = slot.combineSlots(this.client.playerMob.getLevel(), this.client.playerMob, fromSlot, amount, true, false, purpose);
            if (combineResult.success) {
               int amountMoved = startAmount - fromSlot.getItemAmount();
               amount -= amountMoved;
            } else if (combineResult.error != null) {
               error = combineResult.error;
            }
         }
      }

      return new ContainerTransferResult(amount, error);
   }

   public ArrayList<InventoryRange> getNearbyInventories(Level level, int levelCenterX, int levelCenterY, int range) {
      return this.getNearbyInventories(level, levelCenterX, levelCenterY, range, null);
   }

   public ArrayList<InventoryRange> getNearbyInventories(Level level, int levelCenterX, int levelCenterY, int range, Predicate<OEInventory> filter) {
      if (level == null) {
         return new ArrayList<>();
      }

      int tileRange = range / 32;
      int startX = GameMath.getTileCoordinate(levelCenterX) - tileRange - 1;
      int startY = GameMath.getTileCoordinate(levelCenterY) - tileRange - 1;
      int endX = GameMath.getTileCoordinate(levelCenterX) + tileRange + 1;
      int endY = GameMath.getTileCoordinate(levelCenterY) + tileRange + 1;
      ArrayList<InventoryRange> targets = new ArrayList<>();

      for (int y = startY; y <= endY; y++) {
         if (level.isTileYWithinBounds(y)) {
            for (int x = startX; x <= endX; x++) {
               if (level.isTileXWithinBounds(x) && new Point(levelCenterX, levelCenterY).distance(x * 32 + 16, y * 32 + 16) <= range) {
                  ObjectEntity ent = level.entityManager.getObjectEntity(x, y);
                  if (ent instanceof OEInventory && (filter == null || filter.test((OEInventory)ent))) {
                     Inventory inventory = ((OEInventory)ent).getInventory();
                     if (!targets.stream().anyMatch(i -> i.inventory == inventory)) {
                        targets.add(new InventoryRange(inventory));
                     }
                  }
               }
            }
         }
      }

      return targets;
   }

   public ArrayList<InventoryRange> getNearbyInventories(Level level, int centerTileX, int centerTileY, GameTileRange range, Predicate<OEInventory> filter) {
      if (level == null) {
         return new ArrayList<>();
      }

      ArrayList<InventoryRange> targets = new ArrayList<>();

      for (Point tile : range.getValidTiles(centerTileX, centerTileY)) {
         ObjectEntity ent = level.entityManager.getObjectEntity(tile.x, tile.y);
         if (ent instanceof OEInventory && (filter == null || filter.test((OEInventory)ent))) {
            Inventory inventory = ((OEInventory)ent).getInventory();
            if (!targets.stream().anyMatch(i -> i.inventory == inventory)) {
               targets.add(new InventoryRange(inventory));
            }
         }
      }

      return targets;
   }

   public void quickStackPlayerInventory() {
      ArrayList<InventoryRange> targets = this.getNearbyInventories(
         this.client.playerMob.getLevel(), this.client.playerMob.getX(), this.client.playerMob.getY(), 192, OEInventory::canQuickStackInventory
      );
      this.quickStackToInventories(targets, this.client.playerMob.getInv().main);
   }

   public void quickStackToInventories(ArrayList<InventoryRange> targets, InventoryRange inventory) {
      for (int i = inventory.startSlot; i <= inventory.endSlot; i++) {
         if (!inventory.inventory.isSlotClear(i) && !inventory.inventory.isItemLocked(i)) {
            for (InventoryRange target : targets) {
               if (target.inventory
                     .getAmount(
                        this.client.playerMob.getLevel(),
                        this.client.playerMob,
                        inventory.inventory.getItemSlot(i),
                        target.startSlot,
                        target.endSlot,
                        "quickstackto"
                     )
                  > 0) {
                  int lastAmount = inventory.inventory.getAmount(i);
                  target.inventory
                     .addItem(
                        this.client.playerMob.getLevel(),
                        this.client.playerMob,
                        inventory.inventory.getItem(i),
                        target.startSlot,
                        target.endSlot,
                        "quickstackto",
                        null
                     );
                  if (lastAmount != inventory.inventory.getAmount(i)) {
                     inventory.inventory.markDirty(i);
                  }

                  if (inventory.inventory.getAmount(i) <= 0) {
                     inventory.inventory.clearSlot(i);
                     break;
                  }
               }
            }
         }
      }
   }

   public void quickStackToInventories(ArrayList<InventoryRange> targets, Inventory inventory) {
      this.quickStackToInventories(targets, new InventoryRange(inventory));
   }

   public void restockPlayerInventory() {
      ArrayList<InventoryRange> targets = this.getNearbyInventories(
         this.client.playerMob.getLevel(), this.client.playerMob.getX(), this.client.playerMob.getY(), 192, OEInventory::canRestockInventory
      );
      this.restockFromInventories(targets, this.client.playerMob.getInv().main);
   }

   public void restockFromInventories(ArrayList<InventoryRange> targets, InventoryRange inventory) {
      for (InventoryRange target : targets) {
         for (int i = target.startSlot; i <= target.endSlot; i++) {
            if (!target.inventory.isSlotClear(i) && !target.inventory.isItemLocked(i)) {
               InventoryItem item = target.inventory.getItem(i);
               if (inventory.inventory
                  .restockFrom(
                     this.client.playerMob.getLevel(), this.client.playerMob, item, inventory.startSlot, inventory.endSlot, "restockfrom", false, null
                  )) {
                  if (item.getAmount() <= 0) {
                     target.inventory.setItem(i, null);
                  }

                  target.inventory.updateSlot(i);
               }
            }
         }
      }
   }

   public void restockFromInventories(ArrayList<InventoryRange> targets, Inventory inventory) {
      this.restockFromInventories(targets, new InventoryRange(inventory));
   }

   public final void runCustomAction(int id, PacketReader reader) {
      this.actionRegistry.runAction(id, reader);
   }

   public final <T extends ContainerCustomAction> T registerAction(T action) {
      return this.actionRegistry.registerAction(action);
   }

   public final <T extends ContainerEvent> void subscribeEvent(Class<T> eventClass, final Predicate<T> shouldReceiveEvent, final BooleanSupplier isActive) {
      this.eventSubscriptions.removeIf(s -> !s.isActive());
      this.eventSubscriptions.add(new ContainerEventSubscription<T>(eventClass) {
         @Override
         public boolean shouldReceiveEvent(T event) {
            return shouldReceiveEvent.test(event);
         }

         @Override
         public boolean isActive() {
            return isActive.getAsBoolean();
         }
      });
   }

   public final <T extends ContainerEvent> ContainerEventHandler<T> onEvent(Class<T> eventClass, ContainerEventHandler<T> handler) {
      if (handler.isDisposed()) {
         return handler;
      }

      handler.init(eventClass, this.eventHandlers.addLast(eventClass, handler));
      return handler;
   }

   public final <T extends ContainerEvent> ContainerEventHandler<T> onEvent(Class<T> eventClass, final Consumer<T> handler, final BooleanSupplier isActive) {
      return this.onEvent(eventClass, new ContainerEventHandler<T>() {
         @Override
         public void handleEvent(T event) {
            if (!isActive.getAsBoolean()) {
               this.dispose();
            } else {
               handler.accept(event);
            }
         }
      });
   }

   public final <T extends ContainerEvent> ContainerEventHandler<T> onEvent(Class<T> eventClass, final Consumer<T> handler) {
      return this.onEvent(eventClass, new ContainerEventHandler<T>() {
         @Override
         public void handleEvent(T event) {
            handler.accept(event);
         }
      });
   }

   public final boolean shouldReceiveEvent(ContainerEvent event) {
      boolean out = false;
      ListIterator<ContainerEventSubscription<?>> li = this.eventSubscriptions.listIterator();

      while (li.hasNext()) {
         ContainerEventSubscription<?> next = li.next();
         if (!next.isActive()) {
            li.remove();
         } else if (next.testUntypedEvent(event)) {
            out = true;
         }
      }

      return out;
   }

   public final void handleEvent(ContainerEvent event) {
      GameLinkedList<ContainerEventHandler<?>> handlers = this.eventHandlers.get(event.getClass());
      GameLinkedList<ContainerEventHandler<?>>.Element current = handlers.getFirstElement();

      while (current != null) {
         GameLinkedList<ContainerEventHandler<?>>.Element next = current.next();
         ((ContainerEventHandler)current.object).handleEventUntyped(event);
         current = next;
      }
   }

   public int applyCraftingAction(int recipeID, int recipeHash, int craftAmount, boolean transferToInventory) {
      Recipe r = this.getRecipe(recipeID);
      if (r == null) {
         GameLog.warn.println(this.client.playerMob.getDisplayName() + " tried to craft a not existing recipe with id " + recipeID);
         return 0;
      }

      ContainerSlot draggingSlot = this.getClientDraggingSlot();
      if (recipeHash != r.getRecipeHash()) {
         return 0;
      }

      Collection<Inventory> invList = this.getCraftInventories();
      int crafted = 0;

      for (int i = 0; i < craftAmount && this.canCraftRecipe(r, invList, false).canCraft(); i++) {
         ContainerRecipeCraftedEvent event = new ContainerRecipeCraftedEvent(r, r.craft(this.client.playerMob.getLevel(), this.client.playerMob, invList), this);
         r.submitCraftedEvent(event);
         InventoryItem resultItem = event.resultItem;
         InventoryItem addDraggingItem = resultItem;
         InventoryItem addInventoryItem = null;
         if (transferToInventory) {
            int canAdd = this.client.playerMob.getInv().canAddItem(resultItem, true, "crafting");
            if (canAdd >= 0) {
               int remaining = resultItem.getAmount() - canAdd;
               if (remaining > 0) {
                  addDraggingItem = resultItem.copy(remaining);
                  addInventoryItem = resultItem.copy(canAdd);
               } else {
                  addInventoryItem = resultItem;
                  addDraggingItem = null;
               }
            }
         }

         if (addDraggingItem != null
            && !draggingSlot.isClear()
            && (
               !draggingSlot.getItem().canCombine(this.client.playerMob.getLevel(), this.client.playerMob, addDraggingItem, "crafting")
                  || draggingSlot.getItemAmount() + addDraggingItem.getAmount() > draggingSlot.getItemStackLimit(draggingSlot.getItem())
            )) {
            event.itemsUsed.forEach(InventoryItemsRemoved::revert);
            break;
         }

         boolean added = false;
         crafted++;
         if (addInventoryItem != null) {
            addInventoryItem.setNew(true);
            this.client.playerMob.getInv().addItem(addInventoryItem, true, "crafting", null);
            added = true;
         }

         if (addDraggingItem != null) {
            if (draggingSlot.isClear()) {
               draggingSlot.setItem(addDraggingItem);
            } else {
               draggingSlot.getItem()
                  .combine(
                     this.client.playerMob.getLevel(),
                     this.client.playerMob,
                     draggingSlot.getInventory(),
                     draggingSlot.getInventorySlot(),
                     addDraggingItem,
                     "crafting",
                     null
                  );
            }

            added = true;
         }

         if (!added) {
            event.itemsUsed.forEach(InventoryItemsRemoved::revert);
         } else {
            draggingSlot.markDirty();
         }
      }

      if (crafted > 0 && this.client.isServer()) {
         ServerClient serverClient = this.client.getServerClient();
         serverClient.newStats.crafted_items.increment(crafted);
         int finalCrafted = crafted;
         JournalChallengeRegistry.handleListeners(
            serverClient, CraftedRecipeJournalChallengeListener.class, challenge -> challenge.onCraftedRecipe(serverClient, r, finalCrafted)
         );
         if (!serverClient.adventureParty.isEmpty()) {
            for (HumanMob settler : serverClient.adventureParty.getMobs()) {
               PlayerRomanceManager romanceManager = settler.getRomanceManager(serverClient, false);
               if (romanceManager != null) {
                  romanceManager.onCraftedRecipe(serverClient, r, crafted);
               }
            }
         }
      }

      return crafted;
   }

   private int addRecipe(Recipe recipe, boolean isInventory) {
      ContainerRecipe cRecipe = new ContainerRecipe(this.recipes.size(), recipe, isInventory);
      this.recipes.add(cRecipe);
      return cRecipe.id;
   }

   public int addRecipe(Recipe recipe) {
      return this.addRecipe(recipe, false);
   }

   public void addRecipes(Collection<Recipe> recipeList) {
      recipeList.forEach(this::addRecipe);
   }

   public Recipe getRecipe(int id) {
      return this.recipes.size() <= id ? null : this.recipes.get(id).recipe;
   }

   public Stream<ContainerRecipe> streamRecipes(Tech... techs) {
      return this.recipes.stream().filter(cRecipe -> Arrays.stream(techs).anyMatch(cRecipe.recipe::matchTech));
   }

   public CanCraft canCraftRecipe(Recipe recipe, Collection<Inventory> invList, boolean countAllIngredients) {
      return recipe.canCraft(this.client.playerMob.getLevel(), this.client.playerMob, invList, countAllIngredients);
   }

   public CanCraft canCraftRecipe(Ingredient[] ingredients, Collection<Inventory> invList, boolean countAllIngredients, Predicate<Item> ingredientItemFilter) {
      return Recipe.canCraft(ingredients, this.client.playerMob.getLevel(), this.client.playerMob, invList, countAllIngredients, ingredientItemFilter);
   }

   public boolean doesShowRecipe(Recipe recipe, Collection<Inventory> invList) {
      return recipe.doesShow(this.client.playerMob.getLevel(), this.client.playerMob, invList);
   }

   public void markFullDirty() {
      this.getCraftInventories().forEach(Inventory::markFullDirty);
   }

   public Collection<Inventory> getCraftInventories() {
      return this.craftInventories;
   }

   public boolean isValid(ServerClient client) {
      return !this.shouldClose;
   }

   public void close() {
      this.shouldClose = true;
   }

   public void onClose() {
      this.isClosed = true;
      if (this.client.isClient()) {
         GlobalData.updateCraftable();
      }
   }

   public boolean isClosed() {
      return this.isClosed;
   }

   public NetworkClient getClient() {
      return this.client;
   }

   protected static class QuickTransferOption {
      public final Predicate<ContainerSlot> filter;
      public final boolean onlyIfNoOtherFilterValid;
      public final SlotIndexRange input;
      public final SlotIndexRange[] outputs;

      public QuickTransferOption(Predicate<ContainerSlot> filter, boolean onlyIfNoOtherFilterValid, SlotIndexRange input, SlotIndexRange... outputs) {
         this.filter = filter;
         this.onlyIfNoOtherFilterValid = onlyIfNoOtherFilterValid;
         this.input = input;
         this.outputs = outputs;
      }
   }
}
