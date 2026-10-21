package necesse.gfx.forms.presets.containerComponent.settlement.equipment;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.function.Supplier;
import necesse.engine.ClipboardTracker;
import necesse.engine.Settings;
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
import necesse.gfx.forms.components.FormContentIconToggleButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.mob.EquipmentFiltersForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementContainerForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementSettlersForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementSubForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.events.SettlementNewSettlerEquipmentFilterChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentFilterChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentSelfManageChangedEvent;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;

public class SettlementEquipmentForm<T extends SettlementContainer> extends FormSwitcher implements SettlementSubForm {
   public final Client client;
   public final T container;
   public final SettlementContainerForm<T> containerForm;
   public int maxHeight;
   protected FormSwitcher setCurrentWhenLoaded;
   protected Form mainForm;
   protected SettlementEquipmentContentBox content;
   public boolean newSettlerSelfManageEquipment = true;
   public boolean newSettlerPreferArmorSets = true;
   public ItemCategoriesFilter newSettlerEquipmentsFilter = new ItemCategoriesFilter(ItemCategory.equipmentMasterCategory, true);
   public FormTextInput searchInput;
   public SettlementEquipmentForm.PasteButton newSettlerPasteButton;
   public ClipboardTracker<EquipmentFiltersForm.EquipmentFilterData> listClipboard;

