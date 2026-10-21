package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameTool.GameTool;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameBackground;
import necesse.gfx.GameColor;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentButton;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabelEdit;
import necesse.gfx.forms.components.FormMouseHover;
import necesse.gfx.forms.components.FormSettlerDataActivityLabel;
import necesse.gfx.forms.components.FormSettlerDataIcon;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.SavedFormContentBoxScroll;
import necesse.gfx.forms.components.lists.FormSettlerHelpList;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.ConfirmationForm;
import necesse.gfx.forms.presets.NoticeForm;
import necesse.gfx.forms.presets.SelectSettlementContinueForm;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.OffsetGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.events.SettlementMoveErrorEvent;
import necesse.inventory.container.settlement.events.SettlementOpenSettlementListEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerOverviewDataEvent;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlersForm<T extends SettlementContainer> extends FormSwitcher implements SettlementSubForm {
   public static int SETTLER_LIST_PADDING = 0;
   public static String lastSettlersSearch = "";
   public static SavedFormContentBoxScroll lastScroll = new SavedFormContentBoxScroll();
   public final Client client;
   public final T container;
   public final SettlementContainerForm<T> containerForm;
   public int maxHeight;
   public int contentHeight;
   protected Form settlers;
   protected Form settlersHelp;
   protected ConfirmationForm banishConfirm;
   protected FormTextInput searchInput;
   protected FormLocalLabel settlersHeader;
   protected FormContentBox settlersContent;
   protected FormLocalTextButton lockNoSettlersButton;
   public FormContentIconButton firstAssignBedButton;
   protected List<HudDrawElement> settlerSelection = new ArrayList<>();
   protected int mobMoveUniqueID = -1;

   public SettlementSettlersForm(Client client, T container, SettlementContainerForm<T> containerForm) {
      this.client = client;
      this.container = container;
      this.containerForm = containerForm;
      this.maxHeight = 300;
      this.banishConfirm = this.addComponent(new ConfirmationForm("banish", 300, 200));
      this.settlers = this.addComponent(new Form("settlers", 500, 300));
      FormFlow flow = new FormFlow(5);
      this.settlersHeader = this.settlers
         .addComponent(new FormLocalLabel("ui", "settlers", new FontOptions(20), 0, this.settlers.getWidth() / 2, flow.next(30)));
      this.searchInput = this.settlers.addComponent(new FormTextInput(4, flow.next(28), FormInputSize.SIZE_24, this.settlers.getWidth() - 8, -1, 500));
      this.searchInput.placeHolder = new LocalMessage("ui", "searchtip");
      this.searchInput.rightClickToClear = true;
      this.searchInput.rightClickToClearTooltip = new LocalMessage("controls", "clearsearchtip");
      if (lastSettlersSearch != null) {
         this.searchInput.setText(lastSettlersSearch);
      }

      this.searchInput.onChange(e -> {
         lastSettlersSearch = this.searchInput.getText();
         this.updateSettlers();
      });
      this.settlers
         .addComponent(
            new FormContentIconButton(
               this.settlers.getWidth() - 25,
               5,
               FormInputSize.SIZE_20,
               ButtonColor.BASE,
               this.getInterfaceStyle().button_help_20,
               new LocalMessage("ui", "settlershelpbutton")
            )
         )
         .onClicked(e -> this.makeCurrent(this.settlersHelp));
      int settlersContentHeight = 240;
      this.settlersContent = this.settlers
         .addComponent(new FormContentBox(0, flow.next(settlersContentHeight), this.settlers.getWidth(), settlersContentHeight));
      this.lockNoSettlersButton = this.settlers
         .addComponent(
            new FormLocalTextButton("ui", "settlementlockbed", 4, flow.next(30), this.settlers.getWidth() - 8, FormInputSize.SIZE_24, ButtonColor.BASE)
         );
      this.lockNoSettlersButton.onClicked(e -> this.startLockBedTool(client.getLevel()));
      this.settlers.setHeight(flow.next());
      this.settlersHelp = this.addComponent(new Form(400, 200));
      this.settlersHelp.addComponent(new FormLocalLabel("ui", "settlers", new FontOptions(20), -1, 10, 5));
      this.settlersHelp.addComponent(new FormSettlerHelpList(0, 40, this.settlersHelp.getWidth(), this.settlersHelp.getHeight() - 40));
      this.settlersHelp
         .addComponent(new FormLocalTextButton("ui", "backbutton", this.settlersHelp.getWidth() - 120, 0, 120))
         .onClicked(e -> this.makeCurrent(this.settlers));
   }

   @Override
   protected void init() {
      super.init();
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         if (this.containerForm.isCurrent(this)) {
            this.containerForm.dataManager.requestSettlerData(this.container.requestSettlerOverviewData, s -> s.overviewData == null);
            this.updateSettlers();
         }
      });
      this.container
         .onEvent(
            SettlementSettlerOverviewDataEvent.class,
            event -> this.containerForm.dataManager.applySettlerData(event, (data, addon) -> data.overviewData = addon)
         );
      this.container.onEvent(SettlementOpenSettlementListEvent.class, this::openSettlementList);
      this.container.onEvent(SettlementMoveErrorEvent.class, event -> {
         NoticeForm moveError = new NoticeForm("moveError", 300, 400);
         moveError.setupNotice(event.error);
         this.addComponent(moveError);
         moveError.onContinue(() -> {
            this.removeComponent(moveError);
            this.makeCurrent(this.settlers);
         });
         this.makeCurrent(moveError);
      });
   }

   public void updateSettlers() {
      this.settlerSelection.forEach(HudDrawElement::remove);
      this.settlerSelection.clear();
      this.settlersContent.clearComponents();
      FormFlow settlersFlow = new FormFlow();
      String searchLower = lastSettlersSearch == null ? "" : lastSettlersSearch.toLowerCase();
      List<SettlementSettlerData> settlers = this.containerForm
         .dataManager
         .streamSettlers()
         .filter(data -> data.matchesSearch(searchLower, this.client.getLevel(), this.client.getPlayer()))
         .collect(Collectors.toList());
      if (!settlers.isEmpty()) {
         this.addSettlers(settlersFlow, settlers);
      }

      GameMessageBuilder builder = new GameMessageBuilder().append("ui", "settlers").append(" (" + settlers.size() + ")");
      this.settlersHeader.setLocalization(builder);
      if (settlers.isEmpty()) {
         this.settlersContent.alwaysShowVerticalScrollBar = false;
         settlersFlow.next(16);
         if (this.containerForm.dataManager.getTotalSettlers() == 0) {
            this.settlersContent
               .addComponent(
                  settlersFlow.nextY(
                     new FormLocalLabel(
                        "ui", "settlersnosettlers", new FontOptions(16), 0, this.settlersContent.getWidth() / 2, 0, this.settlersContent.getWidth() - 20
                     ),
                     16
                  )
               );
         } else {
            this.settlersContent
               .addComponent(
                  settlersFlow.nextY(
                     new FormLocalLabel(
                        "ui", "settlersnosettlerssearch", new FontOptions(16), 0, this.settlersContent.getWidth() / 2, 0, this.settlersContent.getWidth() - 20
                     ),
                     16
                  )
               );
         }
      } else {
         this.settlersContent.alwaysShowVerticalScrollBar = true;
      }

      this.contentHeight = Math.max(settlersFlow.next(), 70);
      this.updateSize();
      if (!settlers.isEmpty()) {
         lastScroll.load(this.settlersContent);
      }
   }

   public void updateSize() {
      this.settlers.setHeight(Math.min(this.maxHeight, this.settlersContent.getY() + this.contentHeight + 30));
      this.settlersContent.setContentBox(new Rectangle(0, 0, this.settlersContent.getWidth(), this.contentHeight));
      this.settlersContent.setWidth(this.settlers.getWidth());
      this.settlersContent.setHeight(this.settlers.getHeight() - this.settlersContent.getY() - 30);
      this.lockNoSettlersButton.setPosition(4, this.settlers.getHeight() - 28);
      ContainerComponent.setPosInventory(this.settlers);
   }

   public GameTool startLockBedTool(Level level) {
      GameToolManager.clearGameTools(this);
      LockSettlerBedTool tool = new LockSettlerBedTool(this.container, level);
      GameToolManager.setGameTool(tool, this);
      return tool;
   }

   public GameTool startAssignSettlerBedTool(int mobUniqueID, String name) {
      GameToolManager.clearGameTools(this);
      AssignSettlerBedTool tool = new AssignSettlerBedTool(this.container, this.client.getLevel(), mobUniqueID, name);
      GameToolManager.setGameTool(tool, this);
      return tool;
   }

   public boolean isEitherLockingOrAssigningBed() {
      GameTool tool = GameToolManager.getFirstGameTool(this);
      return tool instanceof LockSettlerBedTool || tool instanceof AssignSettlerBedTool;
   }

   private void addSettlers(FormFlow settlersFlow, List<SettlementSettlerData> list) {
      this.firstAssignBedButton = null;

      for (final SettlementSettlerData data : list) {
         int padding = SETTLER_LIST_PADDING;
         int height = 32 + padding * 2;
         int startY = settlersFlow.next(height);
         int y = startY + padding;
         final FormMouseHover mouseHover = this.settlersContent
            .addComponent(new FormMouseHover(0, startY, this.settlersContent.getWidth(), height), Integer.MAX_VALUE);
         HudDrawElement hoverElement = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
               if (mouseHover.isHovering()) {
                  SettlerMob settlerMob = data.getSettlerMob(SettlementSettlersForm.this.client.getLevel());
                  if (settlerMob != null) {
                     final DrawOptions options = SettlementSettlersForm.this.containerForm.getSelectedMobDrawOptions(settlerMob.getMob(), camera);
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
         };
         this.settlerSelection.add(hoverElement);
         this.client.getLevel().hudManager.addElement(hoverElement);
         this.settlersContent.addComponent(new FormSettlerDataIcon(4, y, data, this.containerForm));
         FontOptions nameOptions = new FontOptions(16);
         String startName = data.getSettlerName(this.client.getLevel(), "N/A");
         FormLabelEdit labelEdit = this.settlersContent
            .addComponent(new FormLabelEdit(startName, nameOptions, this.getInterfaceStyle().activeTextColor, 40, y, 100, 30) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  if (!this.isTyping()) {
                     String name = data.getSettlerName(SettlementSettlersForm.this.client.getLevel(), null);
                     if (name != null && !this.getText().equals(name)) {
                        this.setText(name, false);
                     }
                  }

                  super.draw(tickManager, perspective, renderBox);
               }
            }, -1000);
         int buttonX = this.settlersContent.getWidth() - 24 - this.settlersContent.getScrollBarWidth() - 2;
         FormContentIconButton banishButton = this.settlersContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  y + 4,
                  FormInputSize.SIZE_24,
                  ButtonColor.YELLOW,
                  this.getInterfaceStyle().settler_banish,
                  new GameMessage[]{new LocalMessage("ui", "settlerbanish")}
               ) {
                  @Override
                  public boolean isActive() {
                     return super.isActive() && data.overviewData != null && data.overviewData.canBanish;
                  }

                  @Override
                  public GameTooltips getTooltips(PlayerMob perspective) {
                     return !this.isActive() ? null : super.getTooltips(perspective);
                  }
               }
            );
         banishButton.onClicked(e -> {
            String name = data.getSettlerName(this.client.getLevel(), "N/A");
            this.banishConfirm.setupConfirmation(new LocalMessage("ui", "settlerbanishconfirm", "settler", name), () -> {
               this.container.banishSettler.runAndSend(data.mobUniqueID);
               this.makeCurrent(this.settlers);
            }, () -> this.makeCurrent(this.settlers));
            this.makeCurrent(this.banishConfirm);
         });
         buttonX -= 24;
         FormContentIconButton moveOutButton = this.settlersContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  y + 4,
                  FormInputSize.SIZE_24,
                  ButtonColor.BASE,
                  this.getInterfaceStyle().settler_move_settlement,
                  new GameMessage[]{new LocalMessage("ui", "settlermovesettlement")}
               ) {
                  @Override
                  public boolean isActive() {
                     return super.isActive() && data.overviewData != null && data.overviewData.canMoveOut;
                  }

                  @Override
                  public GameTooltips getTooltips(PlayerMob perspective) {
                     return !this.isActive() ? null : super.getTooltips(perspective);
                  }
               }
            );
         moveOutButton.onClicked(e -> {
            this.mobMoveUniqueID = data.mobUniqueID;
            NoticeForm loadingForm = new NoticeForm("loadingsettlements", 300, 400);
            loadingForm.setButtonCooldown(-2);
            loadingForm.setupNotice(new LocalMessage("ui", "loadingdotdot"));
            this.addComponent(loadingForm, (form, switched) -> {
               if (!switched) {
                  this.removeComponent(form);
               }
            });
            this.makeCurrent(loadingForm);
            this.container.requestMoveSettlerList.runAndSend(this.mobMoveUniqueID);
         });
         buttonX -= 24;
         FormContentIconButton assignBedButton = this.settlersContent
            .addComponent(
               new FormContentIconButton(
                  buttonX,
                  y + 4,
                  FormInputSize.SIZE_24,
                  data.bedPosition == null ? ButtonColor.RED : ButtonColor.BASE,
                  this.getInterfaceStyle().settler_assign_bed,
                  new GameMessage[0]
               ) {
                  @Override
                  public GameTooltips getTooltips(PlayerMob perspective) {
                     ListGameTooltips tooltips = new ListGameTooltips();
                     tooltips.add(Localization.translate("ui", "settlerassignbed"));
                     if (data.bedPosition == null) {
                        tooltips.add(Localization.translate("ui", "settlerhasnobed"));
                     }

                     return tooltips;
                  }
               }
            );
         if (this.firstAssignBedButton == null) {
            this.firstAssignBedButton = assignBedButton;
         }

         assignBedButton.onClicked(e -> this.startAssignSettlerBedTool(data.mobUniqueID, data.getSettlerName(this.client.getLevel(), "N/A")));
         buttonX -= 24;
         FormContentButton happinessIndicator = this.settlersContent
            .addComponent(
               new FormContentButton(buttonX, y + 4, 24, FormInputSize.SIZE_24, ButtonColor.BASE) {
                  @Override
                  protected void drawContent(int x, int yx, int width, int heightx) {
                     SettlerMob mob = data.getSettlerMob(SettlementSettlersForm.this.client.getLevel());
                     if (mob != null) {
                        int happiness = GameMath.limit(mob.getSettlerHappiness(), 0, 100);
                        Color color;
                        if (happiness < HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD) {
                           float clamp = GameMath.clamp(happiness, 0.0F, HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD);
                           color = new Color(GameMath.lerp(clamp, 0.7F, 0.6F), GameMath.lerp(clamp, 0.7F, 0.8F), 0.0F);
                        } else if (happiness >= HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD) {
                           float clamp = GameMath.clamp(happiness, HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD, 100.0F);
                           color = new Color(GameMath.lerp(clamp, 0.3F, 0.0F), 1.0F, 0.0F);
                        } else {
                           float clamp = GameMath.clamp(happiness, HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD, HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD);
                           color = new Color(GameMath.lerp(clamp, 0.4F, 0.2F), 0.9F, 0.0F);
                        }

                        Renderer.initQuadDraw(width, heightx).color(color).draw(x, yx);
                        if (!mob.shouldIgnoreQualityOfLifeAndHappiness()) {
                           int qualityOfLifeLevel = mob.getSettlerQualityOfLife();
                           FontOptions fontOptions = new FontOptions(16).color(this.getInterfaceStyle().activeTextColor);
                           int charWidth = FontManager.bit.getWidthCeil("" + qualityOfLifeLevel, fontOptions);
                           FontManager.bit
                              .drawString(x + width / 2 - charWidth / 2, yx + heightx / 2 - fontOptions.getSize() / 2, "" + qualityOfLifeLevel, fontOptions);
                        }
                     } else {
                        FontOptions fontOptions = new FontOptions(16).color(this.getInterfaceStyle().activeTextColor);
                        int charWidth = FontManager.bit.getWidthCeil('?', fontOptions);
                        FontManager.bit.drawChar(x + width / 2 - charWidth / 2, yx + heightx / 2 - fontOptions.getSize() / 2, '?', fontOptions);
                     }
                  }

                  @Override
                  public boolean isActive() {
                     return false;
                  }

                  @Override
                  public Color getDrawColor() {
                     return Color.WHITE;
                  }

                  @Override
                  protected void addTooltips(PlayerMob perspective) {
                     super.addTooltips(perspective);
                     SettlerMob mob = data.getSettlerMob(SettlementSettlersForm.this.client.getLevel());
                     ListGameTooltips tooltips = new ListGameTooltips();
                     if (mob != null) {
                        if (mob.shouldIgnoreQualityOfLifeAndHappiness()) {
                           return;
                        }

                        int qualityOfLife = mob.getSettlerQualityOfLife();
                        tooltips.add(Settler.getQualityOfLife(qualityOfLife).getDescription().translate());
                        tooltips.add(new SpacerGameTooltip(10));
                        int happiness = GameMath.limit(mob.getSettlerHappiness(), 0, 100);
                        String nameReplacement = GameColor.ITEM_QUEST.getColorCode() + mob.getSettlerName() + GameColor.NO_COLOR.getColorCode();
                        if (happiness < HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD) {
                           tooltips.add(Localization.translate("settlement", "qoldecrease", "name", nameReplacement));
                        } else if (happiness >= HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD) {
                           tooltips.add(Localization.translate("settlement", "qolincrease", "name", nameReplacement));
                        } else {
                           tooltips.add(Localization.translate("settlement", "qolstay", "name", nameReplacement));
                        }

                        tooltips.add(new SpacerGameTooltip(10));
                        if (mob instanceof HumanMob) {
                           HumanMob humanMob = (HumanMob)mob;
                           tooltips.add(Localization.translate("personalities", "personalitiesheader"));
                           tooltips.add(new SpacerGameTooltip(4));
                           ArrayList<DialogueForm.PersonalityData> personalityData = DialogueForm.getPersonalityData(humanMob);
                           tooltips.add(
                              new OffsetGameTooltips(
                                 DialogueForm.toHorizontalOverviewTooltips(personalityData, GameBackground.itemTooltip, Settings.UI.incursionTierPurple, 400),
                                 10,
                                 10
                              )
                           );
                           tooltips.add(new SpacerGameTooltip(10));
                        }

                        tooltips.add(new StringTooltips(Localization.translate("settlement", "moodtalkto"), GameColor.LIGHT_GRAY));
                     } else {
                        tooltips.add(Localization.translate("settlement", "movecloser"));
                     }

                     GameTooltipManager.addTooltip(tooltips, GameBackground.itemTooltip, TooltipLocation.FORM_FOCUS);
                  }
               }
            );
         happinessIndicator.handleClicksIfNoEventHandlers = true;
         buttonX -= 24;
         FormContentIconButton renameButton = this.settlersContent
            .addComponent(new FormContentIconButton(buttonX, y + 4, FormInputSize.SIZE_24, ButtonColor.BASE, this.getInterfaceStyle().container_rename));
         labelEdit.onMouseChangedTyping(e -> this.runRenameUpdate(data, labelEdit, renameButton));
         labelEdit.onSubmit(e -> this.runRenameUpdate(data, labelEdit, renameButton));
         renameButton.onClicked(e -> {
            labelEdit.setTyping(!labelEdit.isTyping());
            this.runRenameUpdate(data, labelEdit, renameButton);
         });
         this.runRenameUpdate(data, labelEdit, renameButton);
         labelEdit.setWidth(buttonX - 40);
         FontOptions activityFontOptions = new FontOptions(12);
         this.settlersContent.addComponent(new FormSettlerDataActivityLabel(this.client, data, activityFontOptions, 40, y + 16, buttonX - 40));
      }
   }

   public void openSettlementList(SettlementOpenSettlementListEvent event) {
      if (this.mobMoveUniqueID != event.mobUniqueID) {
         GameLog.warn.println("Received wrong settlement list for mob id " + event.mobUniqueID);
      } else {
         SelectSettlementContinueForm.Option[] selectOptions = event.options
            .stream()
            .map(o -> new SelectSettlementContinueForm.Option(true, o.settlementUniqueID, o.name) {
               @Override
               public void onSelected(SelectSettlementContinueForm form) {
                  SettlementSettlersForm.this.container.moveSettlerSettlement.runAndSend(event.mobUniqueID, this.settlementUniqueID);
                  SettlementSettlersForm.this.makeCurrent(SettlementSettlersForm.this.settlers);
               }
            })
            .toArray(SelectSettlementContinueForm.Option[]::new);
         SelectSettlementContinueForm selectSettlementForm = new SelectSettlementContinueForm(
            "movetosettlement", 300, 400, new LocalMessage("ui", "settlementselect"), selectOptions
         ) {
            @Override
            public void onCancel() {
               SettlementSettlersForm.this.makeCurrent(SettlementSettlersForm.this.settlers);
               this.removeComponent(this);
            }
         };
         this.addComponent(selectSettlementForm);
         this.makeCurrent(selectSettlementForm);
      }
   }

   private void runRenameUpdate(SettlementSettlerData data, FormLabelEdit label, FormContentIconButton renameButton) {
      if (label.isTyping()) {
         renameButton.setIcon(this.getInterfaceStyle().container_rename_save);
         renameButton.setTooltips(new LocalMessage("ui", "settlersavename"));
      } else {
         String name = data.getSettlerName(this.client.getLevel(), null);
         if (name != null) {
            if (label.getText().isEmpty()) {
               label.setText(name);
            } else {
               if (data.looksData != null) {
                  data.looksData.settlerName = label.getText();
               }

               this.container.renameSettler.runAndSend(data.mobUniqueID, label.getText());
            }
         }

         renameButton.setIcon(this.getInterfaceStyle().container_rename);
         renameButton.setTooltips(new LocalMessage("ui", "settlerchangename"));
      }
   }

   @Override
   public void onSetCurrent(boolean current) {
      this.settlerSelection.forEach(HudDrawElement::remove);
      this.settlerSelection.clear();
      this.settlersContent.clearComponents();
      if (lastSettlersSearch != null) {
         this.searchInput.setText(lastSettlersSearch, false);
      }

      if (current) {
         this.containerForm.dataManager.requestSettlerData(this.container.requestSettlerOverviewData, s -> s.overviewData == null);
         this.updateSettlers();
         this.makeCurrent(this.settlers);
      } else {
         GameToolManager.clearGameTools(this);
      }
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.updateSize();
      ContainerComponent.setPosInventory(this.settlersHelp);
   }

   @Override
   public void dispose() {
      GameToolManager.clearGameTools(this);
      this.settlerSelection.forEach(HudDrawElement::remove);
      this.settlerSelection.clear();
      lastScroll.save(this.settlersContent);
      super.dispose();
   }

   @Override
   public GameMessage getMenuButtonName() {
      return new LocalMessage("ui", "settlementsettlers");
   }

   @Override
   public String getTypeString() {
      return "settlers";
   }
}
