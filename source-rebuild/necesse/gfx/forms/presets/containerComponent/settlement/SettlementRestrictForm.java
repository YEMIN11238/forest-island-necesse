package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Zoning;
import necesse.engine.util.ZoningChange;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentButton;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormDropdownButton;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormLabelEdit;
import necesse.gfx.forms.components.FormMouseHover;
import necesse.gfx.forms.components.FormSettlerDataIcon;
import necesse.gfx.forms.components.FormSettlerDataNameLabel;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.SavedFormContentBoxScroll;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.floatMenu.ColorHueSelectorFloatMenu;
import necesse.gfx.forms.presets.ConfirmationForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.data.SettlementRestrictZoneData;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.events.SettlementNewSettlerRestrictZoneChangedEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZoneChangedEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZoneRecolorEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZoneRenameEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZonesFullEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerRestrictZoneDataEvent;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;

public class SettlementRestrictForm<T extends SettlementContainer> extends FormSwitcher implements SettlementSubForm {
   public static SavedFormContentBoxScroll lastSettlersScroll = new SavedFormContentBoxScroll();
   public static SavedFormContentBoxScroll lastZonesScroll = new SavedFormContentBoxScroll();
   public static boolean manageLastOpen;
   public final Client client;
   public final T container;
   public final SettlementContainerForm<T> containerForm;
   public int maxHeight;
   public int settlersContentHeight;
   public int zonesContentHeight;
   protected FormSwitcher setCurrentWhenLoaded;
   protected HashMap<Integer, SettlementRestrictForm.RestrictZone> zones;
   protected int newSettlerRestrictZoneUniqueID;
   protected Form settlersForm;
   protected Form zonesForm;
   protected FormContentBox settlersContent;
   protected FormContentBox zonesContent;
   protected FormTextInput searchInput;
   protected ConfirmationForm deleteConfirm;
   protected FormDropdownButton allSettlersSelectButton;
   protected FormDropdownSelectionButton<Integer> newSettlerSelectButton;
   protected FormLocalTextButton manageZonesButton;
   protected FormLocalTextButton createNewZoneButton;
   protected FormLocalTextButton zonesBackButton;
   protected ArrayList<SettlementRestrictForm.SelectButton> settlerSelectButtons = new ArrayList<>();
   protected List<HudDrawElement> hudElements = new ArrayList<>();
   protected int currentEditZoneUniqueID;
   protected int currentColorEditZoneUniqueID;

