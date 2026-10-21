package necesse.gfx.forms.presets.containerComponent;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameTool.GameTool;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.state.State;
import necesse.engine.util.GameMath;
import necesse.engine.util.Zoning;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.presets.containerComponent.settlement.SettlementToolHandler;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public abstract class SelectSettlersContainerGameTool implements GameTool {
   public static int maxPixelDistanceToConsiderClick = 4;
   public final Client client;
   public final Level level;
   public final SelectedSettlersHandler selectedSettlers;
   protected Point mouseDownPos;
   protected boolean mouseDownIsLeftClick;
   protected HudDrawElement hudElement;
   protected GameWindow.CURSOR cursor;
   protected ListGameTooltips tooltips;
   protected InputEvent ignoreNextInputEvent;

   public SelectSettlersContainerGameTool(Client client, SelectedSettlersHandler selectedSettlers) {
      this.client = client;
      this.level = client.getLevel();
      this.selectedSettlers = selectedSettlers;
   }

   public abstract SettlementToolHandler getCurrentToolHandler();

   public abstract Stream<CommandMob> streamAllLevelSettlers(Rectangle var1);

   public abstract Stream<SelectSettlersContainerGameTool.HudCommandMob> streamAllHudCommandMobs(Rectangle var1);

   public abstract void commandAttack(Mob var1);

   public abstract void commandGuard(int var1, int var2);

   public abstract void commandGuard(ArrayList<Point> var1);

   @Override
   public void init() {
      if (this.hudElement != null) {
         this.hudElement.remove();
      }

      this.level
         .hudManager
         .addElement(
            this.hudElement = new HudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
                  Point mouseDownPos = SelectSettlersContainerGameTool.this.mouseDownPos;
                  if (mouseDownPos != null) {
                     Point mouseReleasePos = camera.getMouseLevelPos();
                     double dist = mouseDownPos.distance(mouseReleasePos);
                     DrawOptions drawOptions = null;
                     SettlementToolHandler toolHandler = SelectSettlersContainerGameTool.this.getCurrentToolHandler();
                     if (dist <= SelectSettlersContainerGameTool.maxPixelDistanceToConsiderClick) {
                        if (toolHandler != null) {
                           if (SelectSettlersContainerGameTool.this.mouseDownIsLeftClick) {
                              drawOptions = toolHandler.getLeftClickDraw(mouseDownPos, mouseReleasePos);
                           } else {
                              drawOptions = toolHandler.getRightClickDraw(mouseDownPos, mouseReleasePos);
                           }
                        }
                     } else {
                        Rectangle rectangle = new Rectangle(
                           Math.min(mouseDownPos.x, mouseReleasePos.x),
                           Math.min(mouseDownPos.y, mouseReleasePos.y),
                           Math.max(Math.abs(mouseDownPos.x - mouseReleasePos.x), 1),
                           Math.max(Math.abs(mouseDownPos.y - mouseReleasePos.y), 1)
                        );
                        if (toolHandler != null) {
                           if (SelectSettlersContainerGameTool.this.mouseDownIsLeftClick) {
                              drawOptions = toolHandler.getLeftClickSelectionDraw(mouseDownPos, mouseReleasePos, rectangle);
                           } else {
                              drawOptions = toolHandler.getRightClickSelectionDraw(mouseDownPos, mouseReleasePos, rectangle);
                           }
                        }

                        if (drawOptions == null) {
                           if (SelectSettlersContainerGameTool.this.mouseDownIsLeftClick) {
                              Color edgeColor = new Color(255, 255, 255, 200);
                              Color fillColor = new Color(255, 255, 255, 20);
                              SharedTextureDrawOptions zoningDrawOptions = Zoning.getRectangleDrawOptions(rectangle, edgeColor, fillColor, 8, camera);
                              drawOptions = zoningDrawOptions::draw;
                           } else if (!SelectSettlersContainerGameTool.this.selectedSettlers.isEmpty()) {
                              ArrayList<Point> movePositions = SelectSettlersContainerGameTool.this.getSettlerMovePositions(mouseDownPos, mouseReleasePos);
                              DrawOptionsList movePositionDraws = new DrawOptionsList();

                              for (Point pos : movePositions) {
                                 Color color = new Color(93, 3, 255);
                                 movePositionDraws.add(SelectSettlersContainerGameTool.this.getActionParticleDrawOptions(camera, pos.x, pos.y, 0.0F, color));
                              }

                              drawOptions = movePositionDraws;
                           }
                        }
                     }

                     if (drawOptions != null) {
                        final DrawOptions finalDrawOptions = drawOptions;
                        list.add(new SortedDrawable() {
                           @Override
                           public int getPriority() {
                              return Integer.MAX_VALUE;
                           }

                           @Override
                           public void draw(TickManager tickManager) {
                              finalDrawOptions.draw();
                           }
                        });
                     }
                  }
               }
            }
         );
   }

   @Override
   public boolean inputEvent(InputEvent event) {
      State currentState = GlobalData.getCurrentState();
      if (event.isMouseMoveEvent() || event.isMouseClickEvent()) {
         this.tooltips = null;
         this.cursor = null;
         if (this.mouseDownPos == null && (currentState.getFormManager() == null || !currentState.getFormManager().isMouseOver(event))) {
            int posX = currentState.getCamera().getMouseLevelPosX(event);
            int posY = currentState.getCamera().getMouseLevelPosY(event);
            boolean toolHandled = false;
            SettlementToolHandler toolHandler = this.getCurrentToolHandler();
            if (toolHandler != null) {
               toolHandled = toolHandler.onHover(new Point(posX, posY), tooltipsx -> this.tooltips = tooltipsx, cursor -> this.cursor = cursor);
            }

            if (!toolHandled && (this.tooltips == null || this.tooltips.isEmpty()) && this.cursor == null && Settings.showControlTips) {
               ListGameTooltips tooltips = new ListGameTooltips();
               if (Input.lastInputIsController) {
                  if (!this.selectedSettlers.isEmpty()) {
                     tooltips.add(new InputTooltip(ControllerInput.MENU_PREV, Localization.translate("ui", "settlementcommandguard")));
                  } else {
                     tooltips.add(new InputTooltip(ControllerInput.MENU_NEXT, Localization.translate("ui", "settlementcommandselect")));
                  }
               } else if (!this.selectedSettlers.isEmpty()) {
                  tooltips.add(new InputTooltip(-99, Localization.translate("ui", "settlementcommandguard")));
               } else {
                  tooltips.add(new InputTooltip(-100, Localization.translate("ui", "settlementcommandselect")));
               }

               this.tooltips = tooltips;
            }
         }
      }

      if (event.getID() == -100) {
         if (this.mouseDownPos != null && !event.state) {
            Point releasePos = currentState.getCamera().getMouseLevelPos(event);
            boolean out = false;
            if (this.mouseDownIsLeftClick) {
               out = this.leftClick(this.mouseDownPos, releasePos);
            }

            if (out) {
               event.use();
            } else if (this.mouseDownPos.distance(releasePos) <= maxPixelDistanceToConsiderClick) {
               this.ignoreNextInputEvent = InputEvent.MouseButtonEvent(0, true, event.pos, GlobalData.getCurrentGameLoop());
               WindowManager.getWindow().getInput().submitNonButtonInputEvent(this.ignoreNextInputEvent);
            }

            this.mouseDownPos = null;
            return out;
         }
      } else if (event.getID() == -99 && this.mouseDownPos != null && !event.state) {
         Point releasePos = currentState.getCamera().getMouseLevelPos(event);
         boolean out = false;
         if (!this.mouseDownIsLeftClick) {
            out = this.rightClick(this.mouseDownPos, releasePos);
         }

         if (out) {
            event.use();
         } else if (this.mouseDownPos.distance(releasePos) <= maxPixelDistanceToConsiderClick) {
            this.ignoreNextInputEvent = InputEvent.MouseButtonEvent(1, true, event.pos, GlobalData.getCurrentGameLoop());
            WindowManager.getWindow().getInput().submitNonButtonInputEvent(this.ignoreNextInputEvent);
         }

         this.mouseDownPos = null;
         return out;
      }

      if (!event.isMoveUsed()
         && !event.isUsed()
         && (currentState.getFormManager() == null || !currentState.getFormManager().isMouseOver(event))
         && !event.isSameEvent(this.ignoreNextInputEvent)
         && event.state) {
         if (event.getID() == -100) {
            this.mouseDownPos = currentState.getCamera().getMouseLevelPos(event);
            this.mouseDownIsLeftClick = true;
            event.use();
            return true;
         }

         if (event.getID() == -99) {
            this.mouseDownPos = currentState.getCamera().getMouseLevelPos(event);
            this.mouseDownIsLeftClick = false;
            event.use();
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean controllerEvent(ControllerEvent event) {
      State currentState = GlobalData.getCurrentState();
      if (event.getState() == ControllerInput.AIM || event.isButton) {
         this.tooltips = null;
         this.cursor = null;
         if (this.mouseDownPos == null
            && (currentState.getFormManager() == null || !currentState.getFormManager().isMouseOver(WindowManager.getWindow().mousePos()))) {
            int posX = currentState.getCamera().getMouseLevelPosX();
            int posY = currentState.getCamera().getMouseLevelPosY();
            boolean toolHandled = false;
            SettlementToolHandler toolHandler = this.getCurrentToolHandler();
            if (toolHandler != null) {
               toolHandled = toolHandler.onHover(new Point(posX, posY), tooltipsx -> this.tooltips = tooltipsx, cursor -> this.cursor = cursor);
            }

            if (!toolHandled && (this.tooltips == null || this.tooltips.isEmpty()) && this.cursor == null && Settings.showControlTips) {
               ListGameTooltips tooltips = new ListGameTooltips();
               if (Input.lastInputIsController) {
                  if (!this.selectedSettlers.isEmpty()) {
                     tooltips.add(new InputTooltip(ControllerInput.MENU_PREV, Localization.translate("ui", "settlementcommandguard")));
                  } else {
                     tooltips.add(new InputTooltip(ControllerInput.MENU_NEXT, Localization.translate("ui", "settlementcommandselect")));
                  }
               } else if (!this.selectedSettlers.isEmpty()) {
                  tooltips.add(new InputTooltip(-99, Localization.translate("ui", "settlementcommandguard")));
               } else {
                  tooltips.add(new InputTooltip(-100, Localization.translate("ui", "settlementcommandselect")));
               }

               this.tooltips = tooltips;
            }
         }
      }

      if (event.getState() == ControllerInput.MENU_NEXT) {
         if (this.mouseDownPos != null && !event.buttonState) {
            Point releasePos = currentState.getCamera().getMouseLevelPos();
            boolean out = false;
            if (this.mouseDownIsLeftClick) {
               out = this.leftClick(this.mouseDownPos, releasePos);
            }

            this.mouseDownPos = null;
            if (out) {
               event.use();
            }

            return out;
         }
      } else if (event.getState() == ControllerInput.MENU_PREV && this.mouseDownPos != null && !event.buttonState) {
         Point releasePos = currentState.getCamera().getMouseLevelPos();
         boolean out = false;
         if (!this.mouseDownIsLeftClick) {
            out = this.rightClick(this.mouseDownPos, releasePos);
         }

         this.mouseDownPos = null;
         if (out) {
            event.use();
         }

         return out;
      }

      if (!event.isUsed() && (currentState.getFormManager() == null || !currentState.getFormManager().isMouseOver()) && event.buttonState) {
         if (event.getState() == ControllerInput.MENU_NEXT) {
            this.mouseDownPos = currentState.getCamera().getMouseLevelPos();
            this.mouseDownIsLeftClick = true;
            event.use();
            return true;
         }

         if (event.getState() == ControllerInput.MENU_PREV) {
            this.mouseDownPos = currentState.getCamera().getMouseLevelPos();
            this.mouseDownIsLeftClick = false;
            event.use();
            return true;
         }
      }

      return false;
   }

   public boolean leftClick(Point startPos, Point endPos) {
      double dist = startPos.distance(endPos);
      SettlementToolHandler toolHandler = this.getCurrentToolHandler();
      if (dist <= maxPixelDistanceToConsiderClick) {
         if (toolHandler != null && toolHandler.onLeftClick(endPos)) {
            return true;
         }

         Rectangle selectionBox = new Rectangle(endPos.x - 50, endPos.y - 50, 100, 100);
         State currentState = GlobalData.getCurrentState();
         GameCamera camera = currentState == null ? null : currentState.getCamera();
         if (camera != null) {
            int hudSelectedMob = this.streamAllHudCommandMobs(selectionBox)
               .filter(hudCommandMob -> hudCommandMob.canBeSelected(false))
               .filter(m -> m.getLevelSelectBox(camera).contains(endPos))
               .mapToInt(SelectSettlersContainerGameTool.HudCommandMob::getMobUniqueID)
               .findFirst()
               .orElse(Integer.MIN_VALUE);
            if (hudSelectedMob != Integer.MIN_VALUE) {
               this.selectedSettlers.selectOrDeselectSettler(hudSelectedMob);
               FormComponent.playStaticTickSound();
               return true;
            }
         }

         Mob mob = this.streamAllLevelSettlers(selectionBox)
            .map(CommandMob::getMob)
            .filter(m -> m.getSelectBox().contains(endPos))
            .max(Comparator.comparingInt(Mob::getDrawY))
            .orElse(null);
         if (mob != null) {
            this.selectedSettlers.selectOrDeselectSettler(mob.getUniqueID());
            FormComponent.playStaticTickSound();
         } else {
            this.selectedSettlers.clear();
         }

         return true;
      } else {
         Rectangle selectionBox = new Rectangle(
            Math.min(startPos.x, endPos.x),
            Math.min(startPos.y, endPos.y),
            Math.max(Math.abs(startPos.x - endPos.x), 1),
            Math.max(Math.abs(startPos.y - endPos.y), 1)
         );
         if (toolHandler != null && toolHandler.onLeftClickSelection(startPos, endPos, selectionBox)) {
            return true;
         }

         IntStream stream = this.streamAllLevelSettlers(selectionBox)
            .map(CommandMob::getMob)
            .filter(mob -> selectionBox.intersects(mob.getSelectBox()))
            .mapToInt(Entity::getUniqueID);
         State currentState = GlobalData.getCurrentState();
         GameCamera camera = currentState == null ? null : currentState.getCamera();
         if (camera != null) {
            stream = IntStream.concat(
                  stream,
                  this.streamAllHudCommandMobs(selectionBox)
                     .filter(hudCommandMob -> hudCommandMob.canBeSelected(true))
                     .filter(m -> selectionBox.intersects(m.getLevelSelectBox(camera)))
                     .mapToInt(SelectSettlersContainerGameTool.HudCommandMob::getMobUniqueID)
               )
               .distinct();
         }

         List<Integer> newSettlers = stream.boxed().collect(Collectors.toList());
         this.selectedSettlers.selectSettlers(newSettlers);
         if (!newSettlers.isEmpty()) {
            FormComponent.playStaticTickSound();
         }

         return true;
      }
   }

   public boolean rightClick(Point startPos, Point endPos) {
      double dist = startPos.distance(endPos);
      if (dist <= maxPixelDistanceToConsiderClick) {
         SettlementToolHandler toolHandler = this.getCurrentToolHandler();
         if (toolHandler != null && toolHandler.onRightClick(endPos)) {
            return true;
         }

         if (this.selectedSettlers.isEmpty()) {
            return false;
         }

         synchronized (this.selectedSettlers) {
            PlayerMob player = this.client.getPlayer();
            Mob targetMob = this.level
               .entityManager
               .streamAreaMobsAndPlayersTileRange(endPos.x, endPos.y, 10)
               .filter(m -> m.getSelectBox().contains(endPos))
               .filter(m -> !this.selectedSettlers.contains(m.getUniqueID()))
               .filter(m -> m.canBeTargeted(player, player.getNetworkClient()))
               .findBestDistance(1, Comparator.comparingInt(m -> -m.getDrawY()))
               .orElse(null);
            if (targetMob != null) {
               this.commandAttack(targetMob);
               Color color = new Color(255, 41, 3);
               Rectangle selectBox = targetMob.getSelectBox();
               this.spawnActionParticles(selectBox.x + selectBox.width / 2.0F, selectBox.y + selectBox.height / 2.0F, color);
               return true;
            } else {
               this.commandGuard(endPos.x, endPos.y);
               this.spawnActionParticles(endPos.x, endPos.y);
               return true;
            }
         }
      } else {
         Rectangle selectionBox = new Rectangle(
            Math.min(startPos.x, endPos.x), Math.min(startPos.y, endPos.y), Math.abs(startPos.x - endPos.x), Math.abs(startPos.y - endPos.y)
         );
         SettlementToolHandler toolHandler = this.getCurrentToolHandler();
         if (toolHandler != null && toolHandler.onRightClickSelection(startPos, endPos, selectionBox)) {
            return true;
         }

         if (this.selectedSettlers.isEmpty()) {
            return false;
         }

         if (this.selectedSettlers.getSize() == 1) {
            PlayerMob player = this.client.getPlayer();
            Mob targetMob = this.level
               .entityManager
               .streamAreaMobsAndPlayersTileRange(endPos.x, endPos.y, 10)
               .filter(m -> m.getSelectBox().contains(endPos))
               .filter(m -> !this.selectedSettlers.contains(m.getUniqueID()))
               .filter(m -> m.canBeTargeted(player, player.getNetworkClient()))
               .findBestDistance(1, Comparator.comparingInt(m -> -m.getDrawY()))
               .orElse(null);
            if (targetMob != null) {
               this.commandAttack(targetMob);
               Color color = new Color(255, 41, 3);
               Rectangle selectBox = targetMob.getSelectBox();
               this.spawnActionParticles(selectBox.x + selectBox.width / 2.0F, selectBox.y + selectBox.height / 2.0F, color);
               return true;
            }
         }

         ArrayList<Point> movePositions = this.getSettlerMovePositions(startPos, endPos);
         synchronized (this.selectedSettlers) {
            if (movePositions.size() == 1) {
               Point pos = movePositions.get(0);
               this.commandGuard(pos.x, pos.y);
            } else {
               this.commandGuard(movePositions);
            }
         }

         for (Point pos : movePositions) {
            this.spawnActionParticles(pos.x, pos.y);
         }

         return true;
      }
   }

   public ArrayList<Point> getSettlerMovePositions(Point startPos, Point endPos) {
      double dist = startPos.distance(endPos);
      synchronized (this.selectedSettlers) {
         if (this.selectedSettlers.getSize() <= 1) {
            return new ArrayList<>(Collections.singletonList(endPos));
         }

         Float dir = GameMath.normalize(endPos.x - startPos.x, endPos.y - startPos.y);
         int pointsSize = this.selectedSettlers.getSize();
         ArrayList<Point> points = new ArrayList<>(pointsSize);
         double distPerSettler = dist / Math.max(pointsSize - 1, 1);

         for (int i = 0; i < pointsSize; i++) {
            double distFromStart = distPerSettler * i;
            int x = (int)(startPos.x + dir.x * distFromStart);
            int y = (int)(startPos.y + dir.y * distFromStart);
            points.add(new Point(x, y));
         }

         return points;
      }
   }

   public void spawnActionParticles(float posX, float posY) {
      this.spawnActionParticles(posX, posY, new Color(93, 3, 255));
   }

   public DrawOptions getActionParticleDrawOptions(GameCamera camera, float x, float y, float lifePercent, Color color) {
      int drawX = camera.getDrawX(x);
      int drawY = camera.getDrawY(y);
      float rotation = (float)(this.level.getWorldEntity().getLocalTime() / 2.0 % 360.0);
      float percentSize = 1.0F - lifePercent;
      int pixelSize = (int)(Math.max(GameResources.aim.getWidth(), GameResources.aim.getHeight()) * percentSize);
      return GameResources.aim.initDraw().color(color).size(pixelSize).rotate(rotation).posMiddle(drawX, drawY);
   }

   public void spawnActionParticles(float posX, float posY, final Color color) {
      this.level
         .entityManager
         .addParticle(
            new Particle(this.level, posX, posY, 500L) {
               @Override
               public void addDrawables(
                  List<LevelSortedDrawable> list,
                  OrderableDrawables tileList,
                  OrderableDrawables topList,
                  OrderableDrawables overlayList,
                  Level level,
                  TickManager tickManager,
                  GameCamera camera,
                  PlayerMob perspective
               ) {
                  DrawOptions drawOptions = SelectSettlersContainerGameTool.this.getActionParticleDrawOptions(
                     camera, this.x, this.y, this.getLifeCyclePercent(), color
                  );
                  topList.add(tm -> drawOptions.draw());
               }
            },
            Particle.GType.CRITICAL
         );
   }

   @Override
   public void isCancelled() {
      this.hudElement.remove();
   }

   @Override
   public void isCleared() {
      this.hudElement.remove();
   }

   @Override
   public boolean canCancel() {
      return false;
   }

   @Override
   public boolean forceControllerCursor() {
      return true;
   }

   @Override
   public GameWindow.CURSOR getCursor() {
      return this.cursor;
   }

   @Override
   public GameTooltips getTooltips() {
      return this.tooltips;
   }

   public interface HudCommandMob {
      int getMobUniqueID();

      default boolean canBeSelected(boolean isByBoxSelection) {
         return true;
      }

      CommandMob getMob();

      Rectangle getLevelSelectBox(GameCamera var1);
   }
}
