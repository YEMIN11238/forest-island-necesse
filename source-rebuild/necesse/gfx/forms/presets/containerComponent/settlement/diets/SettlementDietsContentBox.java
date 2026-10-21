package necesse.gfx.forms.presets.containerComponent.settlement.diets;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSettlerDataIcon;
import necesse.gfx.forms.components.FormSettlerDataNameLabel;
import necesse.gfx.forms.components.SavedFormContentBoxScroll;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementSettlersForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.item.Item;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;

public class SettlementDietsContentBox extends FormContentBox {
   public static SavedFormContentBoxScroll lastScroll = new SavedFormContentBoxScroll();
   public final SettlementDietsForm<?> dietsForm;
   public ArrayList<SettlementDietsForm.PasteButton> pasteButtons = new ArrayList<>();
   public int contentHeight;

   public SettlementDietsContentBox(SettlementDietsForm<?> dietsForm, int x, int y, int width, int height) {
      super(x, y, width, height);
      this.dietsForm = dietsForm;
   }

   public void updateDietsContent() {
      this.clearComponents();
      this.pasteButtons.clear();
      FormFlow flow = new FormFlow(0);
      String searchLower = SettlementSettlersForm.lastSettlersSearch == null ? "" : SettlementSettlersForm.lastSettlersSearch.toLowerCase();
      List<SettlementSettlerData> settlers = this.dietsForm
         .containerForm
         .dataManager
         .streamSettlers()
         .filter(data -> data.dietData != null)
         .filter(data -> data.matchesSearch(searchLower, this.dietsForm.client.getLevel(), this.dietsForm.client.getPlayer()))
         .collect(Collectors.toList());
      if (!settlers.isEmpty()) {
         this.addSettlers(flow, settlers);
      }

      this.dietsForm.listClipboard.forceUpdate();
      this.dietsForm.listClipboard.onUpdate(this.dietsForm.listClipboard.getValue());
      if (settlers.isEmpty()) {
         this.alwaysShowVerticalScrollBar = false;
         flow.next(16);
         if (this.dietsForm.containerForm.dataManager.getTotalSettlers() == 0) {
            this.addComponent(
               flow.nextY(new FormLocalLabel("ui", "settlersnosettlers", new FontOptions(16), 0, this.getWidth() / 2, 0, this.getWidth() - 20), 16)
            );
         } else {
            this.addComponent(
               flow.nextY(new FormLocalLabel("ui", "settlersnosettlerssearch", new FontOptions(16), 0, this.getWidth() / 2, 0, this.getWidth() - 20), 16)
            );
         }
      } else {
         this.alwaysShowVerticalScrollBar = true;
      }

      this.contentHeight = Math.max(flow.next(), 70);
      this.dietsForm.updateSize();
      if (!settlers.isEmpty()) {
         lastScroll.load(this);
      }
   }

