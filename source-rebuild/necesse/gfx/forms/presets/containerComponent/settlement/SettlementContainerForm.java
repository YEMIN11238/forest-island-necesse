package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameTool.GameToolManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.Zoning;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLocalTextButtonToggle;
import necesse.gfx.forms.components.FormSettlerDataIcon;
import necesse.gfx.forms.presets.containerComponent.ContainerFormList;
import necesse.gfx.forms.presets.containerComponent.SelectSettlersContainerGameTool;
import necesse.gfx.forms.presets.containerComponent.SelectedSettlersHandler;
import necesse.gfx.forms.presets.containerComponent.settlement.diets.SettlementDietsForm;
import necesse.gfx.forms.presets.containerComponent.settlement.equipment.SettlementEquipmentForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.HUD;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.data.SettlementClientDataManager;
import necesse.inventory.container.settlement.data.SettlementLockedBedData;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.data.SettlerCommandData;
import necesse.inventory.container.settlement.events.SettlementDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerCommandDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupDeletedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupFullEvent;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.hudManager.MapHudDrawElement;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementContainerForm<T extends SettlementContainer> extends ContainerFormList<T> {
   public static String lastOpenType;
   private final Form menuBar;
   private final FormSwitcher contentSwitcher;
   private final LinkedList<SettlementSubForm> menus;
   private final SettlementPrivateForm privateForm;
   public SettlementClientDataManager dataManager;
   public FormLocalTextButtonToggle settlerMenuButton;
   public final SettlementSettlersForm<T> settlersForm;
   public final SettlementCommandsForm<T> commandsForm;
   public final SelectedSettlersHandler selectedSettlers;
   public FormContentBox settlersBar;
   public ArrayList<SelectSettlersContainerGameTool.HudCommandMob> settlerBarCommandMobs = new ArrayList<>();
   protected boolean shouldUpdateSettlersBar;
   public SettlementContainerGameTool tool;
   protected HudDrawElement boundsHudDrawElement;
   protected List<HudDrawElement> settlerHudDrawElements = new ArrayList<>();

   public SettlementContainerForm(final Client client, final T container) {
      super(client, container);
      this.dataManager = new SettlementClientDataManager(container, client);
      this.selectedSettlers = new SelectedSettlersHandler(client) {
         @Override
         public void updateSelectedSettlers(boolean switchToCommandForm) {
            Collection<Integer> selectedSettlers = this.get();
            if (!selectedSettlers.isEmpty()) {
               HashSet<Integer> requests = new HashSet<>();

               for (int mobUniqueID : selectedSettlers) {
                  SettlementSettlerData settler = SettlementContainerForm.this.dataManager.getSettler(mobUniqueID);
                  if (settler != null) {
                     SettlerCommandData commandData = settler.updateCommandData(this.client.getLevel(), true);
                     if (commandData == null) {
                        requests.add(mobUniqueID);
                     }
                  }
               }

               if (!requests.isEmpty()) {
                  container.requestSettlerCommandDataAction.runAndSend(selectedSettlers);
               }
            }

            SettlementContainerForm.this.updateSelectedSettlers(switchToCommandForm);
         }
      };
      this.menuBar = this.addComponent(new Form("menubar", 800, 40));
      this.contentSwitcher = this.addComponent(new FormSwitcher());
      this.menus = new LinkedList<>();
      this.menus.add(new SettlementSettingsForm<>(client, container, this));
      this.menus.add(this.settlersForm = new SettlementSettlersForm<>(client, container, this));
      this.menus.add(this.commandsForm = new SettlementCommandsForm<>(client, this));
      this.menus.add(new SettlementEquipmentForm<>(client, container, this));
      this.menus.add(new SettlementDietsForm<>(client, container, this));
      this.menus.add(new SettlementRestrictForm<>(client, container, this));
      this.menus.add(new SettlementWorkPrioritiesForm<>(client, container, this));
      this.menus.add(new SettlementAssignWorkForm<>(client, container, this));

      for (SettlementSubForm menu : this.menus) {
         this.contentSwitcher.addComponent((FormComponent)menu, (c, active) -> ((SettlementSubForm)c).onSetCurrent(active));
      }

      this.updateMenuBar();
      this.settlersBar = this.addComponent(
         new FormContentBox(5, 30, 100, 5) {
            @Override
            public boolean isMouseOver(InputEvent event) {
               if (event.isMouseWheelEvent()) {
                  Rectangle contentBox = this.getContentBox();
                  return new Rectangle(this.getX(), this.getY(), Math.min(this.getWidth(), contentBox.width), Math.min(this.getHeight(), contentBox.height))
                     .contains(event.pos.hudX, event.pos.hudY);
               }

               if (this.hasScrollbarX() && this.isMouseOverScrollbarX(event)) {
                  return true;
               }

               if (this.hasScrollbarY() && this.isMouseOverScrollbarY(event)) {
                  return true;
               }

               InputEvent offsetEvent = this.getComponentList().offsetEvent(event, false);
               return this.getComponents().stream().anyMatch(c -> c.isMouseOver(offsetEvent));
            }

            @Override
            public boolean shouldDraw() {
               return super.shouldDraw() && !client.isDrawingCredits() && SettlementContainerForm.this.menuBar.shouldDraw();
            }
         },
         -100
      );
      this.settlersBar.drawHorizontalOnTop = true;
      this.settlersBar.drawVerticalOnLeft = true;
      this.settlersBar.hitboxFullSize = false;
      this.privateForm = this.contentSwitcher.addComponent(new SettlementPrivateForm(container.settlementData, container.requestJoin));
      this.onWindowResized(WindowManager.getWindow());
   }

   @Override
   protected void init() {
      this.dataManager.init();
      this.container.onEvent(SettlementDataEvent.class, e -> this.updatePrivateForm());
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         synchronized (this.selectedSettlers) {
            Set<Integer> possibleSelected = this.dataManager.streamSettlers().map(d -> d.mobUniqueID).collect(Collectors.toSet());
            this.selectedSettlers.cleanUp(possibleSelected::contains);
         }

         this.updateSelectedSettlers(false);
         this.shouldUpdateSettlersBar = true;
      });
      this.container.onEvent(SettlementSettlerCommandDataEvent.class, event -> {
         for (int mobUniqueID : event.setterData.keySet()) {
            if (this.selectedSettlers.contains(mobUniqueID)) {
               this.commandsForm.updateSelectedForm();
               break;
            }
         }
      });
      this.container.onEvent(SettlementSettlerGroupChangedEvent.class, event -> this.updateSelectedSettlers(false));
      this.container.onEvent(SettlementSettlerGroupFullEvent.class, event -> this.updateSelectedSettlers(false));
      this.container.onEvent(SettlementSettlerGroupDeletedEvent.class, event -> this.updateSelectedSettlers(false));
      this.selectedSettlers.init();
      if (this.boundsHudDrawElement != null) {
         this.boundsHudDrawElement.remove();
      }

      this.boundsHudDrawElement = this.client
         .getLevel()
         .hudManager
         .addElement(
            new MapHudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
                  if (SettlementContainerForm.this.container.settlementData != null) {
                     Rectangle levelRectangle = SettlementBoundsManager.getLevelRectangleFromTier(
                        SettlementContainerForm.this.container.settlementData.tileX,
                        SettlementContainerForm.this.container.settlementData.tileY,
                        SettlementContainerForm.this.container.settlementData.flagTier
                     );
                     final SharedTextureDrawOptions options = Zoning.getRectangleDrawOptions(
                        levelRectangle, new Color(0.2F, 0.2F, 0.6F, 0.5F), new Color(0.2F, 0.2F, 0.6F, 0.0F), 128, camera
                     );
                     list.add(new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return -100000;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           options.draw();
                        }
                     });
                  }
               }

               @Override
               public Rectangle getMapLevelDrawBounds() {
                  if (SettlementContainerForm.this.container.settlementData == null) {
                     return null;
                  }

                  Rectangle levelRectangle = SettlementBoundsManager.getLevelRectangleFromTier(
                     SettlementContainerForm.this.container.settlementData.tileX,
                     SettlementContainerForm.this.container.settlementData.tileY,
                     SettlementContainerForm.this.container.settlementData.flagTier
                  );
                  levelRectangle.x = levelRectangle.x - GameMath.getLevelCoordinate(SettlementContainerForm.this.container.settlementData.tileX);
                  levelRectangle.y = levelRectangle.y - GameMath.getLevelCoordinate(SettlementContainerForm.this.container.settlementData.tileY);
                  return levelRectangle;
               }

               @Override
               public Point getMapLevelPos() {
                  return SettlementContainerForm.this.container.settlementData == null
                     ? new Point()
                     : new Point(
                        GameMath.getLevelCoordinate(SettlementContainerForm.this.container.settlementData.tileX),
                        GameMath.getLevelCoordinate(SettlementContainerForm.this.container.settlementData.tileY)
                     );
               }

               @Override
               public void drawOnMap(
                  TickManager tickManager,
                  Client client,
                  int drawX,
                  int drawY,
                  double tileScale,
                  Rectangle drawBounds,
                  boolean isMinimap,
                  int mouseHudX,
                  int mouseHudY
               ) {
                  if (SettlementContainerForm.this.container.settlementData != null) {
                     Rectangle tileRectangle = SettlementBoundsManager.getTileRectangleFromTier(
                        SettlementContainerForm.this.container.settlementData.tileX,
                        SettlementContainerForm.this.container.settlementData.tileY,
                        SettlementContainerForm.this.container.settlementData.flagTier
                     );
                     tileRectangle.x = tileRectangle.x - SettlementContainerForm.this.container.settlementData.tileX;
                     tileRectangle.y = tileRectangle.y - SettlementContainerForm.this.container.settlementData.tileY;
                     tileRectangle.x = (int)(tileRectangle.x * tileScale);
                     tileRectangle.y = (int)(tileRectangle.y * tileScale);
                     tileRectangle.width = (int)(tileRectangle.width * tileScale);
                     tileRectangle.height = (int)(tileRectangle.height * tileScale);
                     SharedTextureDrawOptions options = Zoning.getRectangleDrawOptions(
                        tileRectangle, new Color(0.2F, 0.2F, 0.6F, 0.7F), new Color(0.2F, 0.2F, 0.6F, 0.0F), (int)(4.0 * tileScale), drawX, drawY
                     );
                     options.draw();
                  }
               }
            }
         );
      this.updatePrivateForm();
      if (lastOpenType != null) {
         if (!this.contentSwitcher.isCurrent(this.privateForm)) {
            for (SettlementSubForm menu : this.menus) {
               if (lastOpenType.equals(menu.getTypeString())) {
                  menu.onMenuButtonClicked(this.contentSwitcher);
                  break;
               }
            }
         } else {
            lastOpenType = null;
         }
      }

      super.init();
   }

   private void updateMenuBar() {
      GameWindow window = WindowManager.getWindow();
      int minWidth = Math.min(50 * this.menus.size(), 200);
      int maxWidth = window.getHudWidth() - 200;
      this.menuBar.setWidth(GameMath.limit(200 * this.menus.size(), minWidth, Math.max(maxWidth, minWidth)));
      this.menuBar.clearComponents();
      int usedWidth = 4;
      int i = 0;

      for (final SettlementSubForm menu : this.menus) {
         int remainingButtons = this.menus.size() - i;
         int remainingWidth = this.menuBar.getWidth() - 4 - usedWidth;
         int width = remainingWidth / remainingButtons;
         FormLocalTextButtonToggle button = this.menuBar
            .addComponent(new FormLocalTextButtonToggle(menu.getMenuButtonName(), usedWidth, 0, width, FormInputSize.SIZE_32_TO_40, ButtonColor.BASE) {
               @Override
               public boolean isToggled() {
                  return SettlementContainerForm.this.contentSwitcher.isCurrent((FormComponent)menu);
               }
            });
         button.controllerFocusHashcode = "settlementMenuButton" + menu.getTypeString();
         button.onClicked(e -> {
            e.preventDefault();
            this.playTickSound();
            if (this.contentSwitcher.isCurrent((FormComponent)menu)) {
               this.contentSwitcher.clearCurrent();
               lastOpenType = null;
            } else {
               menu.onMenuButtonClicked(this.contentSwitcher);
               lastOpenType = menu.getTypeString();
            }
         });
         if (menu == this.settlersForm) {
            this.settlerMenuButton = button;
         }

         usedWidth += width;
         i++;
      }

      FormContentIconButton exitButton = this.menuBar
         .addComponent(
            new FormContentIconButton(
               this.menuBar.getWidth() - 4,
               0,
               32,
               FormInputSize.SIZE_32_TO_40,
               ButtonColor.RED,
               this.getInterfaceStyle().button_cross,
               new LocalMessage("ui", "settlementexit")
            )
         );
      exitButton.onClicked(e -> this.client.closeContainer(true));
      this.menuBar.setWidth(this.menuBar.getWidth() + exitButton.getWidth());
      this.menuBar.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() - this.menuBar.getHeight() / 2 - this.getInterfaceStyle().formSpacing - 28);
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.updatePrivateFormActive(false);
      if (this.shouldUpdateSettlersBar) {
         this.updateSettlersBar();
      }

      super.draw(tickManager, perspective, renderBox);
   }

   private void updatePrivateForm() {
      this.privateForm.updateContent(this.container.settlementData);
      this.updatePrivateFormActive(true);
   }

   private void updateSettlersBar() {
      this.shouldUpdateSettlersBar = false;
      this.settlerHudDrawElements.forEach(HudDrawElement::remove);
      this.settlerHudDrawElements.clear();
      GameWindow window = WindowManager.getWindow();
      int settlersBarMaxY = Math.min(window.getHudHeight() - 100, this.menuBar.getY());
      FormManager manager = this.getManager();
      if (manager instanceof MainGameFormManager) {
         MainGameFormManager mainGameFormManager = (MainGameFormManager)manager;
         settlersBarMaxY = Math.min(mainGameFormManager.chat.getY(), settlersBarMaxY);
         settlersBarMaxY = Math.min(mainGameFormManager.settlementNotifications.getY(), settlersBarMaxY);
      }

      this.settlersBar.setHeight(Math.max(settlersBarMaxY - this.settlersBar.getY(), 100));
      this.settlersBar.clearComponents();
      this.settlerBarCommandMobs.clear();
      int totalSettlers = this.dataManager.getTotalSettlers();
      final FontOptions fontOptions = new FontOptions(12).color(Color.WHITE).outline();
      int topPadding = 8;
      int settlerHeight = 32 + (fontOptions.getSize() + 2) * 2 + topPadding;
      int settlerWidth = 64;
      int totalRows = Math.max((this.settlersBar.getHeight() - topPadding) / settlerHeight, 1);

      int totalColumns;
      for (totalColumns = totalSettlers / totalRows + (totalSettlers % totalRows == 0 ? 0 : 1);
         totalColumns > 4;
         totalColumns = totalSettlers / totalRows + (totalSettlers % totalRows == 0 ? 0 : 1)
      ) {
         totalRows++;
      }

      int currentSettlerIndex = 0;

      for (final SettlementSettlerData data : this.dataManager.getSettlers()) {
         int currentRow = currentSettlerIndex / totalColumns;
         int currentCol = currentSettlerIndex % totalColumns;
         int currentX = currentCol * settlerWidth;
         final int currentY = topPadding + currentRow * settlerHeight;
         final int iconX = currentX + settlerWidth / 2 - 16;
         this.settlersBar.addComponent(new FormCustomDraw(iconX, currentY, 32, 32) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               GameBackground.itemTooltip.getDrawOptions(this.getX(), this.getY(), this.width, this.height).forEachDraw(e -> e.alpha(0.5F)).draw();
            }
         }, -10);
         final FormSettlerDataIcon formIcon = this.settlersBar.addComponent(new FormSettlerDataIcon(iconX, currentY, data, this));
         HudDrawElement hoverElement = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
               if (formIcon.isHovering()) {
                  SettlerMob settlerMob = data.getSettlerMob(SettlementContainerForm.this.client.getLevel());
                  if (settlerMob != null) {
                     final DrawOptions options = SettlementContainerForm.this.getSelectedMobDrawOptions(settlerMob.getMob(), camera);
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
         this.settlerHudDrawElements.add(hoverElement);
         this.client.getLevel().hudManager.addElement(hoverElement);
         this.settlersBar.addComponent(new FormCustomDraw(currentX, currentY + 32 + 2, settlerWidth, fontOptions.getSize()) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               String settlerName = data.getSettlerName(SettlementContainerForm.this.client.getLevel(), null);
               if (settlerName != null) {
                  settlerName = GameUtils.maxString(settlerName, fontOptions, 64);
                  int textWidth = FontManager.bit.getWidthCeil(settlerName, fontOptions);
                  FontManager.bit.drawString(this.getX() + this.width / 2 - textWidth / 2, this.getY(), settlerName, fontOptions);
               }
            }
         });
         this.settlersBar.addComponent(new FormCustomDraw(currentX, currentY + 32 + 2 + fontOptions.getSize() + 2, settlerWidth, fontOptions.getSize()) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               String typeName = data.settler.getGenericMobName();
               if (typeName != null) {
                  FontOptions typeFontOptions = new FontOptions(fontOptions).alphaf(0.7F);
                  typeName = GameUtils.maxString(typeName, typeFontOptions, 64);
                  int textWidth = FontManager.bit.getWidthCeil(typeName, typeFontOptions);
                  FontManager.bit.drawString(this.getX() + this.width / 2 - textWidth / 2, this.getY(), typeName, typeFontOptions);
               }
            }
         });
         this.settlerBarCommandMobs
            .add(
               new SelectSettlersContainerGameTool.HudCommandMob() {
                  @Override
                  public int getMobUniqueID() {
                     return data.mobUniqueID;
                  }

                  @Override
                  public boolean canBeSelected(boolean isByBoxSelection) {
                     return isByBoxSelection;
                  }

                  @Override
                  public CommandMob getMob() {
                     SettlerMob mob = data.getSettlerMob(SettlementContainerForm.this.client.getLevel());
                     return mob instanceof CommandMob && SettlementContainerForm.this.settlersBar.isContentWithinScroll(new Rectangle(iconX, currentY, 32, 32))
                        ? (CommandMob)mob
                        : null;
                  }

                  @Override
                  public Rectangle getLevelSelectBox(GameCamera camera) {
                     GameWindow windowx = WindowManager.getWindow();
                     float ratioX = (float)windowx.getSceneWidth() / windowx.getHudWidth();
                     float ratioY = (float)windowx.getSceneHeight() / windowx.getHudHeight();
                     int width = (int)(32.0F * ratioX);
                     int height = (int)(32.0F * ratioY);
                     InputPosition offset = InputPosition.fromHudPos(
                        windowx,
                        SettlementContainerForm.this.settlersBar.getX() + iconX + 10,
                        SettlementContainerForm.this.settlersBar.getY() + currentY - SettlementContainerForm.this.settlersBar.getScrollY()
                     );
                     return new Rectangle(camera.getMouseLevelPosX(offset), camera.getMouseLevelPosY(offset), width, height);
                  }
               }
            );
         HudDrawElement flagElement = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
               if (data.bedPosition != null) {
                  if (SettlementContainerForm.this.shouldShowBedIcon(data)) {
                     final DrawOptions drawOptions = SettlementSettlerData.getSettlerFlagDrawOptionsTile(
                        data.bedPosition.x,
                        data.bedPosition.y,
                        camera,
                        (size, drawX, drawY) -> data.getIconDrawOptions(SettlementContainerForm.this.client.getLevel(), size, drawX, drawY)
                     );
                     final boolean isMouseOver = !SettlementContainerForm.this.getManager().isMouseOver()
                        && data.settler.isMouseOverSettlerFlag(data.bedPosition.x, data.bedPosition.y, camera);
                     SettlerMob settlerMob = data.getSettlerMob(SettlementContainerForm.this.client.getLevel());
                     final DrawOptions selectedOptions = isMouseOver && settlerMob != null
                        ? SettlementContainerForm.this.getSelectedMobDrawOptions(settlerMob.getMob(), camera)
                        : null;
                     list.add(new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return Integer.MAX_VALUE;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           if (selectedOptions != null) {
                              selectedOptions.draw();
                           }

                           if (isMouseOver) {
                              String settlerName = data.getSettlerName(SettlementContainerForm.this.client.getLevel(), null);
                              if (settlerName != null) {
                                 GameTooltipManager.addTooltip(new StringTooltips(settlerName), TooltipLocation.FORM_FOCUS);
                              }
                           }

                           drawOptions.draw();
                        }
                     });
                  }
               }
            }
         };
         this.settlerBarCommandMobs
            .add(
               new SelectSettlersContainerGameTool.HudCommandMob() {
                  @Override
                  public int getMobUniqueID() {
                     return data.mobUniqueID;
                  }

                  @Override
                  public boolean canBeSelected(boolean isByBoxSelection) {
                     if (data.bedPosition == null) {
                        return false;
                     } else {
                        return !SettlementContainerForm.this.shouldShowBedIcon(data)
                           ? false
                           : SelectSettlersContainerGameTool.HudCommandMob.super.canBeSelected(isByBoxSelection);
                     }
                  }

                  @Override
                  public CommandMob getMob() {
                     SettlerMob mob = data.getSettlerMob(SettlementContainerForm.this.client.getLevel());
                     return mob instanceof CommandMob && SettlementContainerForm.this.settlersBar.isContentWithinScroll(new Rectangle(iconX, currentY, 32, 32))
                        ? (CommandMob)mob
                        : null;
                  }

                  @Override
                  public Rectangle getLevelSelectBox(GameCamera camera) {
                     return new Rectangle(GameMath.getLevelCoordinate(data.bedPosition.x), GameMath.getLevelCoordinate(data.bedPosition.y), 32, 32);
                  }
               }
            );
         this.settlerHudDrawElements.add(flagElement);
         this.client.getLevel().hudManager.addElement(flagElement);
         currentSettlerIndex++;
      }

      this.settlersBar.setWidth(totalColumns * settlerWidth);
      this.settlersBar.setContentBox(new Rectangle(this.settlersBar.getWidth(), totalRows * settlerHeight + topPadding));
      this.addLockedRooms(this.dataManager.lockedBeds);
   }

   public boolean shouldShowBedIcons() {
      return this.contentSwitcher.isCurrent(this.settlersForm) || this.settlersForm.isEitherLockingOrAssigningBed();
   }

   public boolean shouldShowBedIcon(SettlementSettlerData settlerData) {
      return this.shouldShowBedIcons() || this.selectedSettlers.contains(settlerData.mobUniqueID);
   }

   private void addLockedRooms(List<SettlementLockedBedData> list) {
      for (final SettlementLockedBedData bed : list) {
         HudDrawElement element = new HudDrawElement() {
            @Override
            public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
               if (SettlementContainerForm.this.shouldShowBedIcons()) {
                  final DrawOptions drawOptions = SettlementSettlerData.getSettlerFlagDrawOptionsTile(
                     bed.tileX, bed.tileY, camera, (size, drawX, drawY) -> SettlerRegistry.SETTLER_LOCKED.getSettlerFaceDrawOptions(drawX, drawY, size, null)
                  );
                  list.add(new SortedDrawable() {
                     @Override
                     public int getPriority() {
                        return Integer.MAX_VALUE;
                     }

                     @Override
                     public void draw(TickManager tickManager) {
                        drawOptions.draw();
                     }
                  });
               }
            }
         };
         this.settlerHudDrawElements.add(element);
         this.client.getLevel().hudManager.addElement(element);
      }
   }

   private void updatePrivateFormActive(boolean forceUpdate) {
      boolean hasAccess = this.container.hasSettlementAccess(this.client);
      this.dataManager.updateSubscriptions(hasAccess);
      if (!hasAccess) {
         if (forceUpdate || !this.contentSwitcher.isCurrent(this.privateForm)) {
            this.contentSwitcher.makeCurrent(this.privateForm);
            this.menuBar.setHidden(true);
            if (!this.selectedSettlers.isEmpty()) {
               this.selectedSettlers.clear();
            }

            if (this.tool != null) {
               GameToolManager.clearGameTool(this.tool);
            }

            this.tool = null;
         }
      } else if (forceUpdate || this.contentSwitcher.isCurrent(this.privateForm)) {
         if (this.contentSwitcher.isCurrent(this.privateForm)) {
            this.contentSwitcher.clearCurrent();
         }

         this.menuBar.setHidden(false);
         if (this.tool == null) {
            GameToolManager.setGameTool(this.tool = new SettlementContainerGameTool(this.client, this.selectedSettlers, this.container, this));
         }
      }
   }

   public DrawOptions getSelectedMobDrawOptions(Mob mob, GameCamera camera) {
      DrawOptionsList options = new DrawOptionsList();
      ArrayList<String> lines = new ArrayList<>(Collections.singleton(mob.getDisplayName()));
      PlayerMob player = this.client.getPlayer();
      options.add(HUD.getDirectionIndicator(player.x, player.y, mob, 200, 5.0F, lines, new FontOptions(16).outline().color(200, 200, 200), camera));
      options.add(HUD.levelBoundOptions(camera, mob.getSelectBox()));
      return options;
   }

   public boolean isCurrent(SettlementSubForm form) {
      return this.contentSwitcher.isCurrent((FormComponent)form);
   }

   public SettlementSubForm getCurrentSubForm() {
      FormComponent current = this.contentSwitcher.getCurrent();
      return current instanceof SettlementSubForm ? (SettlementSubForm)current : null;
   }

   public SettlementToolHandler getCurrentToolHandler() {
      SettlementSubForm current = this.getCurrentSubForm();
      return current != null ? current.getToolHandler() : null;
   }

   public void updateSelectedSettlers(boolean switchToCommandForm) {
      if (this.container.hasSettlementAccess(this.client)) {
         if (!this.selectedSettlers.isEmpty()) {
            if (switchToCommandForm && !this.contentSwitcher.isCurrent(this.commandsForm) || this.contentSwitcher.getCurrent() == null) {
               this.commandsForm.onMenuButtonClicked(this.contentSwitcher);
               lastOpenType = this.commandsForm.getTypeString();
            }

            this.commandsForm.updateNoneSelectedForm();
            this.commandsForm.updateSelectedForm();
         } else if (this.contentSwitcher.isCurrent(this.commandsForm)) {
            this.commandsForm.updateNoneSelectedForm();
            this.commandsForm.updateCurrentForm();
         }

         WindowManager.getWindow().getInput().submitNextMoveEvent();
      }
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.updateMenuBar();
      this.shouldUpdateSettlersBar = true;
   }

   @Override
   public void dispose() {
      this.selectedSettlers.dispose();
      GameToolManager.clearGameTool(this.tool);
      this.settlerHudDrawElements.forEach(HudDrawElement::remove);
      this.settlerHudDrawElements.clear();
      if (this.boundsHudDrawElement != null) {
         this.boundsHudDrawElement.remove();
         this.boundsHudDrawElement = null;
      }

      super.dispose();
   }

   @Override
   public boolean shouldOpenInventory() {
      return false;
   }

   @Override
   public boolean shouldShowInventory() {
      return false;
   }

   @Override
   public boolean shouldShowToolbar() {
      return false;
   }

   @Override
   public boolean shouldShowSidebar() {
      return false;
   }
}
