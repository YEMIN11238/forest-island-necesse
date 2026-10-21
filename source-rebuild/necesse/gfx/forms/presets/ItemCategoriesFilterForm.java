package necesse.gfx.forms.presets;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import necesse.engine.GlobalData;
import necesse.engine.ItemCategoryExpandedSetting;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.playerStats.stats.ItemsObtainedStat;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormContentIconValueButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormItemPreview;
import necesse.gfx.forms.components.FormMouseHover;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.floatMenu.FormFloatMenu;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonIcon;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemSearchTester;
import necesse.inventory.itemFilter.ItemCategoriesFilter;

public abstract class ItemCategoriesFilterForm extends Form {
   public final ItemCategoriesFilter filter;
   public final ItemCategoriesFilterForm.Mode mode;
   private ItemCategoryExpandedSetting expandedSetting;
   private final ItemsObtainedStat itemsObtainedStats;
   private final ItemCategoriesFilterForm.LinkedForm contentForm;
   private final HashMap<Integer, ItemCategoriesFilterForm.ItemForm> itemForms = new HashMap<>();
   private final HashMap<Integer, ItemCategoriesFilterForm.CategoryForm> categoryForms = new HashMap<>();

   public ItemCategoriesFilterForm(
      int x,
      int y,
      ItemCategoriesFilter filter,
      ItemCategoriesFilterForm.Mode mode,
      ItemCategoryExpandedSetting expandedSetting,
      ItemsObtainedStat itemsObtainedStats,
      boolean collapseEmpty
   ) {
      super(0, 0);
      this.drawBase = false;
      this.filter = filter;
      this.mode = mode;
      this.expandedSetting = expandedSetting;
      this.itemsObtainedStats = itemsObtainedStats;
      this.setPosition(x, y);
      this.contentForm = this.addComponent(new ItemCategoriesFilterForm.LinkedForm("master", 0, 0, null, null));
      this.contentForm.drawBase = false;
      ItemCategoriesFilter.ItemCategoryFilter startCategory = filter.master;
      if (collapseEmpty) {
         while (true) {
            List<ItemCategoriesFilter.ItemCategoryFilter> childrenWithItems = startCategory.getChildren()
               .stream()
               .filter(c -> !this.isCategoryEmptyRecursive(c))
               .collect(Collectors.toList());
            if (childrenWithItems.size() != 1) {
               break;
            }

            ItemCategoriesFilter.ItemCategoryFilter next = childrenWithItems.get(0);
            startCategory = next;
            this.expandedSetting = expandedSetting == null ? null : expandedSetting.getChild(next.category);
         }
      }

      if (startCategory.parent != null) {
         startCategory = startCategory.parent;
      }

      this.addChildren(this.contentForm, startCategory, expandedSetting);
      this.contentForm.updateYChildren();
      this.contentForm.updateDimensionsForward();
      this.updateDimensions();
   }

   public void onItemsChanged(Item[] items, boolean allowed) {
   }

   public void onItemLimitsChanged(Item item, ItemCategoriesFilter.ItemLimits limits) {
   }

