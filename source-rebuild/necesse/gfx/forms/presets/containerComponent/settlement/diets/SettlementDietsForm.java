package necesse.gfx.forms.presets.containerComponent.settlement.diets;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.function.Supplier;
import necesse.engine.ClipboardTracker;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementContainerForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementSettlersForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementSubForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.events.SettlementNewSettlerDietChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerDietChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerDietDataEvent;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;

public class SettlementDietsForm<T extends SettlementContainer> extends FormSwitcher implements SettlementSubForm {
   public final Client client;
   public final T container;
   public final SettlementContainerForm<T> containerForm;
   public int maxHeight;
   protected FormSwitcher setCurrentWhenLoaded;
   protected Form mainForm;
   protected SettlementDietsContentBox content;
   public ItemCategoriesFilter newSettlerDiet = new ItemCategoriesFilter(ItemCategory.foodQualityMasterCategory, true);
   public FormTextInput searchInput;
   public SettlementDietsForm.PasteButton newSettlerDietPasteButton;
   public ClipboardTracker<SettlementDietFilterForm.DietData> listClipboard;

   public SettlementDietsForm(Client client, T container, SettlementContainerForm<T> containerForm) {
      this.client = client;
      this.container = container;
      this.containerForm = containerForm;
      this.maxHeight = 300;
      this.mainForm = this.addComponent(new Form("settlers", 500, 300));
      FormFlow flow = new FormFlow(5);
      this.mainForm.addComponent(new FormLocalLabel("ui", "settlementdiets", new FontOptions(20), 0, this.mainForm.getWidth() / 2, flow.next(25)));
      int dietButtonWidth = 150;
      int newSettlersButtonX = this.mainForm.getWidth() - dietButtonWidth - this.getInterfaceStyle().scrollbar.active.getHeight() - 2;
      int newSettlersY = flow.next(30);
      this.mainForm
         .addComponent(
            new FormLocalTextButton(
               "ui", "settlementchangediet", newSettlersButtonX, newSettlersY + 3, dietButtonWidth, FormInputSize.SIZE_24, ButtonColor.BASE
            )
         )
         .onClicked(
            e -> {
               SettlementDietFilterForm filterForm = new SettlementDietFilterForm(
                  "changediet", 408, 250, this.newSettlerDiet, client, new LocalMessage("ui", "settlementnewsettlers"), new LocalMessage("ui", "backbutton")
               ) {
                  @Override
                  public void onItemsChanged(Item[] items, boolean allowed) {
                     container.setNewSettlerDiet.runAndSendChange(ItemCategoriesFilterChange.itemsAllowed(items, allowed));
                  }

                  @Override
                  public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
                     container.setNewSettlerDiet.runAndSendChange(ItemCategoriesFilterChange.categoryAllowed(category, allowed));
                  }

                  @Override
                  public void onFullChange(ItemCategoriesFilter filter) {
                     container.setNewSettlerDiet.runAndSendChange(ItemCategoriesFilterChange.fullChange(filter));
                  }

                  @Override
                  public void onButtonPressed() {
                     SettlementDietsForm.this.makeCurrent(SettlementDietsForm.this.mainForm);
                  }

                  @Override
                  public void onWindowResized(GameWindow window) {
                     super.onWindowResized(window);
                     ContainerComponent.setPosInventory(this);
                  }
               };
               this.addAndMakeCurrentTemporary(filterForm);
               filterForm.onWindowResized(WindowManager.getWindow());
            }
         );
      newSettlersButtonX -= 24;
      FormContentIconButton pasteButton = this.mainForm
         .addComponent(
            new FormContentIconButton(
               newSettlersButtonX,
               newSettlersY + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().paste_button,
               new LocalMessage("ui", "pastebutton")
            )
         );
      pasteButton.onClicked(e -> {
         SettlementDietFilterForm.DietData dietData = this.listClipboard.getValue();
         if (dietData != null) {
            SaveData save = new SaveData("");
            dietData.filter.addSaveData(save);
            this.newSettlerDiet.applyLoadData(save.toLoadData());
            container.setNewSettlerDiet.runAndSendChange(ItemCategoriesFilterChange.fullChange(this.newSettlerDiet));
            pasteButton.setActive(false);
         }
      });
      pasteButton.setupDragPressOtherButtons("dietPasteButton");
      this.newSettlerDietPasteButton = new SettlementDietsForm.PasteButton(pasteButton, () -> this.newSettlerDiet);
      newSettlersButtonX -= 24;
      this.mainForm
         .addComponent(
            new FormContentIconButton(
               newSettlersButtonX,
               newSettlersY + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().copy_button,
               new LocalMessage("ui", "copybutton")
            )
         )
         .onClicked(e -> {
            SettlementDietFilterForm.DietData dietData = new SettlementDietFilterForm.DietData(this.newSettlerDiet);
            WindowManager.getWindow().putClipboard(dietData.getSaveData().getScript());
            this.listClipboard.forceUpdate();
         });
      this.mainForm.addComponent(new FormLocalLabel("ui", "settlementnewsettlers", new FontOptions(20), -1, 5, newSettlersY + 5));
      this.mainForm.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 0, flow.next(4), this.mainForm.getWidth(), true));
      this.searchInput = this.mainForm.addComponent(new FormTextInput(4, flow.next(28), FormInputSize.SIZE_24, this.mainForm.getWidth() - 8, -1, 500));
      this.searchInput.placeHolder = new LocalMessage("ui", "searchtip");
      this.searchInput.rightClickToClear = true;
      this.searchInput.rightClickToClearTooltip = new LocalMessage("controls", "clearsearchtip");
      if (SettlementSettlersForm.lastSettlersSearch != null) {
         this.searchInput.setText(SettlementSettlersForm.lastSettlersSearch);
      }

      this.searchInput.onChange(e -> {
         SettlementSettlersForm.lastSettlersSearch = this.searchInput.getText();
         this.content.updateDietsContent();
      });
      int settlersContentHeight = 240;
      this.content = this.mainForm
         .addComponent(new SettlementDietsContentBox(this, 0, flow.next(settlersContentHeight), this.mainForm.getWidth(), settlersContentHeight));
      this.listClipboard = new ClipboardTracker<SettlementDietFilterForm.DietData>() {
         public SettlementDietFilterForm.DietData parse(String clipboard) {
            try {
               return new SettlementDietFilterForm.DietData(new LoadData(clipboard));
            } catch (Exception e) {
               return null;
            }
         }

         public void onUpdate(SettlementDietFilterForm.DietData value) {
            SettlementDietsForm.this.newSettlerDietPasteButton.updateActive(value);
            SettlementDietsForm.this.content.updatePasteButtons(value);
         }
      };
   }

   @Override
   protected void init() {
      super.init();
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         if (this.containerForm.isCurrent(this)) {
            this.containerForm.dataManager.requestSettlerData(this.container.dietsSubscription, s -> s.dietData == null);
            this.content.updateDietsContent();
         }
      });
      this.container.onEvent(SettlementSettlerDietDataEvent.class, event -> {
         this.containerForm.dataManager.applySettlerData(event, (data, addon) -> data.dietData = addon);
         if (this.setCurrentWhenLoaded != null) {
            this.setCurrentWhenLoaded.makeCurrent(this);
         }

         this.setCurrentWhenLoaded = null;
         if (this.containerForm.isCurrent(this)) {
            this.content.updateDietsContent();
         }
      });
      this.container.onEvent(SettlementNewSettlerDietChangedEvent.class, event -> {
         event.change.applyTo(this.newSettlerDiet);
         this.listClipboard.forceUpdate();
         this.listClipboard.onUpdate(this.listClipboard.getValue());
      });
      this.container.onEvent(SettlementSettlerDietChangedEvent.class, event -> {
         for (SettlementSettlerData settler : this.containerForm.dataManager.getSettlers()) {
            if (settler.mobUniqueID == event.mobUniqueID && settler.dietData != null) {
               event.change.applyTo(settler.dietData.dietFilter);
               this.listClipboard.forceUpdate();
               this.listClipboard.onUpdate(this.listClipboard.getValue());
               return;
            }
         }

         this.container.dietsSubscription.requestData(Collections.singletonList(event.mobUniqueID));
      });
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.listClipboard.update();
      super.draw(tickManager, perspective, renderBox);
   }

   public void updateSize() {
      this.mainForm.setHeight(Math.min(this.maxHeight, this.content.getY() + this.content.contentHeight));
      this.content.setContentBox(new Rectangle(0, 0, this.content.getWidth(), this.content.contentHeight));
      this.content.setWidth(this.mainForm.getWidth());
      this.content.setHeight(this.mainForm.getHeight() - this.content.getY());
      ContainerComponent.setPosInventory(this.mainForm);
   }

   @Override
   public void onSetCurrent(boolean current) {
      this.content.clearComponents();
      if (SettlementSettlersForm.lastSettlersSearch != null) {
         this.searchInput.setText(SettlementSettlersForm.lastSettlersSearch, false);
      }

      if (current) {
         this.container.dietsSubscription.ensureSubscribed();
         this.makeCurrent(this.mainForm);
      } else {
         this.container.dietsSubscription.ensureUnsubscribed();
      }
   }

   @Override
   public void onMenuButtonClicked(FormSwitcher switcher) {
      this.setCurrentWhenLoaded = switcher;
      this.container.dietsSubscription.ensureSubscribed();
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.updateSize();
   }

   @Override
   public GameMessage getMenuButtonName() {
      return new LocalMessage("ui", "settlementdiets");
   }

   @Override
   public String getTypeString() {
      return "diets";
   }

   public static class PasteButton {
      public final FormContentIconButton button;
      public final Supplier<ItemCategoriesFilter> dietFilter;

      public PasteButton(FormContentIconButton button, Supplier<ItemCategoriesFilter> dietFilter) {
         this.button = button;
         this.dietFilter = dietFilter;
      }

      public void updateActive(SettlementDietFilterForm.DietData data) {
         if (data != null && data.filter != null) {
            ItemCategoriesFilter filter = this.dietFilter.get();
            this.button.setActive(filter != null && !data.filter.isEqualsFilter(filter));
         } else {
            this.button.setActive(false);
         }
      }
   }
}