   public SettlementRestrictForm(Client client, T container, SettlementContainerForm<T> containerForm) {
      this.client = client;
      this.container = container;
      this.containerForm = containerForm;
      this.maxHeight = 300;
      this.deleteConfirm = this.addComponent(new ConfirmationForm("delete", 300, 200));
      this.settlersForm = this.addComponent(new Form("settlers", 500, 300));
      FormFlow settlersFlow = new FormFlow(5);
      this.settlersForm
         .addComponent(new FormLocalLabel("ui", "settlementrestrict", new FontOptions(20), 0, this.settlersForm.getWidth() / 2, settlersFlow.next(25)));
      this.settlersForm
         .addComponent(
            new FormContentIconButton(this.settlersForm.getWidth() - 25, 5, FormInputSize.SIZE_20, ButtonColor.BASE, this.getInterfaceStyle().button_help_20) {
               @Override
               public GameTooltips getTooltips(PlayerMob perspective) {
                  return new StringTooltips(Localization.translate("ui", "settlementrestricthelp"), 400);
               }
            }
         );
      int selectButtonWidth = 250;
      int newSettlersButtonX = this.settlersForm.getWidth() - selectButtonWidth - this.getInterfaceStyle().scrollbar.active.getHeight() - 2;
      int allSettlersY = settlersFlow.next(30);
      this.allSettlersSelectButton = this.settlersForm
         .addComponent(new FormDropdownButton(newSettlersButtonX, allSettlersY + 3, FormInputSize.SIZE_24, ButtonColor.BASE, selectButtonWidth));
      this.settlersForm.addComponent(new FormLocalLabel("ui", "settlementallsettlers", new FontOptions(20), -1, 5, allSettlersY + 5));
      int newSettlersY = settlersFlow.next(30);
      this.newSettlerSelectButton = this.settlersForm
         .addComponent(new FormDropdownSelectionButton<>(newSettlersButtonX, newSettlersY + 3, FormInputSize.SIZE_24, ButtonColor.BASE, selectButtonWidth));
      this.newSettlerSelectButton.onSelected(e -> container.setNewSettlerRestrictZone.runAndSend(e.value));
      this.newSettlerSelectButton.setupDragToOtherButtons("restrictSelectionButton", o -> true);
      this.settlersForm.addComponent(new FormLocalLabel("ui", "settlementnewsettlers", new FontOptions(20), -1, 5, newSettlersY + 5));
      this.settlersForm.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, 0, settlersFlow.next(4), this.settlersForm.getWidth(), true));
      this.searchInput = this.settlersForm
         .addComponent(new FormTextInput(4, settlersFlow.next(28), FormInputSize.SIZE_24, this.settlersForm.getWidth() - 8, -1, 500));
      this.searchInput.placeHolder = new LocalMessage("ui", "searchtip");
      this.searchInput.rightClickToClear = true;
      this.searchInput.rightClickToClearTooltip = new LocalMessage("controls", "clearsearchtip");
      if (SettlementSettlersForm.lastSettlersSearch != null) {
         this.searchInput.setText(SettlementSettlersForm.lastSettlersSearch);
      }

      this.searchInput.onChange(e -> {
         SettlementSettlersForm.lastSettlersSearch = this.searchInput.getText();
         this.updateSettlersContent();
      });
      int settlersContentHeight = 210;
      this.settlersContent = this.settlersForm
         .addComponent(new FormContentBox(0, settlersFlow.next(settlersContentHeight), this.settlersForm.getWidth(), settlersContentHeight));
      this.manageZonesButton = this.settlersForm
         .addComponent(
            new FormLocalTextButton(
               "ui", "settlementmanageareas", 4, this.settlersForm.getHeight() - 28, this.settlersForm.getWidth() - 8, FormInputSize.SIZE_24, ButtonColor.BASE
            )
         );
      this.manageZonesButton.onClicked(e -> {
         this.makeCurrent(this.zonesForm);
         manageLastOpen = true;
      });
      this.zonesForm = this.addComponent(new Form("zones", 500, 300));
      this.zonesContent = this.zonesForm.addComponent(new FormContentBox(0, 0, this.zonesForm.getWidth(), this.zonesForm.getHeight() - 30));
      this.zonesContent.addComponent(new FormLocalLabel("ui", "settlementrestrict", new FontOptions(20), 0, this.zonesForm.getWidth() / 2, 5));
      int backButtonWidth = 150;
      this.createNewZoneButton = this.zonesForm
         .addComponent(
            new FormLocalTextButton(
               "ui",
               "settlementareanew",
               4,
               this.zonesForm.getHeight() - 28,
               this.zonesForm.getWidth() - backButtonWidth - 6,
               FormInputSize.SIZE_24,
               ButtonColor.BASE
            )
         );
      this.createNewZoneButton.setCooldown(1000);
      this.createNewZoneButton.onClicked(e -> container.createNewRestrictZone.runAndSend());
      this.zonesBackButton = this.zonesForm
         .addComponent(
            new FormLocalTextButton(
               "ui",
               "backbutton",
               this.zonesForm.getWidth() - backButtonWidth + 2,
               this.zonesForm.getHeight() - 28,
               backButtonWidth - 6,
               FormInputSize.SIZE_24,
               ButtonColor.BASE
            )
         );
      this.zonesBackButton.onClicked(e -> {
         this.makeCurrent(this.settlersForm);
         manageLastOpen = false;
         GameToolManager.clearGameTools(this);
      });
   }

   @Override
   protected void init() {
      super.init();
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         if (this.containerForm.isCurrent(this)) {
            this.containerForm.dataManager.requestSettlerData(this.container.restrictSubscription, s -> s.restrictZoneData == null);
            this.container.requestFullRestricts.runAndSend();
         }
      });
      this.container.onEvent(SettlementSettlerRestrictZoneDataEvent.class, event -> {
         this.containerForm.dataManager.applySettlerData(event, (data, addon) -> data.restrictZoneData = addon);
         if (this.containerForm.isCurrent(this)) {
            this.updateSettlersContent();
         }
      });
      this.container
         .onEvent(
            SettlementRestrictZonesFullEvent.class,
            event -> {
               if (this.setCurrentWhenLoaded != null) {
                  this.setCurrentWhenLoaded.makeCurrent(this);
               }

               this.setCurrentWhenLoaded = null;
               if (this.containerForm.isCurrent(this)) {
                  this.zones = new HashMap<>();

                  for (Entry<Integer, SettlementRestrictZoneData> e : event.zones.entrySet()) {
                     this.zones
                        .put(
                           e.getKey(),
                           new SettlementRestrictForm.RestrictZone(
                              e.getValue(),
                              () -> SettlementBoundsManager.getTileRectangleFromTier(
                                 this.container.settlementData.tileX, this.container.settlementData.tileY, this.container.settlementData.flagTier
                              )
                           )
                        );
                  }

                  this.newSettlerRestrictZoneUniqueID = event.newSettlerRestrictZoneUniqueID;
                  this.hudElements.forEach(HudDrawElement::remove);
                  this.hudElements.clear();
                  this.updateSettlersContent();
                  this.updateZonesContent();
                  this.updateSize();
                  this.checkValidEditTool();
                  if (this.containerForm.dataManager.getTotalSettlers() != 0) {
                     lastSettlersScroll.load(this.settlersContent);
                  }

                  if (!event.zones.isEmpty()) {
                     lastZonesScroll.load(this.zonesContent);
                  }
               }
            }
         );
      this.container.onEvent(SettlementNewSettlerRestrictZoneChangedEvent.class, event -> {
         this.newSettlerRestrictZoneUniqueID = event.restrictZoneUniqueID;
         this.updateSelectButton(this.newSettlerSelectButton, this.newSettlerRestrictZoneUniqueID);
      });
      this.container.onEvent(SettlementRestrictZoneChangedEvent.class, event -> {
         if (this.zones != null) {
            SettlementRestrictForm.RestrictZone zone = this.zones.get(event.restrictZoneUniqueID);
            if (zone != null) {
               synchronized (zone.zoning) {
                  event.change.applyTo(zone.zoning);
               }
            } else {
               this.container.requestFullRestricts.runAndSend();
            }
         }
      });
      this.container.onEvent(SettlementRestrictZoneRenameEvent.class, event -> {
         if (this.zones != null) {
            SettlementRestrictForm.RestrictZone zone = this.zones.get(event.restrictZoneUniqueID);
            if (zone != null) {
               zone.name = event.name;
               if (zone.labelEdit != null && !zone.labelEdit.isTyping()) {
                  zone.labelEdit.setText(zone.name.translate());
               }

               this.updateSelectButtons();
            } else {
               this.container.requestFullRestricts.runAndSend();
            }
         }
      });
      this.container.onEvent(SettlementRestrictZoneRecolorEvent.class, event -> {
         if (this.zones != null) {
            SettlementRestrictForm.RestrictZone zone = this.zones.get(event.restrictZoneUniqueID);
            if (zone != null) {
               zone.colorHue = event.hue;
            } else {
               this.container.requestFullRestricts.runAndSend();
            }
         }
      });
   }

   public void updateSettlersContent() {
      this.settlerSelectButtons.clear();
      this.settlersContent.clearComponents();
      FormFlow flow = new FormFlow(0);
      String searchLower = SettlementSettlersForm.lastSettlersSearch == null ? "" : SettlementSettlersForm.lastSettlersSearch.toLowerCase();
      List<SettlementSettlerData> settlers = this.containerForm.dataManager.streamSettlers().filter(data -> data.restrictZoneData != null).filter(data -> {
         if (data.matchesSearch(searchLower, this.client.getLevel(), this.client.getPlayer())) {
            return true;
         }

         if (data.restrictZoneData.restrictZoneUniqueID == 0) {
            return Localization.translate("ui", "settlementunrestricted").toLowerCase().contains(searchLower);
         }

         SettlementRestrictForm.RestrictZone currentZone = this.zones.get(data.restrictZoneData.restrictZoneUniqueID);
         return currentZone != null ? currentZone.name.translate().toLowerCase().contains(searchLower) : false;
      }).collect(Collectors.toList());
      if (!settlers.isEmpty()) {
         this.addSettlers(flow, settlers);
      }

      if (settlers.isEmpty()) {
         this.settlersContent.alwaysShowVerticalScrollBar = false;
         flow.next(16);
         if (this.containerForm.dataManager.getTotalSettlers() == 0) {
            this.settlersContent
               .addComponent(
                  flow.nextY(
                     new FormLocalLabel(
                        "ui", "settlersnosettlers", new FontOptions(16), 0, this.settlersForm.getWidth() / 2, 0, this.settlersForm.getWidth() - 20
                     ),
                     16
                  )
               );
         } else {
            this.settlersContent
               .addComponent(
                  flow.nextY(
                     new FormLocalLabel(
                        "ui", "settlersnosettlerssearch", new FontOptions(16), 0, this.settlersForm.getWidth() / 2, 0, this.settlersForm.getWidth() - 20
                     ),
                     16
                  )
               );
         }
      } else {
         this.settlersContent.alwaysShowVerticalScrollBar = true;
      }

      this.settlersContentHeight = Math.max(flow.next(), 100);
      this.updateSelectButtons();
      this.updateSize();
      ControllerInput.submitNextRefreshFocusEvent();
   }

   private void addSettlers(FormFlow flow, List<SettlementSettlerData> list) {
      for (final SettlementSettlerData data : list) {
         int padding = SettlementSettlersForm.SETTLER_LIST_PADDING;
         int height = 32 + padding * 2;
         int startY = flow.next(height);
         int y = startY + padding;
         final FormMouseHover mouseHover = this.settlersContent
            .addComponent(new FormMouseHover(0, y, this.settlersContent.getWidth(), height), Integer.MAX_VALUE);
         int selectButtonWidth = 250;
         int buttonX = this.settlersForm.getWidth() - selectButtonWidth - this.settlersContent.getScrollBarWidth() - 2;
         final FormDropdownSelectionButton<Integer> selectionButton = this.settlersContent
            .addComponent(new FormDropdownSelectionButton<Integer>(buttonX, y + 3, FormInputSize.SIZE_24, ButtonColor.BASE, selectButtonWidth) {
               @Override
               public boolean isActive() {
                  return super.isActive() && data.restrictZoneData != null;
               }
            });
         this.settlerSelectButtons.add(new SettlementRestrictForm.SelectButton(data, selectionButton));
         selectionButton.onSelected(e -> {
            if (data.restrictZoneData != null) {
               this.container.setSettlerRestrictZone.runAndSend(data.mobUniqueID, e.value);
               data.restrictZoneData.restrictZoneUniqueID = e.value;
            }
         });
         selectionButton.controllerFocusHashcode = "settlerrestrictselection" + data.mobUniqueID;
         selectionButton.setupDragToOtherButtons("restrictSelectionButton", o -> true);
         HudDrawElement hoverElement = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
               if (data.restrictZoneData != null) {
                  if (mouseHover.isHovering() || SettlementRestrictForm.this.isControllerFocus(selectionButton)) {
                     SettlementRestrictForm.RestrictZone zone = SettlementRestrictForm.this.zones.get(data.restrictZoneData.restrictZoneUniqueID);
                     if (zone != null) {
                        Color edgeColor = Color.getHSBColor(zone.colorHue / 360.0F, 0.8F, 0.6F);
                        Color fillColor = Color.getHSBColor(zone.colorHue / 360.0F, 0.8F, 0.8F);
                        synchronized (zone.zoning) {
                           final SharedTextureDrawOptions options = zone.zoning
                              .getDrawOptions(edgeColor, new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 75), camera);
                           if (options != null) {
                              list.add(new SortedDrawable() {
                                 @Override
                                 public int getPriority() {
                                    return 2147482647;
                                 }

                                 @Override
                                 public void draw(TickManager tickManager) {
                                    options.draw();
                                 }
                              });
                           }
                        }
                     }
                  }
               }
            }
         };
         this.hudElements.add(hoverElement);
         this.client.getLevel().hudManager.addElement(hoverElement);
         this.settlersContent.addComponent(new FormSettlerDataIcon(5, y, data, this.containerForm));
         int namesX = 37;
         int namesWidth = buttonX - namesX;
         FontOptions nameFontOptions = new FontOptions(16);
         this.settlersContent
            .addComponent(new FormSettlerDataNameLabel(this.containerForm.getClient(), data, nameFontOptions, FairType.TextAlign.LEFT, namesX, y, namesWidth));
         FontOptions settlerOptions = new FontOptions(12);
         this.settlersContent
            .addComponent(new FormLabel(GameUtils.maxString(data.settler.getGenericMobName(), settlerOptions, namesWidth), settlerOptions, -1, namesX, y + 16));
      }
   }

   public void updateZonesContent() {
      this.zonesContent.clearComponents();
      FormFlow flow = new FormFlow(5);
      this.zonesContent.addComponent(new FormLocalLabel("ui", "settlementmanageareas", new FontOptions(20), 0, this.zonesForm.getWidth() / 2, flow.next(25)));
      ArrayList<SettlementRestrictForm.RestrictZone> sortedZones = new ArrayList<>(this.zones.values());
      sortedZones.sort(Comparator.comparingInt(z -> z.index));

      for (final SettlementRestrictForm.RestrictZone zone : sortedZones) {
         int height = 24;
         final FormMouseHover mouseHover = this.zonesContent
            .addComponent(new FormMouseHover(0, flow.next(), this.zonesContent.getWidth(), height), Integer.MAX_VALUE);
         FontOptions nameOptions = new FontOptions(16);
         zone.labelEdit = this.zonesContent
            .addComponent(new FormLabelEdit(zone.name.translate(), nameOptions, this.getInterfaceStyle().activeTextColor, 5, flow.next() + 4, 100, 30), -1000);
         int buttonX = this.zonesContent.getWidth() - 24 - this.zonesContent.getScrollBarWidth() - 2;
         final FormContentIconButton deleteButton = this.zonesContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  flow.next(),
                  FormInputSize.SIZE_24,
                  ButtonColor.RED,
                  this.getInterfaceStyle().container_storage_remove,
                  new LocalMessage("ui", "deletebutton")
               )
            );
         deleteButton.onClicked(e -> {
            this.deleteConfirm.setupConfirmation(new LocalMessage("ui", "settlementareadeleteconfirm", "zone", zone.name.translate()), () -> {
               this.container.deleteRestrictZone.runAndSend(zone.uniqueID);
               this.makeCurrent(this.zonesForm);
            }, () -> this.makeCurrent(this.zonesForm));
            this.makeCurrent(this.deleteConfirm);
         });
         deleteButton.controllerFocusHashcode = "zonedelete" + zone.uniqueID;
         buttonX -= 24;
         final FormContentIconButton cloneButton = this.zonesContent
            .addComponent(
               new FormContentIconButton(
                  buttonX, flow.next(), FormInputSize.SIZE_24, ButtonColor.BASE, this.getInterfaceStyle().copy_button, new LocalMessage("ui", "clonebutton")
               )
            );
         cloneButton.onClicked(e -> this.container.cloneRestrictZone.runAndSend(zone.uniqueID));
         cloneButton.setActive(this.zones.size() < ServerSettlementData.MAX_RESTRICT_ZONES);
         cloneButton.controllerFocusHashcode = "zoneclone" + zone.uniqueID;
         buttonX -= 24;
         final FormContentIconButton invertButton = this.zonesContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  flow.next(),
                  FormInputSize.SIZE_24,
                  ButtonColor.BASE,
                  this.getInterfaceStyle().inventory_sort,
                  new LocalMessage("ui", "settlementareainvert")
               )
            );
         invertButton.onClicked(e -> {
            synchronized (zone.zoning) {
               zone.zoning.invert();
            }

            this.container.changeRestrictZone.runAndSend(zone.uniqueID, ZoningChange.fullInvert());
         });
         invertButton.controllerFocusHashcode = "zoneinvert" + zone.uniqueID;
         buttonX -= 24;
         final FormContentIconButton configureButton = this.zonesContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  flow.next(),
                  FormInputSize.SIZE_24,
                  ButtonColor.BASE,
                  this.getInterfaceStyle().container_storage_config,
                  new LocalMessage("ui", "configurebutton")
               )
            );
         configureButton.onClicked(e -> this.startEditZoneTool(zone));
         buttonX -= 24;
         final FormContentButton colorButton = this.zonesContent
            .addComponent(new FormContentButton(buttonX, flow.next(), 24, FormInputSize.SIZE_24, ButtonColor.BASE) {
               @Override
               protected void drawContent(int x, int y, int width, int heightx) {
                  Color drawColor = this.getDrawColor();
                  float[] hsb = Color.RGBtoHSB(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(), null);
                  Color color = Color.getHSBColor(zone.colorHue / 360.0F, 0.8F, hsb[2]);
                  Renderer.initQuadDraw(width, heightx).color(color).draw(x, y);
               }

               @Override
               protected void addTooltips(PlayerMob perspective) {
                  super.addTooltips(perspective);
                  GameTooltipManager.addTooltip(new StringTooltips(Localization.translate("ui", "changecolorbutton")), TooltipLocation.FORM_FOCUS);
               }
            });
         colorButton.onClicked(e -> {
            this.currentColorEditZoneUniqueID = zone.uniqueID;
            final int startHue = zone.colorHue;
            e.from.getManager().openFloatMenu(new ColorHueSelectorFloatMenu(e.from, 150, 24, zone.colorHue / 360.0F) {
               @Override
               public void onChanged(float hue) {
                  zone.colorHue = (int)(hue * 360.0F);
               }

               @Override
               public void dispose() {
                  super.dispose();
                  int nextHue = (int)(this.picker.getSelectedHue() * 360.0F);
                  if (startHue != nextHue) {
                     SettlementRestrictForm.this.container.recolorRestrictZone.runAndSend(zone.uniqueID, nextHue);
                     zone.colorHue = nextHue;
                  }

                  SettlementRestrictForm.this.currentColorEditZoneUniqueID = 0;
               }
            }, colorButton, e.event, 0, 0);
         });
         colorButton.controllerFocusHashcode = "zonecolor" + zone.uniqueID;
         buttonX -= 24;
         final FormContentIconButton renameButton = this.zonesContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  flow.next(),
                  FormInputSize.SIZE_24,
                  ButtonColor.BASE,
                  this.getInterfaceStyle().container_rename,
                  new LocalMessage("ui", "renamebutton")
               )
            );
         AtomicBoolean isTyping = new AtomicBoolean(false);
         zone.labelEdit.onMouseChangedTyping(e -> {
            isTyping.set(zone.labelEdit.isTyping());
            this.runRenameUpdate(zone, zone.labelEdit, renameButton);
         });
         zone.labelEdit.onSubmit(e -> {
            isTyping.set(zone.labelEdit.isTyping());
            this.runRenameUpdate(zone, zone.labelEdit, renameButton);
         });
         renameButton.onClicked(e -> {
            isTyping.set(!zone.labelEdit.isTyping());
            zone.labelEdit.setTyping(!zone.labelEdit.isTyping());
            this.runRenameUpdate(zone, zone.labelEdit, renameButton);
         });
         this.runRenameUpdate(zone, zone.labelEdit, renameButton);
         renameButton.controllerFocusHashcode = "zonerename" + zone.uniqueID;
         HudDrawElement hoverElement = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
               if (mouseHover.isHovering()
                  || SettlementRestrictForm.this.isControllerFocus(deleteButton)
                  || SettlementRestrictForm.this.isControllerFocus(cloneButton)
                  || SettlementRestrictForm.this.isControllerFocus(invertButton)
                  || SettlementRestrictForm.this.isControllerFocus(configureButton)
                  || SettlementRestrictForm.this.isControllerFocus(colorButton)
                  || SettlementRestrictForm.this.isControllerFocus(renameButton)
                  || SettlementRestrictForm.this.currentEditZoneUniqueID == zone.uniqueID
                  || SettlementRestrictForm.this.currentColorEditZoneUniqueID == zone.uniqueID) {
                  Color edgeColor = Color.getHSBColor(zone.colorHue / 360.0F, 0.8F, 0.6F);
                  Color fillColor = Color.getHSBColor(zone.colorHue / 360.0F, 0.8F, 0.8F);
                  synchronized (zone.zoning) {
                     final SharedTextureDrawOptions options = zone.zoning
                        .getDrawOptions(edgeColor, new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 75), camera);
                     if (options != null) {
                        list.add(new SortedDrawable() {
                           @Override
                           public int getPriority() {
                              return 2147482647;
                           }

                           @Override
                           public void draw(TickManager tickManager) {
                              options.draw();
                           }
                        });
                     }
                  }
               }
            }
         };
         this.hudElements.add(hoverElement);
         this.client.getLevel().hudManager.addElement(hoverElement);
         zone.labelEdit.setWidth(buttonX);
         flow.next(height);
      }

      this.zonesContentHeight = Math.max(flow.next(), 100);
      this.zonesContent.alwaysShowVerticalScrollBar = true;
      this.createNewZoneButton.setActive(this.zones.size() < ServerSettlementData.MAX_RESTRICT_ZONES);
      ControllerInput.submitNextRefreshFocusEvent();
   }

   private void runRenameUpdate(SettlementRestrictForm.RestrictZone zone, FormLabelEdit label, FormContentIconButton renameButton) {
      if (label.isTyping()) {
         renameButton.setIcon(this.getInterfaceStyle().container_rename_save);
         renameButton.setTooltips(new LocalMessage("ui", "savebutton"));
      } else {
         if (!label.getText().equals(zone.name.translate())) {
            if (label.getText().isEmpty()) {
               label.setText(zone.name.translate());
            } else {
               zone.name = new StaticMessage(label.getText());
               this.updateSelectButtons();
               this.container.renameRestrictZone.runAndSend(zone.uniqueID, label.getText());
            }
         }

         renameButton.setIcon(this.getInterfaceStyle().container_rename);
         renameButton.setTooltips(new LocalMessage("ui", "renamebutton"));
      }
   }

   private void checkValidEditTool() {
      if (this.currentEditZoneUniqueID != 0 && this.zones.get(this.currentEditZoneUniqueID) == null) {
         GameToolManager.clearGameTools(this);
      }
   }

   private void startEditZoneTool(final SettlementRestrictForm.RestrictZone zone) {
      this.currentEditZoneUniqueID = zone.uniqueID;
      GameToolManager.clearGameTools(this);
      GameToolManager.setGameTool(new CreateOrExpandGlobalZoneGameTool(this.client.getLevel()) {
         @Override
         public void onExpandedZone(Rectangle rectangle) {
            synchronized (zone.zoning) {
               zone.zoning.addRectangle(rectangle);
            }

            SettlementRestrictForm.this.container.changeRestrictZone.runAndSend(zone.uniqueID, ZoningChange.expand(rectangle));
         }

         @Override
         public void onShrankZone(Rectangle rectangle) {
            synchronized (zone.zoning) {
               zone.zoning.removeRectangle(rectangle);
            }

            SettlementRestrictForm.this.container.changeRestrictZone.runAndSend(zone.uniqueID, ZoningChange.shrink(rectangle));
         }

         @Override
         public void isCancelled() {
            super.isCancelled();
            if (SettlementRestrictForm.this.currentEditZoneUniqueID == zone.uniqueID) {
               SettlementRestrictForm.this.currentEditZoneUniqueID = 0;
            }
         }

         @Override
         public void isCleared() {
            super.isCleared();
            if (SettlementRestrictForm.this.currentEditZoneUniqueID == zone.uniqueID) {
               SettlementRestrictForm.this.currentEditZoneUniqueID = 0;
            }
         }
      }, this);
   }

   private void updateSelectButtons() {
      this.updateSelectButton(this.allSettlersSelectButton, uniqueID -> {
         for (SettlementSettlerData data : this.containerForm.dataManager.getSettlers()) {
            if (data.restrictZoneData != null) {
               this.container.setSettlerRestrictZone.runAndSend(data.mobUniqueID, uniqueID);
               data.restrictZoneData.restrictZoneUniqueID = uniqueID;
            }
         }

         this.updateSelectButtons();
      });
      this.updateSelectButton(this.newSettlerSelectButton, this.newSettlerRestrictZoneUniqueID);

      for (SettlementRestrictForm.SelectButton selectionButton : this.settlerSelectButtons) {
         if (selectionButton.data.restrictZoneData != null) {
            this.updateSelectButton(selectionButton.button, selectionButton.data.restrictZoneData.restrictZoneUniqueID);
         }
      }
   }

   private void updateSelectButton(FormDropdownButton button, Consumer<Integer> onSelected) {
      button.options.clear();
      button.options.add(new LocalMessage("ui", "settlementunrestricted"), () -> onSelected.accept(0));
      this.zones
         .values()
         .stream()
         .sorted(Comparator.comparingInt(z -> z.index))
         .forEach(zone -> button.options.add(zone.name, () -> onSelected.accept(zone.uniqueID)));
   }

   private void updateSelectButton(FormDropdownSelectionButton<Integer> button, int currentZoneUniqueID) {
      button.options.clear();
      button.options.add(0, new LocalMessage("ui", "settlementunrestricted"));
      this.zones.values().stream().sorted(Comparator.comparingInt(z -> z.index)).forEach(zone -> button.options.add(zone.uniqueID, zone.name));
      if (currentZoneUniqueID == 0) {
         button.setSelected(0, new LocalMessage("ui", "settlementunrestricted"));
      } else {
         SettlementRestrictForm.RestrictZone currentZone = this.zones.get(currentZoneUniqueID);
         if (currentZone != null) {
            button.setSelected(currentZoneUniqueID, currentZone.name);
         } else {
            button.setSelected(currentZoneUniqueID, new LocalMessage("ui", "settlementunknownarea"));
         }
      }
   }

   public void updateSize() {
      this.settlersForm.setHeight(Math.min(this.maxHeight, this.settlersContent.getY() + this.settlersContentHeight + 30));
      this.settlersContent.setContentBox(new Rectangle(0, 0, this.settlersContent.getWidth(), this.settlersContentHeight));
      this.settlersContent.setWidth(this.settlersForm.getWidth());
      this.settlersContent.setHeight(this.settlersForm.getHeight() - this.settlersContent.getY() - 30);
      this.manageZonesButton.setY(this.settlersForm.getHeight() - 27);
      ContainerComponent.setPosInventory(this.settlersForm);
      this.zonesForm.setHeight(Math.min(this.maxHeight, this.zonesContent.getY() + this.zonesContentHeight + 30));
      this.zonesContent.setContentBox(new Rectangle(0, 0, this.zonesContent.getWidth(), this.zonesContentHeight));
      this.zonesContent.setWidth(this.zonesForm.getWidth());
      this.zonesContent.setHeight(this.zonesForm.getHeight() - this.zonesContent.getY() - 30);
      this.createNewZoneButton.setY(this.zonesForm.getHeight() - 27);
      this.zonesBackButton.setY(this.zonesForm.getHeight() - 27);
      ContainerComponent.setPosInventory(this.zonesForm);
   }

   @Override
   public void onSetCurrent(boolean current) {
      this.hudElements.forEach(HudDrawElement::remove);
      this.hudElements.clear();
      this.settlersContent.clearComponents();
      this.zonesContent.clearComponents();
      this.zones = null;
      if (SettlementSettlersForm.lastSettlersSearch != null) {
         this.searchInput.setText(SettlementSettlersForm.lastSettlersSearch, false);
      }

      if (current) {
         this.container.restrictSubscription.ensureSubscribed();
         this.makeCurrent(manageLastOpen ? this.zonesForm : this.settlersForm);
      } else {
         this.container.restrictSubscription.ensureUnsubscribed();
      }
   }

   @Override
   public void onMenuButtonClicked(FormSwitcher switcher) {
      this.setCurrentWhenLoaded = switcher;
      this.container.requestFullRestricts.runAndSend();
      this.container.restrictSubscription.ensureSubscribed();
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.updateSize();
   }

   @Override
   public void dispose() {
      GameToolManager.clearGameTools(this);
      this.hudElements.forEach(HudDrawElement::remove);
      this.hudElements.clear();
      lastSettlersScroll.save(this.settlersContent);
      lastZonesScroll.save(this.zonesContent);
      super.dispose();
   }

   @Override
   public GameMessage getMenuButtonName() {
      return new LocalMessage("ui", "settlementrestrict");
   }

   @Override
   public String getTypeString() {
      return "restrict";
   }

   protected static class RestrictZone {
      public final int uniqueID;
      public int index;
      public int colorHue;
      public GameMessage name;
      public final Zoning zoning;
      public FormLabelEdit labelEdit;

      public RestrictZone(SettlementRestrictZoneData data, Supplier<Rectangle> settlementTileRectangleGetter) {
         this.uniqueID = data.uniqueID;
         this.index = data.index;
         this.colorHue = data.colorHue;
         this.name = data.name;
         this.zoning = data.getZoning(settlementTileRectangleGetter);
      }
   }

   protected static class SelectButton {
      public final SettlementSettlerData data;
      public final FormDropdownSelectionButton<Integer> button;

      public SelectButton(SettlementSettlerData data, FormDropdownSelectionButton<Integer> button) {
         this.data = data;
         this.button = button;
      }
   }
}