   private void addSettlers(FormFlow flow, List<SettlementSettlerData> list) {
      for (final SettlementSettlerData data : list) {
         int padding = SettlementSettlersForm.SETTLER_LIST_PADDING;
         int height = 32 + padding * 2;
         int startY = flow.next(height);
         int y = startY + padding;
         int dietButtonWidth = 150;
         int buttonX = this.getWidth() - dietButtonWidth - this.getScrollBarWidth() - 2;
         FormLocalTextButton configureButton = this.addComponent(
            new FormLocalTextButton("ui", "settlementchangediet", buttonX, y + 3, dietButtonWidth, FormInputSize.SIZE_24, ButtonColor.BASE) {
               @Override
               public boolean isActive() {
                  return super.isActive() && data.dietData != null && data.dietData.doesSettlerEat;
               }
            }
         );
         configureButton.onClicked(
            e -> {
               if (data.dietData != null) {
                  GameMessage header = new StaticMessage(data.getSettlerName(this.dietsForm.client.getLevel(), "N/A"));
                  SettlementDietFilterForm filterForm = new SettlementDietFilterForm(
                     "changediet", 408, 250, header, data.dietData.dietFilter, this.dietsForm.client
                  ) {
                     @Override
                     public void onItemsChanged(Item[] items, boolean allowed) {
                        SettlementDietsContentBox.this.dietsForm
                           .container
                           .setSettlerDiet
                           .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.itemsAllowed(items, allowed));
                     }

                     @Override
                     public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
                        SettlementDietsContentBox.this.dietsForm
                           .container
                           .setSettlerDiet
                           .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.categoryAllowed(category, allowed));
                     }

                     @Override
                     public void onFullChange(ItemCategoriesFilter filter) {
                        SettlementDietsContentBox.this.dietsForm
                           .container
                           .setSettlerDiet
                           .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.fullChange(filter));
                     }

                     @Override
                     public void onButtonPressed() {
                        SettlementDietsContentBox.this.dietsForm.makeCurrent(SettlementDietsContentBox.this.dietsForm.mainForm);
                     }

                     @Override
                     public void onWindowResized(GameWindow window) {
                        super.onWindowResized(window);
                        ContainerComponent.setPosInventory(this);
                     }
                  };
                  this.dietsForm.addAndMakeCurrentTemporary(filterForm);
                  filterForm.onWindowResized(WindowManager.getWindow());
               }
            }
         );
         buttonX -= 24;
         FormContentIconButton pasteButton = this.addComponent(
            new FormContentIconButton(
               buttonX,
               y + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().paste_button,
               new GameMessage[]{new LocalMessage("ui", "pastebutton")}
            ) {
               @Override
               public boolean isActive() {
                  return super.isActive() && data.dietData != null && data.dietData.doesSettlerEat;
               }
            }
         );
         pasteButton.onClicked(e -> {
            if (data.dietData != null) {
               SettlementDietFilterForm.DietData dietData = this.dietsForm.listClipboard.getValue();
               if (dietData != null) {
                  SaveData save = new SaveData("");
                  dietData.filter.addSaveData(save);
                  data.dietData.dietFilter.applyLoadData(save.toLoadData());
                  this.dietsForm.container.setSettlerDiet.runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.fullChange(data.dietData.dietFilter));
                  pasteButton.setActive(false);
               }
            }
         });
         pasteButton.setupDragPressOtherButtons("dietPasteButton");
         this.pasteButtons.add(new SettlementDietsForm.PasteButton(pasteButton, () -> data.dietData == null ? null : data.dietData.dietFilter));
         buttonX -= 24;
         FormContentIconButton copyButton = this.addComponent(
            new FormContentIconButton(
               buttonX,
               y + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().copy_button,
               new GameMessage[]{new LocalMessage("ui", "copybutton")}
            ) {
               @Override
               public boolean isActive() {
                  return super.isActive() && data.dietData != null && data.dietData.doesSettlerEat;
               }
            }
         );
         copyButton.onClicked(e -> {
            if (data.dietData != null) {
               SettlementDietFilterForm.DietData dietData = new SettlementDietFilterForm.DietData(data.dietData.dietFilter);
               WindowManager.getWindow().putClipboard(dietData.getSaveData().getScript());
               this.dietsForm.listClipboard.forceUpdate();
            }
         });
         this.addComponent(new FormSettlerDataIcon(5, y, data, this.dietsForm.containerForm));
         int namesX = 37;
         int namesWidth = buttonX - namesX;
         FontOptions nameFontOptions = new FontOptions(16);
         this.addComponent(
            new FormSettlerDataNameLabel(this.dietsForm.containerForm.getClient(), data, nameFontOptions, FairType.TextAlign.LEFT, namesX, y, namesWidth)
         );
         FontOptions settlerOptions = new FontOptions(12);
         this.addComponent(new FormLabel(GameUtils.maxString(data.settler.getGenericMobName(), settlerOptions, namesWidth), settlerOptions, -1, namesX, y + 16));
      }
   }

   public void updatePasteButtons(SettlementDietFilterForm.DietData value) {
      for (SettlementDietsForm.PasteButton pasteButton : this.pasteButtons) {
         pasteButton.updateActive(value);
      }
   }

   @Override
   public void dispose() {
      lastScroll.save(this);
      super.dispose();
   }
}
