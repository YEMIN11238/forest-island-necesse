package necesse.gfx.forms.presets.creative;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketSpawnCreativeItem;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormContainerCreativeItem;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFairTypeButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.position.FormRelativePosition;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemSearchTester;
import necesse.inventory.item.toolItem.miscToolItem.EraserToolItem;
import necesse.inventory.item.toolItem.miscToolItem.PipetteItem;

public class CreativeItemsTab extends CreativeTab {
   private final FormContentBox craftingContent;
   private final FormTextInput searchInput;
   protected final HashMap<ItemCategory, CreativeItemsTab.CategoryForm> categories = new HashMap<>();
   protected final List<CreativeItemsTab.CategoryForm> topCategories = new ArrayList<>();
   private final CreativeItemsTab.SearchThread searchThread;
   protected final FormFairTypeButton pipetteButton;
   protected final FormFairTypeButton eraserButton;
   protected static String searchString = "";
   protected boolean compactMode;
   protected boolean updatePositions;

   public CreativeItemsTab(Form form, Client playerClient, String nameKey, Set<String> includesMasterCategory, Set<String> excludesMasterCategory) {
      super(form, playerClient);
      searchString = "";
      form.addComponent(new FormLocalLabel("ui", nameKey, new FontOptions(20), -1, 5, 5));
      int searchWidth = 150;
      this.searchInput = form.addComponent(new FormTextInput(form.getWidth() - searchWidth - 4, 4, FormInputSize.SIZE_24, searchWidth, -1, 100));
      this.searchInput.rightClickToClear = true;
      this.searchInput.rightClickToClearTooltip = new LocalMessage("controls", "clearsearchtip");
      this.searchInput.placeHolder = new LocalMessage("ui", "searchtip");
      this.searchInput.onChange(e -> this.setSearch(this.searchInput.getText()));
      this.eraserButton = form.addComponent(new FormFairTypeButton(new StaticMessage("[item=eraser]"), 0, 0, 24, FormInputSize.SIZE_24, ButtonColor.BASE));
      this.eraserButton.setParsers(TypeParsers.ItemIcon(11));
      this.eraserButton.setPosition(new FormRelativePosition(this.searchInput, -28, 0));
      this.eraserButton
         .onClicked(
            e -> PacketSpawnCreativeItem.runAndSendAction(
               playerClient,
               new InventoryItem("eraser"),
               Input.lastInputIsController ? PacketSpawnCreativeItem.Destination.Hotbar : PacketSpawnCreativeItem.Destination.DragSlot,
               true,
               false
            )
         );
      this.pipetteButton = form.addComponent(new FormFairTypeButton(new StaticMessage("[item=pipette]"), 0, 0, 24, FormInputSize.SIZE_24, ButtonColor.BASE));
      this.pipetteButton.setParsers(TypeParsers.ItemIcon(11));
      this.pipetteButton.setPosition(new FormRelativePosition(this.eraserButton, -28, 0));
      this.pipetteButton
         .onClicked(
            e -> PacketSpawnCreativeItem.runAndSendAction(
               playerClient,
               new InventoryItem("pipette"),
               Input.lastInputIsController ? PacketSpawnCreativeItem.Destination.Hotbar : PacketSpawnCreativeItem.Destination.DragSlot,
               true,
               false
            )
         );
      this.craftingContent = form.addComponent(new FormContentBox(0, 32, form.getWidth(), form.getHeight() - 4 - 32));
      this.addItems(includesMasterCategory, excludesMasterCategory);
      this.searchThread = new CreativeItemsTab.SearchThread();
      this.searchThread.start();
   }