   public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
   }

   public void onCategoryLimitsChanged(ItemCategoriesFilter.ItemCategoryFilter category, int maxItems) {
   }

   public void onItemClicked(Item item) {
   }

   public abstract void onDimensionsChanged(int var1, int var2);

   public void updateDimensions() {
      boolean update = false;
      int lastWidth = this.getWidth();
      int newWidth = this.contentForm.getWidth();
      if (lastWidth != newWidth) {
         update = true;
         this.setWidth(newWidth);
      }

      int lastHeight = this.getHeight();
      int newHeight = this.contentForm.getHeight();
      if (lastHeight != newHeight) {
         update = true;
         this.setHeight(newHeight);
      }

      if (update) {
         this.onDimensionsChanged(newWidth, newHeight);
      }
   }

   public void updateAllButtons() {
      for (ItemCategoriesFilterForm.ItemForm value : this.itemForms.values()) {
         value.updateButtonsForward();
         value.updateButtonsBack();
      }
   }

   public void updateButton(int itemID) {
      ItemCategoriesFilterForm.ItemForm itemForm = this.itemForms.get(itemID);
      if (itemForm != null) {
         itemForm.updateButtonsBack();
      }
   }

   public void updateButtons(ItemCategory category) {
      ItemCategoriesFilterForm.CategoryForm categoryForm = this.categoryForms.get(category.id);
      if (categoryForm != null) {
         categoryForm.updateButtonsForward();
         categoryForm.updateButtonsBack();
      }
   }

   public void setSearch(String searchString) {
      this.contentForm.updateSearch(searchString);
      this.contentForm.updateYChildren();
      this.contentForm.updateDimensionsForward();
      if (searchString != null && !searchString.isEmpty()) {
         this.contentForm.setExpandedSetting(new ItemCategoryExpandedSetting(true));
      } else {
         this.contentForm.setExpandedSetting(this.expandedSetting);
      }

      this.updateDimensions();
   }

   private boolean isCategoryEmptyRecursive(ItemCategoriesFilter.ItemCategoryFilter category) {
      return !category.hasAnyItems ? category.getChildren().stream().allMatch(this::isCategoryEmptyRecursive) : false;
   }

   protected void addChildren(
      ItemCategoriesFilterForm.LinkedForm form, ItemCategoriesFilter.ItemCategoryFilter itemCategoryFilter, ItemCategoryExpandedSetting lastExpandedSetting
   ) {
      FormFlow flow = new FormFlow();
      AtomicReference<ItemCategoriesFilterForm.LinkedForm> last = new AtomicReference<>();
      itemCategoryFilter.getChildren().stream().sorted().forEach(c -> {
         ItemCategoryExpandedSetting nextSetting = lastExpandedSetting == null ? null : lastExpandedSetting.getChild(c.category);
         ItemCategoriesFilterForm.CategoryForm next = form.addComponent(new ItemCategoriesFilterForm.CategoryForm(form, c, nextSetting, last.get()));
         form.children.add(next);
         last.set(next);
         next.setPosition(0, flow.next(next.isHidden() ? 0 : next.getHeight()));
         form.setHeight(flow.next());
      });
      LinkedList<Item> knownItems = new LinkedList<>();
      LinkedList<Item> unknownItems = new LinkedList<>();
      itemCategoryFilter.streamItems()
         .sorted(Comparator.comparing(i -> ItemRegistry.getDisplayName(i.getID())))
         .forEach(
            i -> {
               if (!GlobalData.debugCheatActive()
                  && (this.itemsObtainedStats == null || !this.itemsObtainedStats.isItemObtained(i.getStringID()))
                  && !GlobalData.stats().items_obtained.isItemObtained(i.getStringID())) {
                  unknownItems.add(i);
               } else {
                  knownItems.add(i);
               }
            }
         );

      for (Item item : knownItems) {
         ItemCategoriesFilterForm.ItemForm itemForm = form.addComponent(
            new ItemCategoriesFilterForm.ItemForm(item.getStringID(), form, last.get(), itemCategoryFilter, item)
         );
         form.children.add(itemForm);
         itemForm.setPosition(0, flow.next(itemForm.isHidden() ? 0 : itemForm.getHeight()));
         form.setHeight(flow.next());
         last.set(itemForm);
         form.markHasItems();
      }

      if (!unknownItems.isEmpty()) {
         ItemCategoriesFilterForm.ItemForm itemForm = form.addComponent(
            new ItemCategoriesFilterForm.ItemForm(
               GameUtils.join(unknownItems.toArray(new Item[0]), Item::getStringID, "."), form, last.get(), itemCategoryFilter, unknownItems
            )
         );
         form.children.add(itemForm);
         itemForm.setPosition(0, flow.next(itemForm.isHidden() ? 0 : itemForm.getHeight()));
         form.setHeight(flow.next());
         form.markHasItems();
      }
   }

   protected class CategoryForm extends ItemCategoriesFilterForm.LinkedForm {
      public final ItemCategoriesFilter.ItemCategoryFilter itemCategoryFilter;
      private final ItemCategoriesFilterForm.LinkedForm childrenForm;
      private final FormContentIconValueButton<ItemCategoriesFilterForm.CheckedState> toggleButton;
      private ItemCategoriesFilterForm.CheckedState lastState;
      private int lastMaxItems;
      private final FormContentIconButton expandButton;
      private final FormLocalLabel label;
      private final FormMouseHover labelHover;
      private ItemCategoryExpandedSetting expandedSetting;
      private boolean isExpanded = false;

      public CategoryForm(
         ItemCategoriesFilterForm.LinkedForm parent,
         final ItemCategoriesFilter.ItemCategoryFilter itemCategoryFilter,
         ItemCategoryExpandedSetting expandedSetting,
         ItemCategoriesFilterForm.LinkedForm lastForm
      ) {
         super(itemCategoryFilter.category.stringID, 20, 20, parent, lastForm);
         this.itemCategoryFilter = itemCategoryFilter;
         this.expandedSetting = expandedSetting;
         ItemCategoriesFilterForm.this.categoryForms.put(itemCategoryFilter.category.id, this);
         this.drawBase = false;
         this.childrenForm = this.addComponent(new ItemCategoriesFilterForm.LinkedForm(this.name + " children", 20, 0, this, null));
         this.children.add(this.childrenForm);
         this.childrenForm.drawBase = false;
         this.childrenForm.setPosition(20, 20);
         this.childrenForm.setHidden(true);
         int currentX = 0;
         if (ItemCategoriesFilterForm.this.mode != ItemCategoriesFilterForm.Mode.ONLY_CLICK_ITEM) {
            this.toggleButton = this.addComponent(new FormContentIconValueButton<>(currentX, 0, FormInputSize.SIZE_20, ButtonColor.BASE));
            ItemCategoriesFilterForm.CheckedState.CHECKED.updateButton(this.toggleButton);
            this.toggleButton.onClicked(e -> {
               if (this.lastState != ItemCategoriesFilterForm.CheckedState.CHECKED && this.lastState != ItemCategoriesFilterForm.CheckedState.DASH) {
                  itemCategoryFilter.setAllowed(true);
                  ItemCategoriesFilterForm.this.onCategoryChanged(itemCategoryFilter, true);
               } else {
                  itemCategoryFilter.setAllowed(false);
                  ItemCategoriesFilterForm.this.onCategoryChanged(itemCategoryFilter, false);
               }

               this.updateButtonsForward();
               this.updateButtonsBack();
            });
            this.toggleButton
               .setupDragToOtherButtons(
                  "pressItemCategory" + (itemCategoryFilter.parent == null ? "Null" : itemCategoryFilter.parent.category.stringID), true, state -> {
                     if (state != ItemCategoriesFilterForm.CheckedState.CHECKED && state != ItemCategoriesFilterForm.CheckedState.ESCAPED) {
                        return false;
                     }

                     itemCategoryFilter.setAllowed(state == ItemCategoriesFilterForm.CheckedState.CHECKED);
                     ItemCategoriesFilterForm.this.onCategoryChanged(itemCategoryFilter, state == ItemCategoriesFilterForm.CheckedState.CHECKED);
                     this.updateButtonsForward();
                     this.updateButtonsBack();
                     return true;
                  }
               );
            currentX += 20;
         } else {
            this.toggleButton = null;
         }

         this.updateButton();
         this.expandButton = this.addComponent(
            new FormContentIconButton(currentX, 0, FormInputSize.SIZE_20, ButtonColor.BASE, this.getInterfaceStyle().button_collapsed_16)
         );
         this.expandButton.onClicked(e -> this.setExpanded(!this.isExpanded));
         currentX += 20;
         this.label = this.addComponent(new FormLocalLabel(this.getLabelText(), new FontOptions(16), -1, currentX + 2, 2));
         Rectangle labelBoundingBox = this.label.getBoundingBox();
         if (ItemCategoriesFilterForm.this.mode == ItemCategoriesFilterForm.Mode.ALLOW_MAX_AMOUNT) {
            this.labelHover = this.addComponent(new FormMouseHover(currentX + 2, 2, labelBoundingBox.width, labelBoundingBox.height) {
               @Override
               public GameTooltips getTooltips(PlayerMob perspective) {
                  ListGameTooltips tooltips = new ListGameTooltips();
                  tooltips.add(new InputTooltip(-100, Localization.translate("ui", "setstoragelimit")));
                  if (!itemCategoryFilter.isDefault()) {
                     tooltips.add(new InputTooltip(-99, Localization.translate("ui", "clearstoragelimit")));
                  }

                  return tooltips;
               }

               @Override
               public GameWindow.CURSOR getHoveringCursor(PlayerMob perspective) {
                  return GameWindow.CURSOR.INTERACT;
               }
            }, 1000);
            this.labelHover.acceptRightClicks = true;
            this.labelHover.onClicked(e -> {
               if (e.event.getID() == -99) {
                  if (!itemCategoryFilter.isDefault()) {
                     itemCategoryFilter.clearMaxItems();
                     ItemCategoriesFilterForm.this.onCategoryLimitsChanged(itemCategoryFilter, itemCategoryFilter.getMaxItems());
                  }
               } else {
                  Form form = new Form(200, 24);
                  FormTextInput limitInput = form.addComponent(new FormTextInput(0, 0, FormInputSize.SIZE_24, form.getWidth(), 7));
                  limitInput.placeHolder = new LocalMessage("ui", "storagelimit");
                  limitInput.setRegexMatchFull("([0-9]+)?");
                  limitInput.rightClickToClear = true;
                  limitInput.setText(itemCategoryFilter.isDefault() ? "" : "" + itemCategoryFilter.getMaxItems());
                  FormFloatMenu floatMenu = new FormFloatMenu(parent, form);
                  this.getManager().openFloatMenu(floatMenu, -10, -12);
                  limitInput.onSubmit(e2 -> {
                     try {
                        int next;
                        if (limitInput.getText().isEmpty()) {
                           next = Integer.MAX_VALUE;
                        } else {
                           next = Integer.parseInt(limitInput.getText());
                        }

                        if (next != itemCategoryFilter.getMaxItems()) {
                           itemCategoryFilter.setMaxItems(next);
                           ItemCategoriesFilterForm.this.onCategoryLimitsChanged(itemCategoryFilter, next);
                        }

                        floatMenu.remove();
                     } catch (NumberFormatException var6x) {
                     }
                  });
                  limitInput.setTyping(true);
               }
            });
         } else {
            this.labelHover = null;
         }

         this.setWidth(currentX + labelBoundingBox.width + 10);
         ItemCategoriesFilterForm.this.addChildren(this.childrenForm, itemCategoryFilter, this.expandedSetting);
         if (this.expandedSetting != null && this.expandedSetting.isExpanded()) {
            this.setExpanded(true);
         }

         this.setHidden(!this.hasItems);
      }

      @Override
      public void setExpandedSetting(ItemCategoryExpandedSetting setting) {
         this.expandedSetting = setting == null ? null : setting.getChild(this.itemCategoryFilter.category);
         if (this.expandedSetting != null) {
            this.setExpanded(this.expandedSetting.isExpanded());

            for (ItemCategoriesFilterForm.LinkedForm child : this.children) {
               child.setExpandedSetting(this.expandedSetting);
            }
         }
      }

      public void setExpanded(boolean expanded) {
         if (this.isExpanded != expanded) {
            this.isExpanded = expanded;
            if (this.expandedSetting != null) {
               this.expandedSetting.setExpanded(expanded);
            }

            if (expanded) {
               this.expandButton.setIcon(this.getInterfaceStyle().button_expanded_16);
               this.childrenForm.setHidden(false);
               this.childrenForm.updateDimensionsBack();
            } else {
               this.expandButton.setIcon(this.getInterfaceStyle().button_collapsed_16);
               this.childrenForm.setHidden(true);
               this.childrenForm.updateDimensionsBack();
            }

            this.updateY();
            if (this.parent != null) {
               this.parent.updateDimensionsBack();
            }

            ItemCategoriesFilterForm.this.updateDimensions();
         }
      }

      @Override
      public void fixDimensions() {
         int childrenWidth = this.children.stream().filter(c -> !c.isHidden()).map(c -> c.getX() + c.getWidth()).max(Comparator.comparingInt(i -> i)).orElse(0);
         this.setWidth(Math.max(childrenWidth, 40 + this.label.getBoundingBox().width + 10));
         this.setHeight(this.children.stream().filter(c -> !c.isHidden()).map(c -> c.getY() + c.getHeight()).max(Comparator.comparingInt(i -> i)).orElse(20));
      }

      @Override
      public void updateSearch(String searchString) {
         super.updateSearch(searchString);
         this.setHidden(!this.hasItems || this.childrenForm.children.stream().allMatch(Form::isHidden));
      }

      @Override
      public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
         ItemCategoriesFilterForm.CheckedState nextState = this.getNextState();
         if (this.lastState != nextState) {
            if (this.parent != null) {
               this.parent.updateButtonsBack();
            }

            this.updateButtonsForward();
         }

         int nextMaxItems = this.itemCategoryFilter.getMaxItems();
         if (nextMaxItems != this.lastMaxItems) {
            this.updateLabel();
         }

         super.draw(tickManager, perspective, renderBox);
      }

      @Override
      public void updateButton() {
         this.lastState = this.getNextState();
         if (this.toggleButton != null) {
            this.lastState.updateButton(this.toggleButton);
         }
      }

      public void updateLabel() {
         this.lastMaxItems = this.itemCategoryFilter.getMaxItems();
         this.label.setText(this.getLabelText());
         if (this.labelHover != null) {
            this.labelHover.width = this.label.getBoundingBox().width;
         }

         this.updateDimensionsBack();
         this.updateDimensionsForward();
         ItemCategoriesFilterForm.this.updateDimensions();
         WindowManager.getWindow().submitNextMoveEvent();
      }

      public GameMessage getLabelText() {
         return this.itemCategoryFilter.isDefault()
            ? this.itemCategoryFilter.category.displayName
            : new LocalMessage("ui", "storagelimitprefix", "count", this.itemCategoryFilter.getMaxItems(), "item", this.itemCategoryFilter.category.displayName);
      }

      protected ItemCategoriesFilterForm.CheckedState getNextState() {
         if (this.itemCategoryFilter.isAllAllowed()) {
            return this.itemCategoryFilter.isDefault() && this.itemCategoryFilter.isAllDefault()
               ? ItemCategoriesFilterForm.CheckedState.CHECKED
               : ItemCategoriesFilterForm.CheckedState.DASH;
         } else {
            return this.itemCategoryFilter.isAnyAllowed() ? ItemCategoriesFilterForm.CheckedState.DASH : ItemCategoriesFilterForm.CheckedState.ESCAPED;
         }
      }
   }

   private enum CheckedState {
      CHECKED(() -> Settings.UI.button_checked_20),
      ESCAPED(() -> Settings.UI.button_escaped_20),
      DASH(() -> Settings.UI.button_dash_20);

      public final Supplier<ButtonIcon> iconSupplier;

      CheckedState(Supplier<ButtonIcon> iconSupplier) {
         this.iconSupplier = iconSupplier;
      }

      public void updateButton(FormContentIconValueButton<ItemCategoriesFilterForm.CheckedState> button) {
         button.setCurrent(this, this.iconSupplier.get());
      }
   }

   protected class ItemForm extends ItemCategoriesFilterForm.LinkedForm {
      public final Item[] items;
      public Item[] searchedItems;
      private final FormContentIconValueButton<ItemCategoriesFilterForm.CheckedState> toggleButton;
      private ItemCategoriesFilterForm.CheckedState state;
      private ItemCategoriesFilter.ItemLimits lastLimits;
      private final FormLocalLabel label;
      private final FormMouseHover labelHover;

      public ItemForm(
         String name,
         ItemCategoriesFilterForm.LinkedForm parent,
         ItemCategoriesFilterForm.LinkedForm lastForm,
         ItemCategoriesFilter.ItemCategoryFilter itemCategoryFilter,
         final Item... items
      ) {
         super(name, 20, 20, parent, lastForm);
         this.drawBase = false;

         for (Item item : items) {
            ItemCategoriesFilterForm.this.itemForms.put(item.getID(), this);
         }

         this.items = items;
         this.searchedItems = items;
         int currentX = 0;
         if (ItemCategoriesFilterForm.this.mode != ItemCategoriesFilterForm.Mode.ONLY_CLICK_ITEM) {
            this.toggleButton = this.addComponent(new FormContentIconValueButton<>(currentX, 0, FormInputSize.SIZE_20, ButtonColor.BASE));
            ItemCategoriesFilterForm.CheckedState.CHECKED.updateButton(this.toggleButton);
            this.toggleButton.onClicked(e -> {
               if (this.state != ItemCategoriesFilterForm.CheckedState.CHECKED && this.state != ItemCategoriesFilterForm.CheckedState.DASH) {
                  for (Item itemx : this.searchedItems) {
                     ItemCategoriesFilterForm.this.filter.setItemAllowed(itemx, true);
                  }

                  ItemCategoriesFilterForm.this.onItemsChanged(this.searchedItems, true);
               } else {
                  for (Item itemxx : this.searchedItems) {
                     ItemCategoriesFilterForm.this.filter.setItemAllowed(itemxx, false);
                  }

                  ItemCategoriesFilterForm.this.onItemsChanged(this.searchedItems, false);
               }

               this.updateButtonsBack();
            });
            this.toggleButton.setupDragToOtherButtons("pressItemCategory" + itemCategoryFilter.category.stringID, true, state -> {
               if (state != ItemCategoriesFilterForm.CheckedState.CHECKED && state != ItemCategoriesFilterForm.CheckedState.ESCAPED) {
                  return false;
               }

               for (Item itemx : this.searchedItems) {
                  ItemCategoriesFilterForm.this.filter.setItemAllowed(itemx, state == ItemCategoriesFilterForm.CheckedState.CHECKED);
               }

               ItemCategoriesFilterForm.this.onItemsChanged(this.searchedItems, state == ItemCategoriesFilterForm.CheckedState.CHECKED);
               this.updateButtonsBack();
               return true;
            });
            currentX += 20;
         } else {
            this.toggleButton = null;
         }

         if (items.length == 1) {
            this.addComponent(new FormItemPreview(currentX, 0, 20, items[0]));
            this.label = this.addComponent(new FormLocalLabel(this.getLabelText(), new FontOptions(16), -1, currentX + 20 + 2, 2));
         } else {
            this.label = this.addComponent(new FormLocalLabel(this.getLabelText(), new FontOptions(16), -1, currentX + 2, 2));
         }

         Rectangle labelBoundingBox = this.label.getBoundingBox();
         if (ItemCategoriesFilterForm.this.mode == ItemCategoriesFilterForm.Mode.ALLOW_MAX_AMOUNT) {
            this.labelHover = this.addComponent(new FormMouseHover(currentX + 2, 2, labelBoundingBox.width + 20, labelBoundingBox.height) {
               @Override
               public GameTooltips getTooltips(PlayerMob perspective) {
                  if (items.length == 1) {
                     ListGameTooltips tooltips = new ListGameTooltips();
                     ItemCategoriesFilter.ItemLimits current = ItemCategoriesFilterForm.this.filter.getItemLimits(items[0]);
                     tooltips.add(new InputTooltip(-100, Localization.translate("ui", "setstoragelimit")));
                     if (current != null && !current.isDefault()) {
                        tooltips.add(new InputTooltip(-99, Localization.translate("ui", "clearstoragelimit")));
                     }

                     return tooltips;
                  } else {
                     return null;
                  }
               }

               @Override
               public GameWindow.CURSOR getHoveringCursor(PlayerMob perspective) {
                  return items.length == 1 ? GameWindow.CURSOR.INTERACT : null;
               }
            }, 1000);
            this.labelHover.acceptRightClicks = true;
            this.labelHover.onClicked(e -> {
               if (items.length == 1) {
                  ItemCategoriesFilter.ItemLimits current = ItemCategoriesFilterForm.this.filter.getItemLimits(items[0]);
                  if (e.event.getID() == -99) {
                     if (current != null && !current.isDefault()) {
                        ItemCategoriesFilter.ItemLimits next = new ItemCategoriesFilter.ItemLimits();
                        ItemCategoriesFilterForm.this.filter.setItemAllowed(items[0], next);
                        ItemCategoriesFilterForm.this.onItemLimitsChanged(items[0], next);
                     }
                  } else {
                     Form form = new Form(200, 24);
                     FormTextInput limitInput = form.addComponent(new FormTextInput(0, 0, FormInputSize.SIZE_24, form.getWidth(), 7));
                     limitInput.placeHolder = new LocalMessage("ui", "storagelimit");
                     limitInput.setRegexMatchFull("([0-9]+)?");
                     limitInput.rightClickToClear = true;
                     limitInput.setText(current != null && !current.isDefault() ? "" + current.getMaxItems() : "");
                     FormFloatMenu floatMenu = new FormFloatMenu(parent, form);
                     this.getManager().openFloatMenu(floatMenu, -10, -12);
                     limitInput.onSubmit(e2 -> {
                        try {
                           ItemCategoriesFilter.ItemLimits next;
                           if (limitInput.getText().isEmpty()) {
                              next = current == null ? null : new ItemCategoriesFilter.ItemLimits();
                           } else {
                              next = new ItemCategoriesFilter.ItemLimits(Integer.parseInt(limitInput.getText()));
                           }

                           if (current != next && (current == null || !current.isSame(next))) {
                              ItemCategoriesFilterForm.this.filter.setItemAllowed(items[0], next);
                              ItemCategoriesFilterForm.this.onItemLimitsChanged(items[0], next);
                           }

                           floatMenu.remove();
                        } catch (NumberFormatException var7x) {
                        }
                     });
                     limitInput.setTyping(true);
                  }
               }
            });
         } else if (ItemCategoriesFilterForm.this.mode == ItemCategoriesFilterForm.Mode.ONLY_CLICK_ITEM && items.length == 1) {
            this.labelHover = this.addComponent(new FormMouseHover(currentX + 2, 2, labelBoundingBox.width + 20, labelBoundingBox.height) {
               @Override
               public GameWindow.CURSOR getHoveringCursor(PlayerMob perspective) {
                  return GameWindow.CURSOR.INTERACT;
               }
            }, 1000);
            this.labelHover.onClicked(e -> ItemCategoriesFilterForm.this.onItemClicked(items[0]));
         } else {
            this.labelHover = null;
         }

         this.fixDimensions();
         this.updateButton();
      }

      public ItemForm(
         String name,
         ItemCategoriesFilterForm.LinkedForm parent,
         ItemCategoriesFilterForm.LinkedForm lastForm,
         ItemCategoriesFilter.ItemCategoryFilter itemCategoryFilter,
         List<Item> items
      ) {
         this(name, parent, lastForm, itemCategoryFilter, items.toArray(new Item[0]));
      }

      @Override
      public void fixDimensions() {
         Rectangle labelBox = this.label.getBoundingBox();
         this.setWidth(labelBox.x + labelBox.width + 4);
      }

      @Override
      public void updateSearch(String searchString) {
         super.updateSearch(searchString);
         ItemSearchTester tester = ItemSearchTester.constructSearchTester(searchString);
         this.searchedItems = Arrays.stream(this.items).filter(i -> tester.matches(i.getDefaultItem(null, 1), null, new GameBlackboard())).toArray(Item[]::new);
         if (this.items.length != 1) {
            this.label.setLocalization(new LocalMessage("itemcategory", "unknownitems", "count", this.searchedItems.length));
            this.fixDimensions();
         }

         this.setHidden(this.searchedItems.length == 0);
         this.updateButton();
      }

      @Override
      public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
         ItemCategoriesFilterForm.CheckedState nextState = this.getNextState();
         if (nextState != this.state) {
            this.updateButtonsBack();
         }

         ItemCategoriesFilter.ItemLimits nextLimits = this.items.length == 1 ? ItemCategoriesFilterForm.this.filter.getItemLimits(this.items[0]) : null;
         if (nextLimits != this.lastLimits || nextLimits != null && nextLimits.isSame(this.lastLimits)) {
            this.updateLabel();
         }

         super.draw(tickManager, perspective, renderBox);
      }

      @Override
      public void updateButton() {
         this.state = this.getNextState();
         if (this.toggleButton != null) {
            this.state.updateButton(this.toggleButton);
         }
      }

      public void updateLabel() {
         this.lastLimits = this.items.length == 1 ? ItemCategoriesFilterForm.this.filter.getItemLimits(this.items[0]) : null;
         this.label.setText(this.getLabelText());
         if (this.labelHover != null) {
            this.labelHover.width = this.label.getBoundingBox().width + 20;
         }

         this.updateDimensionsBack();
         this.updateDimensionsForward();
         ItemCategoriesFilterForm.this.updateDimensions();
         WindowManager.getWindow().submitNextMoveEvent();
      }

      public GameMessage getLabelText() {
         if (this.items.length == 1) {
            GameMessage itemName = ItemRegistry.getLocalization(this.items[0].getID());
            return this.lastLimits != null && !this.lastLimits.isDefault()
               ? new LocalMessage("ui", "storagelimitprefix", "count", this.lastLimits.getMaxItems(), "item", itemName)
               : itemName;
         } else {
            return new LocalMessage("itemcategory", "unknownitems", "count", this.items.length);
         }
      }

      protected ItemCategoriesFilterForm.CheckedState getNextState() {
         int itemsAllowed = 0;
         int itemsDefault = 0;

         for (Item item : this.searchedItems) {
            ItemCategoriesFilter.ItemLimits limits = ItemCategoriesFilterForm.this.filter.getItemLimits(item);
            if (limits != null) {
               itemsAllowed++;
               if (limits.isDefault()) {
                  itemsDefault++;
               }
            }
         }

         if (itemsAllowed == 0) {
            return ItemCategoriesFilterForm.CheckedState.ESCAPED;
         } else {
            return itemsAllowed == this.searchedItems.length && itemsDefault == this.searchedItems.length
               ? ItemCategoriesFilterForm.CheckedState.CHECKED
               : ItemCategoriesFilterForm.CheckedState.DASH;
         }
      }
   }

   protected static class LinkedForm extends Form {
      public boolean hasItems;
      public ItemCategoriesFilterForm.LinkedForm parent;
      public ItemCategoriesFilterForm.LinkedForm lastForm;
      public ItemCategoriesFilterForm.LinkedForm nextForm;
      public LinkedList<ItemCategoriesFilterForm.LinkedForm> children = new LinkedList<>();

      public LinkedForm(String name, int width, int height, ItemCategoriesFilterForm.LinkedForm parent, ItemCategoriesFilterForm.LinkedForm lastForm) {
         super(name, width, height);
         this.parent = parent;
         this.lastForm = lastForm;
         if (lastForm != null) {
            lastForm.nextForm = this;
         }
      }

      public void markHasItems() {
         this.hasItems = true;
         if (this.parent != null && !this.parent.hasItems) {
            this.parent.markHasItems();
         }
      }

      public void updateY() {
         if (this.lastForm != null) {
            if (this.lastForm.isHidden()) {
               this.setY(this.lastForm.getY());
            } else {
               this.setY(this.lastForm.getY() + this.lastForm.getHeight());
            }
         }

         if (this.nextForm != null) {
            this.nextForm.updateY();
         }
      }

      public void updateYChildren() {
         if (this.lastForm != null) {
            if (this.lastForm.isHidden()) {
               this.setY(this.lastForm.getY());
            } else {
               this.setY(this.lastForm.getY() + this.lastForm.getHeight());
            }
         }

         for (ItemCategoriesFilterForm.LinkedForm child : this.children) {
            child.updateYChildren();
         }
      }

      public final void updateDimensionsBack() {
         this.fixDimensions();
         this.updateY();
         if (this.parent != null) {
            this.parent.updateDimensionsBack();
         }
      }

      public final void updateDimensionsForward() {
         this.children.forEach(ItemCategoriesFilterForm.LinkedForm::updateDimensionsForward);
         this.fixDimensions();
         this.updateY();
      }

      public void fixDimensions() {
         this.setWidth(this.children.stream().filter(c -> !c.isHidden()).map(c -> c.getX() + c.getWidth()).max(Comparator.comparingInt(i -> i)).orElse(0));
         this.setHeight(this.children.stream().filter(c -> !c.isHidden()).map(c -> c.getY() + c.getHeight()).max(Comparator.comparingInt(i -> i)).orElse(0));
      }

      public void setExpandedSetting(ItemCategoryExpandedSetting setting) {
         for (ItemCategoriesFilterForm.LinkedForm child : this.children) {
            child.setExpandedSetting(setting);
         }
      }

      public void updateSearch(String searchString) {
         this.children.forEach(f -> f.updateSearch(searchString));
      }

      public final void updateButtonsBack() {
         this.updateButton();
         if (this.parent != null) {
            this.parent.updateButtonsBack();
         }
      }

      public final void updateButtonsForward() {
         this.updateButton();

         for (ItemCategoriesFilterForm.LinkedForm child : this.children) {
            child.updateButtonsForward();
         }
      }

      public void updateButton() {
      }
   }

   public enum Mode {
      ALLOW_MAX_AMOUNT,
      ONLY_ALLOWED,
      ONLY_CLICK_ITEM;
   }
}