   public SettlementEquipmentForm(Client client, T container, SettlementContainerForm<T> containerForm) {
      this.client = client;
      this.container = container;
      this.containerForm = containerForm;
      this.maxHeight = 300;
      FormFlow flow = new FormFlow(5);
      this.mainForm = this.addComponent(new Form("settlers", 500, 300));
      this.mainForm.addComponent(new FormLocalLabel("ui", "settlementequipment", new FontOptions(20), 0, this.mainForm.getWidth() / 2, flow.next(25)));
      FormContentIconToggleButton showHeadArmor = this.mainForm
         .addComponent(
            new FormContentIconToggleButton(
               this.mainForm.getWidth() - 28,
               4,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().button_checked_20,
               this.getInterfaceStyle().button_escaped_20,
               new LocalMessage("settingsui", "showsettlerheadarmor")
            ) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  this.setToggled(Settings.showSettlerHeadArmor);
                  super.draw(tickManager, perspective, renderBox);
               }
            }
         );
      showHeadArmor.onToggled(e -> {
         Settings.showSettlerHeadArmor = e.from.isToggled();
         Settings.saveClientSettings();
      });
      int filterButtonWidth = 24;
      int newSettlersButtonX = this.mainForm.getWidth() - filterButtonWidth - this.getInterfaceStyle().scrollbar.active.getHeight() - 2;
      int newSettlersY = flow.next(30);
      FormContentIconButton configureFilterButton = this.mainForm
         .addComponent(
            new FormContentIconButton(
               newSettlersButtonX,
               newSettlersY + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().container_storage_config,
               new LocalMessage("ui", "settlerfilterequipment")
            ) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  this.setActive(SettlementEquipmentForm.this.newSettlerSelfManageEquipment);
                  super.draw(tickManager, perspective, renderBox);
               }
            }
         );
      configureFilterButton.onClicked(
         e -> {
            EquipmentFiltersForm filterForm = new EquipmentFiltersForm(
               "changeequipmentfilter",
               408,
               300,
               this.newSettlerEquipmentsFilter,
               () -> this.newSettlerPreferArmorSets,
               client,
               new LocalMessage("ui", "settlementnewsettlers"),
               new LocalMessage("ui", "backbutton")
            ) {
               @Override
               public void onSetPreferArmorSets(boolean preferArmorSets) {
                  SettlementEquipmentForm.this.newSettlerPreferArmorSets = preferArmorSets;
                  container.setNewSettlerEquipmentFilter.runAndSendPreferArmorSets(preferArmorSets);
               }

               @Override
               public void onItemsChanged(Item[] items, boolean allowed) {
                  container.setNewSettlerEquipmentFilter.runAndSendChange(ItemCategoriesFilterChange.itemsAllowed(items, allowed));
               }

               @Override
               public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
                  container.setNewSettlerEquipmentFilter.runAndSendChange(ItemCategoriesFilterChange.categoryAllowed(category, allowed));
               }

               @Override
               public void onFullChange(ItemCategoriesFilter filter) {
                  container.setNewSettlerEquipmentFilter.runAndSendChange(ItemCategoriesFilterChange.fullChange(filter));
               }

               @Override
               public void onButtonPressed() {
                  SettlementEquipmentForm.this.makeCurrent(SettlementEquipmentForm.this.mainForm);
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
            ) {
               @Override
               public boolean isActive() {
                  return super.isActive() && SettlementEquipmentForm.this.newSettlerSelfManageEquipment;
               }
            }
         );
      pasteButton.onClicked(e -> {
         EquipmentFiltersForm.EquipmentFilterData equipmentFilterData = this.listClipboard.getValue();
         if (equipmentFilterData != null) {
            SaveData save = new SaveData("");
            equipmentFilterData.filter.addSaveData(save);
            this.newSettlerPreferArmorSets = equipmentFilterData.preferArmorSets;
            this.newSettlerEquipmentsFilter.applyLoadData(save.toLoadData());
            container.setNewSettlerEquipmentFilter.runAndSendPreferArmorSets(this.newSettlerPreferArmorSets);
            container.setNewSettlerEquipmentFilter.runAndSendChange(ItemCategoriesFilterChange.fullChange(this.newSettlerEquipmentsFilter));
            pasteButton.setActive(false);
         }
      });
      pasteButton.setupDragPressOtherButtons("equipmentPasteButton");
      this.newSettlerPasteButton = new SettlementEquipmentForm.PasteButton(
         pasteButton, () -> this.newSettlerPreferArmorSets, () -> this.newSettlerEquipmentsFilter
      );
      newSettlersButtonX -= 24;
      FormContentIconButton copyButton = this.mainForm
         .addComponent(
            new FormContentIconButton(
               newSettlersButtonX,
               newSettlersY + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().copy_button,
               new LocalMessage("ui", "copybutton")
            ) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  this.setActive(SettlementEquipmentForm.this.newSettlerSelfManageEquipment);
                  super.draw(tickManager, perspective, renderBox);
               }
            }
         );
      copyButton.onClicked(
         e -> {
            EquipmentFiltersForm.EquipmentFilterData equipmentFilterData = new EquipmentFiltersForm.EquipmentFilterData(
               this.newSettlerPreferArmorSets, this.newSettlerEquipmentsFilter
            );
            WindowManager.getWindow().putClipboard(equipmentFilterData.getSaveData().getScript());
            this.listClipboard.forceUpdate();
         }
      );
      newSettlersButtonX -= 24;
      FormContentIconToggleButton selfManageButton = this.mainForm
         .addComponent(
            new FormContentIconToggleButton(
               newSettlersButtonX,
               newSettlersY + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().button_checked_20,
               this.getInterfaceStyle().button_escaped_20,
               new LocalMessage("ui", "settlerselfmanagequipment")
            ) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  this.setToggled(SettlementEquipmentForm.this.newSettlerSelfManageEquipment);
                  super.draw(tickManager, perspective, renderBox);
               }
            }
         );
      selfManageButton.onToggled(e -> {
         this.newSettlerSelfManageEquipment = e.from.isToggled();
         container.setNewSettlerEquipmentFilter.runAndSendSelfManageEquipment(this.newSettlerSelfManageEquipment);
      });
      selfManageButton.setupDragToOtherButtons("selfManageEquipment");
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
         this.content.updateEquipmentsContent();
      });
      int settlersContentHeight = 240;
      this.content = this.mainForm
         .addComponent(new SettlementEquipmentContentBox(this, 0, flow.next(settlersContentHeight), this.mainForm.getWidth(), settlersContentHeight));
      this.mainForm.setHeight(flow.next());
      this.listClipboard = new ClipboardTracker<EquipmentFiltersForm.EquipmentFilterData>() {
         public EquipmentFiltersForm.EquipmentFilterData parse(String clipboard) {
            try {
               return new EquipmentFiltersForm.EquipmentFilterData(new LoadData(clipboard));
            } catch (Exception e) {
               return null;
            }
         }

         public void onUpdate(EquipmentFiltersForm.EquipmentFilterData value) {
            SettlementEquipmentForm.this.newSettlerPasteButton.updateActive(value);
            SettlementEquipmentForm.this.content.updatePasteButtons(value);
         }
      };
   }

   @Override
   protected void init() {
      super.init();
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         if (this.containerForm.isCurrent(this)) {
            this.containerForm.dataManager.requestSettlerData(this.container.equipmentSubscription, s -> s.equipmentData == null);
            this.content.updateEquipmentsContent();
         }
      });
      this.container.onEvent(SettlementSettlerEquipmentDataEvent.class, event -> {
         this.containerForm.dataManager.applySettlerData(event, (data, addon) -> data.equipmentData = addon);
         if (this.setCurrentWhenLoaded != null) {
            this.setCurrentWhenLoaded.makeCurrent(this);
         }

         this.setCurrentWhenLoaded = null;
         if (this.containerForm.isCurrent(this)) {
            this.content.updateEquipmentsContent();
            this.containerForm.dataManager.requestSettlerData(this.container.requestSettlerGearData, s -> s.gearData == null);
         }
      });
      this.container.onEvent(SettlementSettlerEquipmentSelfManageChangedEvent.class, event -> {
         SettlementSettlerData settler = this.containerForm.dataManager.getSettler(event.mobUniqueID);
         if (settler != null && settler.equipmentData != null) {
            settler.equipmentData.selfManageEquipment = event.selfManageEquipment;
         }
      });
      this.container.onEvent(SettlementNewSettlerEquipmentFilterChangedEvent.class, event -> {
         this.newSettlerSelfManageEquipment = event.selfManageEquipment;
         this.newSettlerPreferArmorSets = event.preferArmorSets;
         if (event.change != null) {
            event.change.applyTo(this.newSettlerEquipmentsFilter);
         }

         this.listClipboard.forceUpdate();
         this.listClipboard.onUpdate(this.listClipboard.getValue());
      });
      this.container.onEvent(SettlementSettlerEquipmentFilterChangedEvent.class, event -> {
         for (SettlementSettlerData settler : this.containerForm.dataManager.getSettlers()) {
            if (settler.mobUniqueID == event.mobUniqueID && settler.equipmentData != null) {
               settler.equipmentData.preferArmorSets = event.preferArmorSets;
               if (event.change != null) {
                  event.change.applyTo(settler.equipmentData.equipmentFilter);
               }

               this.listClipboard.forceUpdate();
               this.listClipboard.onUpdate(this.listClipboard.getValue());
               return;
            }
         }

         this.container.equipmentSubscription.requestData(Collections.singletonList(event.mobUniqueID));
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
         this.container.equipmentSubscription.ensureSubscribed();
         this.makeCurrent(this.mainForm);
      } else {
         this.container.equipmentSubscription.ensureUnsubscribed();
      }
   }

   @Override
   public void onMenuButtonClicked(FormSwitcher switcher) {
      this.setCurrentWhenLoaded = switcher;
      this.container.equipmentSubscription.ensureSubscribed();
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.updateSize();
   }

   @Override
   public GameMessage getMenuButtonName() {
      return new LocalMessage("ui", "settlementequipment");
   }

   @Override
   public String getTypeString() {
      return "equipment";
   }

   public static class PasteButton {
      public final FormContentIconButton button;
      public final Supplier<Boolean> preferArmorSets;
      public final Supplier<ItemCategoriesFilter> equipmentsFilter;

      public PasteButton(FormContentIconButton button, Supplier<Boolean> preferArmorSets, Supplier<ItemCategoriesFilter> equipmentsFilter) {
         this.button = button;
         this.preferArmorSets = preferArmorSets;
         this.equipmentsFilter = equipmentsFilter;
      }

      public void updateActive(EquipmentFiltersForm.EquipmentFilterData data) {
         if (data != null && data.filter != null) {
            ItemCategoriesFilter other = this.equipmentsFilter.get();
            this.button.setActive(data.preferArmorSets != this.preferArmorSets.get() || other != null && !data.filter.isEqualsFilter(other));
         } else {
            this.button.setActive(false);
         }
      }
   }
}