   @Override
   public void updateBeforeDraw(TickManager tickManager) {
      if (this.updatePositions) {
         FormFlow flow = new FormFlow(0);

         for (CreativeItemsTab.CategoryForm category : this.topCategories) {
            category.updateContentPositions(0);
            if (!category.isHidden()) {
               flow.nextY(category);
            }
         }

         this.craftingContent.setContentBox(new Rectangle(this.craftingContent.getWidth(), flow.next()));
         this.updatePositions = false;
         WindowManager.getWindow().submitNextMoveEvent();
      }

      if (this.eraserButton.isHovering()) {
         GameTooltipManager.addTooltip(
            new StringTooltips(new LocalMessage("item", "eraser").translate() + "\n" + EraserToolItem.getToolTip().translate()), TooltipLocation.FORM_FOCUS
         );
      }

      if (this.pipetteButton.isHovering()) {
         GameTooltipManager.addTooltip(
            new StringTooltips(new LocalMessage("item", "pipette").translate() + "\n" + PipetteItem.getToolTip().translate()), TooltipLocation.FORM_FOCUS
         );
      }
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleInputEvent(event, tickManager, perspective);
      if (event.state && WindowManager.getWindow().isKeyDown(341) && event.getID() == 70) {
         event.use();
         this.searchInput.setTyping(true);
         this.searchInput.selectAll();
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleControllerEvent(event, tickManager, perspective);
   }

   @Override
   public void tabFocused() {
      super.tabFocused();
      this.searchInput.setText(searchString, true);
      this.searchInput.setTyping(false);
   }

   @Override
   public void onTabUnfocused() {
      super.onTabUnfocused();
      this.searchInput.setTyping(false);
   }

   public void setSearch(String string) {
      if (string == null) {
         string = "";
      }

      searchString = string;
      this.searchThread.queryTasks.add(searchString);
   }

   private void addItems(Set<String> includesMasterCategory, Set<String> excludesMasterCategory) {
      List<ItemCategory> itemCategories;
      if (includesMasterCategory == null) {
         itemCategories = ItemCategory.masterCategory
            .streamChildren()
            .filter(categoryx -> excludesMasterCategory == null || !excludesMasterCategory.contains(categoryx.stringID))
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
      } else if (includesMasterCategory.size() == 1) {
         String category = includesMasterCategory.stream().findFirst().orElse(null);
         itemCategories = ItemCategory.getCategory(category).streamChildren().sorted().collect(Collectors.toCollection(ArrayList::new));
      } else {
         itemCategories = includesMasterCategory.stream().map(xva$0 -> ItemCategory.getCategory(xva$0)).sorted().collect(Collectors.toList());
      }

      itemCategories.sort(Comparator.comparing(o -> (ItemCategory)o));
      boolean addBreakLine = false;

      for (ItemCategory category : itemCategories) {
         CreativeItemsTab.CategoryForm categoryForm = this.createCategoryForm(category, addBreakLine, true);
         this.craftingContent.addComponent(categoryForm);
         this.topCategories.add(categoryForm);
         addBreakLine = true;
      }
   }

   private CreativeItemsTab.CategoryForm createCategoryForm(ItemCategory category, boolean addBreakLine, boolean isTopCategory) {
      List<Item> itemsInCategory = category.getItems().stream().filter(ItemRegistry::isValidCreativeItem).collect(Collectors.toList());
      ArrayList<CreativeItemsTab.CategoryForm> subCategories = new ArrayList<>();
      category.getChildren().forEach(subCategory -> {
         if (subCategory.getItemCountIncludingChildren() > 0) {
            subCategories.add(this.createCategoryForm(subCategory, true, false));
         }
      });
      subCategories.sort(Comparator.comparing(o -> o.category));
      CreativeItemsTab.CategoryForm categoryForm = new CreativeItemsTab.CategoryForm(
         category.stringID + "CraftingForm",
         this.craftingContent.getWidth() - 8 - this.craftingContent.getScrollBarWidth(),
         addBreakLine,
         isTopCategory,
         category,
         itemsInCategory,
         subCategories
      );
      this.categories.put(category, categoryForm);
      return categoryForm;
   }

   @Override
   public void dispose() {
      super.dispose();
      this.searchThread.interrupt();
   }

   public class CategoryForm extends Form {
      private final boolean addBreakLine;
      private final int fullWidth;
      public final boolean isTopLevel;
      public final ItemCategory category;
      private final FormContentIconButton expandButton;
      private final FormComponentList content;
      private final List<FormContainerCreativeItem> itemComponents;
      private final List<CreativeItemsTab.CategoryForm> categoryComponents;
      private final FormComponentList categoryHider;
      private int contentHeight;
      private boolean isExpanded = false;

      public CategoryForm(
         String name,
         int width,
         boolean addBreakLine,
         boolean isTopLevel,
         ItemCategory category,
         List<Item> categoryItems,
         List<CreativeItemsTab.CategoryForm> categoryComponents
      ) {
         super(name, width - category.depth * 16, 100);
         this.addBreakLine = addBreakLine;
         this.category = category;
         this.drawBase = false;
         this.categoryComponents = categoryComponents;
         this.fullWidth = width;
         this.isTopLevel = isTopLevel;
         this.categoryHider = this.addComponent(new FormComponentList());
         if (addBreakLine) {
            this.categoryHider.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 0, 1, this.getWidth() - 4, true));
         }

         this.expandButton = this.categoryHider
            .addComponent(
               new FormContentIconButton(0, 2 + (addBreakLine ? 4 : 0), FormInputSize.SIZE_20, ButtonColor.BASE, this.getInterfaceStyle().button_collapsed_16)
            );
         this.expandButton.onClicked(e -> this.setExpanded(!this.isExpanded, false));
         this.categoryHider.addComponent(new FormLocalLabel(category.displayName, new FontOptions(16), -1, 22, 4 + (addBreakLine ? 4 : 0), width));
         this.content = this.addComponent(new FormComponentList() {
            @Override
            public boolean shouldUseMouseEvents() {
               return false;
            }
         });
         List<InventoryItem> items = new ArrayList<>();

         for (Item categoryItem : categoryItems) {
            if (categoryItem != null) {
               categoryItem.addDefaultItems(items, CreativeItemsTab.this.playerClient.getPlayer());
            }
         }

         this.itemComponents = items.stream()
            .sorted(Comparator.comparingInt(inventoryItem -> inventoryItem.item.getID()))
            .map(inventoryItem -> this.content.addComponent(new FormContainerCreativeItem(inventoryItem, 0, 24, CreativeItemsTab.this.playerClient)))
            .collect(Collectors.toCollection(ArrayList::new));
         categoryComponents.forEach(this.content::addComponent);
         CreativeItemsTab.this.updatePositions = true;
         this.setExpanded(true, true);
      }

