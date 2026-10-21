package necesse.gfx.forms.presets.containerComponent.settlement.equipment;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormContentIconToggleButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormItemPreview;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSettlerDataIcon;
import necesse.gfx.forms.components.FormSettlerDataNameLabel;
import necesse.gfx.forms.components.SavedFormContentBoxScroll;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.mob.EquipmentFiltersForm;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementSettlersForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.data.SettlerGearData;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementEquipmentContentBox extends FormContentBox {
   public static SavedFormContentBoxScroll lastScroll = new SavedFormContentBoxScroll();
   public final SettlementEquipmentForm<?> equipmentsForm;
   public ArrayList<SettlementEquipmentForm.PasteButton> pasteButtons = new ArrayList<>();
   public int contentHeight;

   public SettlementEquipmentContentBox(SettlementEquipmentForm<?> equipmentsForm, int x, int y, int width, int height) {
      super(x, y, width, height);
      this.equipmentsForm = equipmentsForm;
   }

   public void updateEquipmentsContent() {
      this.clearComponents();
      this.pasteButtons.clear();
      FormFlow flow = new FormFlow(0);
      String searchLower = SettlementSettlersForm.lastSettlersSearch == null ? "" : SettlementSettlersForm.lastSettlersSearch.toLowerCase();
      List<SettlementSettlerData> settlers = this.equipmentsForm
         .containerForm
         .dataManager
         .streamSettlers()
         .filter(data -> data.equipmentData != null)
         .filter(data -> data.matchesSearch(searchLower, this.equipmentsForm.client.getLevel(), this.equipmentsForm.client.getPlayer(), data.equipmentData))
         .collect(Collectors.toList());
      if (!settlers.isEmpty()) {
         this.addSettlers(flow, settlers);
      }

      this.equipmentsForm.listClipboard.forceUpdate();
      this.equipmentsForm.listClipboard.onUpdate(this.equipmentsForm.listClipboard.getValue());
      if (settlers.isEmpty()) {
         this.alwaysShowVerticalScrollBar = false;
         flow.next(16);
         if (this.equipmentsForm.containerForm.dataManager.getTotalSettlers() == 0) {
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
      this.equipmentsForm.updateSize();
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
         int filterButtonWidth = 24;
         int buttonX = this.getWidth() - filterButtonWidth - this.getScrollBarWidth() - 2;
         FormContentIconButton configureFilterButton = this.addComponent(
            new FormContentIconButton(
               buttonX,
               y + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().container_storage_config,
               new GameMessage[]{new LocalMessage("ui", "settlerfilterequipment")}
            ) {
               @Override
               public boolean isActive() {
                  return super.isActive() && data.selfManagesEquipment(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel(), false);
               }
            }
         );
         configureFilterButton.onClicked(
            e -> {
               if (data.equipmentData != null) {
                  GameMessage header = new StaticMessage(data.getSettlerName(this.equipmentsForm.client.getLevel(), "N/A"));
                  EquipmentFiltersForm filterForm = new EquipmentFiltersForm(
                     "changeequipmentfilter",
                     408,
                     300,
                     header,
                     data.equipmentData.equipmentFilter,
                     () -> data.equipmentData.preferArmorSets,
                     this.equipmentsForm.client
                  ) {
                     @Override
                     public void onSetPreferArmorSets(boolean preferArmorSets) {
                        data.equipmentData.preferArmorSets = preferArmorSets;
                        SettlementEquipmentContentBox.this.equipmentsForm
                           .container
                           .setSettlerEquipmentFilter
                           .runAndSendPreferArmorSets(data.mobUniqueID, preferArmorSets);
                     }

                     @Override
                     public void onItemsChanged(Item[] items, boolean allowed) {
                        SettlementEquipmentContentBox.this.equipmentsForm
                           .container
                           .setSettlerEquipmentFilter
                           .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.itemsAllowed(items, allowed));
                     }

                     @Override
                     public void onCategoryChanged(ItemCategoriesFilter.ItemCategoryFilter category, boolean allowed) {
                        SettlementEquipmentContentBox.this.equipmentsForm
                           .container
                           .setSettlerEquipmentFilter
                           .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.categoryAllowed(category, allowed));
                     }

                     @Override
                     public void onFullChange(ItemCategoriesFilter filter) {
                        SettlementEquipmentContentBox.this.equipmentsForm
                           .container
                           .setSettlerEquipmentFilter
                           .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.fullChange(filter));
                     }

                     @Override
                     public void onButtonPressed() {
                        SettlementEquipmentContentBox.this.equipmentsForm.makeCurrent(SettlementEquipmentContentBox.this.equipmentsForm.mainForm);
                     }

                     @Override
                     public void onWindowResized(GameWindow window) {
                        super.onWindowResized(window);
                        ContainerComponent.setPosInventory(this);
                     }
                  };
                  this.equipmentsForm.addAndMakeCurrentTemporary(filterForm);
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
                  return super.isActive()
                     && data.equipmentData != null
                     && data.selfManagesEquipment(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel(), false);
               }
            }
         );
         pasteButton.onClicked(
            e -> {
               if (data.equipmentData != null) {
                  EquipmentFiltersForm.EquipmentFilterData equipmentFilterData = this.equipmentsForm.listClipboard.getValue();
                  if (equipmentFilterData != null) {
                     SaveData save = new SaveData("");
                     equipmentFilterData.filter.addSaveData(save);
                     data.equipmentData.preferArmorSets = equipmentFilterData.preferArmorSets;
                     data.equipmentData.equipmentFilter.applyLoadData(save.toLoadData());
                     this.equipmentsForm.container.setSettlerEquipmentFilter.runAndSendPreferArmorSets(data.mobUniqueID, data.equipmentData.preferArmorSets);
                     this.equipmentsForm
                        .container
                        .setSettlerEquipmentFilter
                        .runAndSendChange(data.mobUniqueID, ItemCategoriesFilterChange.fullChange(data.equipmentData.equipmentFilter));
                     pasteButton.setActive(false);
                  }
               }
            }
         );
         pasteButton.setupDragPressOtherButtons("equipmentPasteButton");
         this.pasteButtons
            .add(
               new SettlementEquipmentForm.PasteButton(
                  pasteButton,
                  () -> data.equipmentData == null || data.equipmentData.preferArmorSets,
                  () -> data.equipmentData == null ? null : data.equipmentData.equipmentFilter
               )
            );
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
                  return super.isActive()
                     && data.equipmentData != null
                     && data.selfManagesEquipment(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel(), false);
               }
            }
         );
         copyButton.onClicked(
            e -> {
               if (data.equipmentData != null) {
                  EquipmentFiltersForm.EquipmentFilterData equipmentFilterData = new EquipmentFiltersForm.EquipmentFilterData(
                     data.equipmentData.preferArmorSets, data.equipmentData.equipmentFilter
                  );
                  WindowManager.getWindow().putClipboard(equipmentFilterData.getSaveData().getScript());
                  this.equipmentsForm.listClipboard.forceUpdate();
               }
            }
         );
         buttonX -= 24;
         FormContentIconToggleButton selfManageButton = this.addComponent(
            new FormContentIconToggleButton(
               buttonX,
               y + 3,
               FormInputSize.SIZE_24,
               ButtonColor.BASE,
               this.getInterfaceStyle().button_checked_20,
               this.getInterfaceStyle().button_escaped_20,
               new GameMessage[]{new LocalMessage("ui", "settlerselfmanagequipment")}
            ) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  this.setToggled(data.selfManagesEquipment(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel(), true));
                  super.draw(tickManager, perspective, renderBox);
               }
            }
         );
         selfManageButton.onToggled(e -> {
            SettlerMob mob = data.getSettlerMob(this.equipmentsForm.client.getLevel());
            if (mob != null) {
               mob.setManageEquipmentThemself(e.from.isToggled());
            }

            if (data.equipmentData != null) {
               data.equipmentData.selfManageEquipment = e.from.isToggled();
            }

            this.equipmentsForm.container.setSettlerSelfManageEquipment.runAndSend(data.mobUniqueID, e.from.isToggled());
         });
         selfManageButton.setupDragToOtherButtons("selfManageEquipment");
         buttonX -= 4;
         buttonX -= 32;
         this.addArmorItemPreview(data, buttonX, y, SettlerMob::getSettlerBootsCosmetic, gear -> gear.bootsCosmetic, ArmorItem.ArmorType.FEET, true);
         buttonX -= 32;
         this.addArmorItemPreview(data, buttonX, y, SettlerMob::getSettlerChestCosmetic, gear -> gear.chestCosmetic, ArmorItem.ArmorType.CHEST, true);
         buttonX -= 32;
         this.addArmorItemPreview(data, buttonX, y, SettlerMob::getSettlerHeadCosmetic, gear -> gear.headCosmetic, ArmorItem.ArmorType.HEAD, true);
         buttonX -= 4;
         this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, buttonX, y + 4, 24, false));
         buttonX -= 4;
         buttonX -= 32;
         this.addArmorItemPreview(data, buttonX, y, SettlerMob::getSettlerBootsArmor, gear -> gear.bootsArmor, ArmorItem.ArmorType.FEET, false);
         buttonX -= 32;
         this.addArmorItemPreview(data, buttonX, y, SettlerMob::getSettlerChestArmor, gear -> gear.chestArmor, ArmorItem.ArmorType.CHEST, false);
         buttonX -= 32;
         this.addArmorItemPreview(data, buttonX, y, SettlerMob::getSettlerHeadArmor, gear -> gear.headArmor, ArmorItem.ArmorType.HEAD, false);
         buttonX -= 4;
         this.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, buttonX, y + 4, 24, false));
         buttonX -= 4;
         buttonX -= 32;
         this.addWeaponItemPreview(data, buttonX, y);
         this.addComponent(new FormSettlerDataIcon(5, y, data, this.equipmentsForm.containerForm));
         int namesX = 37;
         int namesWidth = buttonX - namesX;
         FontOptions nameFontOptions = new FontOptions(16);
         this.addComponent(
            new FormSettlerDataNameLabel(this.equipmentsForm.containerForm.getClient(), data, nameFontOptions, FairType.TextAlign.LEFT, namesX, y, namesWidth)
         );
         FontOptions settlerOptions = new FontOptions(12);
         this.addComponent(new FormLabel(GameUtils.maxString(data.settler.getGenericMobName(), settlerOptions, namesWidth), settlerOptions, -1, namesX, y + 16));
      }
   }

   private void addArmorItemPreview(
      final SettlementSettlerData data,
      int x,
      int y,
      final Function<SettlerMob, InventoryItem> settlerGetter,
      final Function<SettlerGearData, InventoryItem> gearGetter,
      final ArmorItem.ArmorType armorType,
      final boolean isCosmeticSlot
   ) {
      final GameTexture emptyIcon;
      if (armorType == ArmorItem.ArmorType.HEAD) {
         emptyIcon = isCosmeticSlot ? this.getInterfaceStyle().inventoryslot_icon_hat : this.getInterfaceStyle().inventoryslot_icon_helmet;
      } else if (armorType == ArmorItem.ArmorType.CHEST) {
         emptyIcon = isCosmeticSlot ? this.getInterfaceStyle().inventoryslot_icon_shirt : this.getInterfaceStyle().inventoryslot_icon_chestplate;
      } else if (armorType == ArmorItem.ArmorType.FEET) {
         emptyIcon = isCosmeticSlot ? this.getInterfaceStyle().inventoryslot_icon_shoes : this.getInterfaceStyle().inventoryslot_icon_boots;
      } else {
         emptyIcon = null;
      }

      this.addComponent(new FormItemPreview(x, y, 32, (Item)null) {
         @Override
         public InventoryItem getDrawItem(PlayerMob perspective) {
            SettlerMob mob = data.getSettlerMob(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel());
            if (mob != null) {
               return settlerGetter.apply(mob);
            } else {
               return data.gearData != null ? gearGetter.apply(data.gearData) : null;
            }
         }

         @Override
         public void drawEmpty(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
            if (emptyIcon != null) {
               emptyIcon.initDraw().color(this.getInterfaceStyle().inactiveTextColor).draw(this.getX(), this.getY());
            }
         }

         @Override
         public boolean allowControllerFocus() {
            return true;
         }

         @Override
         public void addTooltips(InventoryItem item, PlayerMob perspective) {
            ListGameTooltips tooltips = new ListGameTooltips();
            if (item != null) {
               if (item.item.isArmorItem()) {
                  GameBlackboard blackboard = new GameBlackboard().set("isCosmeticSlot", isCosmeticSlot);
                  SettlerMob mob = data.getSettlerMob(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel());
                  if (mob != null) {
                     GameBlackboard buffBlackboard = new GameBlackboard();
                     buffBlackboard.set("setItem", item);
                     blackboard.set("equippedMob", mob).set("setBonus", mob.getSettlerGearBuffManager().getSetBonusBuffTooltip(buffBlackboard));
                  } else if (data.gearData != null) {
                     blackboard.set("setHelmet", data.gearData.headArmor).set("setChest", data.gearData.chestArmor).set("setBoots", data.gearData.bootsArmor);
                  }

                  tooltips.add(item.item.getTooltips(item, perspective, blackboard));
               } else {
                  tooltips.add(Localization.translate("ui", "settlercantuseitem"));
               }
            } else if (armorType == ArmorItem.ArmorType.HEAD) {
               tooltips.add(Localization.translate("itemtooltip", (isCosmeticSlot ? "cosmetic" : "") + "headslot"));
            } else if (armorType == ArmorItem.ArmorType.CHEST) {
               tooltips.add(Localization.translate("itemtooltip", (isCosmeticSlot ? "cosmetic" : "") + "chestslot"));
            } else if (armorType == ArmorItem.ArmorType.FEET) {
               tooltips.add(Localization.translate("itemtooltip", (isCosmeticSlot ? "cosmetic" : "") + "feetslot"));
            }

            GameTooltipManager.addTooltip(tooltips, GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
         }
      });
   }

   private void addWeaponItemPreview(final SettlementSettlerData data, int x, int y) {
      this.addComponent(
         new FormItemPreview(x, y, 32, (Item)null) {
            @Override
            public InventoryItem getDrawItem(PlayerMob perspective) {
               SettlerMob mob = data.getSettlerMob(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel());
               if (mob != null) {
                  return mob.getSettlerWeapon();
               } else {
                  return data.gearData != null ? data.gearData.weapon : null;
               }
            }

            @Override
            public void drawEmpty(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               this.getInterfaceStyle().inventoryslot_icon_weapon.initDraw().color(this.getInterfaceStyle().inactiveTextColor).draw(this.getX(), this.getY());
            }

            @Override
            public boolean allowControllerFocus() {
               return true;
            }

            @Override
            public void addTooltips(InventoryItem item, PlayerMob perspective) {
               ListGameTooltips tooltips = new ListGameTooltips();
               if (item != null) {
                  if (item.item instanceof ItemAttackerWeaponItem) {
                     GameBlackboard blackboard = new GameBlackboard()
                        .set("perspective", data.getSettlerMob(SettlementEquipmentContentBox.this.equipmentsForm.client.getLevel()));
                     tooltips.add(item.item.getTooltips(item, null, blackboard));
                  } else {
                     tooltips.add(Localization.translate("ui", "settlercantuseitem"));
                  }
               } else {
                  tooltips.add(Localization.translate("itemtooltip", "weaponslot"));
               }

               GameTooltipManager.addTooltip(tooltips, GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS);
            }
         }
      );
   }

   public void updatePasteButtons(EquipmentFiltersForm.EquipmentFilterData value) {
      for (SettlementEquipmentForm.PasteButton pasteButton : this.pasteButtons) {
         pasteButton.updateActive(value);
      }
   }

   @Override
   public void dispose() {
      lastScroll.save(this);
      super.dispose();
   }
}