      @Override
      public boolean shouldUseMouseEvents() {
         return false;
      }

      @Override
      public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
         event = InputEvent.ReplacePosEvent(
            event, InputPosition.fromHudPos(WindowManager.getWindow().getInput(), event.pos.hudX - this.getX(), event.pos.hudY - this.getY())
         );
         if (!this.categoryHider.isHidden()) {
            this.expandButton.handleInputEvent(event, tickManager, perspective);
         }

         this.content.handleInputEvent(event, tickManager, perspective);
      }

      private int updateContentPositions(int startPosition) {
         if (CreativeItemsTab.this.compactMode) {
            this.setX(this.isTopLevel ? 4 : 0);
            this.setWidth(this.fullWidth - 16);
         } else {
            this.setWidth(this.fullWidth - this.category.depth * 16);
            this.setX(4 + (this.category.depth <= 1 ? 0 : 16));
         }

         this.categoryHider.setHidden(CreativeItemsTab.this.compactMode && !this.isTopLevel);
         int padding = 2;
         int availableWidth = this.getWidth() - 16;
         int elementSize = 32 + padding * 2;
         int elementsPerRow = availableWidth / elementSize;
         int startHeight = CreativeItemsTab.this.compactMode && !this.isTopLevel ? 0 : 24 + (this.addBreakLine ? 4 : 0);
         int positionIndex = startPosition;
         int row = 0;
         int startRow = positionIndex / elementsPerRow;
         this.contentHeight = startHeight;

         for (FormContainerCreativeItem creativeItem : this.itemComponents) {
            creativeItem.setHidden(creativeItem.isFilteredOut());
            if (!creativeItem.isHidden()) {
               int lastRow = row - startRow;
               int column = positionIndex % elementsPerRow;
               row = positionIndex / elementsPerRow;
               creativeItem.setPosition(column * elementSize, (row - startRow) * elementSize + startHeight);
               this.contentHeight += (row - startRow - lastRow) * elementSize;
               positionIndex++;
            }
         }

         if (CreativeItemsTab.this.compactMode || positionIndex > 0) {
            this.contentHeight += elementSize;
         }

         for (CreativeItemsTab.CategoryForm categoryComponent : this.categoryComponents) {
            if (!categoryComponent.isHidden()) {
               row = positionIndex / elementsPerRow;
               categoryComponent.setY(CreativeItemsTab.this.compactMode ? (row - startRow) * elementSize + startHeight : this.contentHeight);
               positionIndex = categoryComponent.updateContentPositions(CreativeItemsTab.this.compactMode ? positionIndex : 0);
               categoryComponent.updateDimensions();
               if (CreativeItemsTab.this.compactMode) {
                  int lastRow = row;
                  row = positionIndex / elementsPerRow;
                  this.contentHeight += (row - lastRow) * elementSize;
               } else {
                  this.contentHeight = this.contentHeight + (categoryComponent.getHeight() - 8);
               }
            }
         }

         this.updateDimensions();
         return positionIndex;
      }

      public void setExpanded(boolean expanded, boolean forceUpdate) {
         if (this.isExpanded != expanded || forceUpdate) {
            this.isExpanded = expanded;
            if (expanded) {
               this.expandButton.setIcon(this.getInterfaceStyle().button_expanded_16);
               this.content.setHidden(false);
            } else {
               this.expandButton.setIcon(this.getInterfaceStyle().button_collapsed_16);
               this.content.setHidden(true);
            }

            this.updateDimensions();
            CreativeItemsTab.this.updatePositions = true;
            if (this.category.parent != null) {
               CreativeItemsTab.CategoryForm parentCategoryForm = CreativeItemsTab.this.categories.get(this.category.parent);
               if (parentCategoryForm != null) {
                  parentCategoryForm.setExpanded(true, false);
               }
            }
         }
      }

      private void updateDimensions() {
         if (this.content.isHidden()) {
            this.setHeight(32 + (this.addBreakLine ? 4 : 0));
         } else {
            this.setHeight(CreativeItemsTab.this.compactMode ? this.contentHeight : (this.addBreakLine ? 4 : 0) + this.contentHeight + 4);
         }
      }

      @Override
      public List<Rectangle> getHitboxes() {
         return this.isHidden()
            ? singleBox(new Rectangle(this.getX(), this.getY(), 0, 0))
            : singleBox(new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight()));
      }
   }

   private class SearchThread extends Thread {
      private final LinkedBlockingQueue<String> queryTasks = new LinkedBlockingQueue<>();

      public SearchThread() {
         super("CreativeObjectsTab Search Thread");
         this.setDaemon(true);
      }

      @Override
      public void run() {
         label62:
         while (true) {
            try {
               String query = this.queryTasks.take();
               if (query.trim().isEmpty()) {
                  for (CreativeItemsTab.CategoryForm category : CreativeItemsTab.this.categories.values()) {
                     for (FormContainerCreativeItem item : category.itemComponents) {
                        if (!this.queryTasks.isEmpty()) {
                           continue label62;
                        }

                        item.setFilteredOut(false);
                     }

                     category.setHidden(false);
                     CreativeItemsTab.this.compactMode = false;
                     CreativeItemsTab.this.updatePositions = true;
                  }
               } else {
                  ItemSearchTester searchTester = ItemSearchTester.constructSearchTester(query);

                  for (CreativeItemsTab.CategoryForm category : CreativeItemsTab.this.topCategories) {
                     if (!this.queryTasks.isEmpty()) {
                        break;
                     }

                     if (this.categoryOrSubCategoryHasItem(category, searchTester)) {
                        CreativeItemsTab.this.updatePositions = true;
                     }
                  }
               }
            } catch (InterruptedException e) {
               return;
            }
         }
      }

      private boolean categoryOrSubCategoryHasItem(CreativeItemsTab.CategoryForm category, ItemSearchTester searchTester) {
         boolean hasItems = false;

         for (FormContainerCreativeItem item : category.itemComponents) {
            if (!this.queryTasks.isEmpty()) {
               return false;
            }

            boolean showItem = searchTester.matches(item.item, CreativeItemsTab.this.playerClient.getPlayer(), new GameBlackboard());
            item.setFilteredOut(!showItem);
            if (showItem) {
               hasItems = true;
            }
         }

         for (CreativeItemsTab.CategoryForm subCategory : category.categoryComponents) {
            if (this.categoryOrSubCategoryHasItem(subCategory, searchTester)) {
               hasItems = true;
            }
         }

         CreativeItemsTab.this.compactMode = true;
         category.setExpanded(true, false);
         category.setHidden(!hasItems);
         category.updateDimensions();
         return hasItems;
      }
   }
}
