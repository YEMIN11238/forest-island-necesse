package necesse.gfx.forms;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.GlobalData;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.util.ObjectValue;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormTypingComponent;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerFocusHandler;
import necesse.gfx.forms.floatMenu.ComponentFloatMenu;
import necesse.gfx.forms.floatMenu.FloatMenu;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.inventory.InventoryItem;

public class FormManager implements ComponentListContainer<FormComponent> {
   public static boolean drawControllerFocusBoxes;
   public static boolean drawControllerAreaBoxes;
   private boolean isDisposed;
   private final ComponentList<FormComponent> components;
   private boolean isMouseOver;
   private FormManager.CurrentFloatMenu floatMenu = null;
   private FormManager.CurrentFloatMenu visibleKeyboard = null;
   private final LinkedList<FormManager.Timeout> timeouts = new LinkedList<>();
   private ControllerFocus currentControllerFocus;
   private ArrayList<ControllerFocusHandler> nextControllerFocuses;
   private HashSet<ControllerFocusHandler> nextControllerFocusesSet;
   private static final HashMap<Integer, Long> lastControllerFocuses = new HashMap<>();

   public static void cleanUpLastControllerFocuses() {
      HashSet<Integer> removes = new HashSet<>();

      for (Entry<Integer, Long> e : lastControllerFocuses.entrySet()) {
         long timeSince = System.currentTimeMillis() - e.getValue();
         if (timeSince > 60000L) {
            removes.add(e.getKey());
         }
      }

      removes.forEach(lastControllerFocuses::remove);
   }

   public FormManager() {
      this.components = new ComponentList<FormComponent>(null) {
         @Override
         public InputEvent offsetEvent(InputEvent event, boolean allowOutside) {
            return event;
         }

         @Override
         public FormManager getManager() {
            return FormManager.this;
         }

         @Override
         public void onChange() {
            FormManager.this.updateMouseOver();
         }
      };
   }

   @Override
   public ComponentList<FormComponent> getComponentList() {
      return this.components;
   }

   public void openFloatMenu(FloatMenu menu) {
      this.openFloatMenu(menu, 0, 0);
   }

   public void openFloatMenu(FloatMenu menu, FormComponent relativeTo, InputEvent event) {
      this.openFloatMenu(menu, relativeTo, event, 0, 0);
   }

   public void openFloatMenu(FloatMenu menu, FormComponent relativeTo, InputEvent event, int xOffset, int yOffset) {
      if (event.isControllerEvent()) {
         ControllerFocus currentFocus = this.getCurrentFocus();
         if (currentFocus != null) {
            Point point = currentFocus.getTooltipAndFloatMenuPoint();
            this.openFloatMenuAt(menu, point.x + xOffset, point.y + yOffset);
            return;
         }
      }

      Rectangle boundingBox = relativeTo.getBoundingBox();
      this.openFloatMenu(menu, boundingBox.x - event.pos.hudX + xOffset, boundingBox.y - event.pos.hudY + yOffset);
   }

   public void openFloatMenu(FloatMenu menu, int xOffset, int yOffset) {
      ControllerFocus currentFocus = this.getCurrentFocus();
      if (currentFocus != null) {
         Point point = currentFocus.getTooltipAndFloatMenuPoint();
         this.openFloatMenuAt(menu, point.x + xOffset, point.y + yOffset);
      } else {
         InputPosition mousePos = WindowManager.getWindow().mousePos();
         this.openFloatMenuAt(menu, mousePos.hudX + xOffset, mousePos.hudY + yOffset);
      }
   }

   public void openFloatMenuAt(FloatMenu menu, int drawX, int drawY) {
      Objects.requireNonNull(menu);
      if (this.floatMenu != null) {
         this.floatMenu.menu.dispose();
      }

      this.floatMenu = new FormManager.CurrentFloatMenu(menu, drawX, drawY, GlobalData.getCurrentGameLoop());
      menu.init(this.floatMenu.drawX, this.floatMenu.drawY, () -> {
         if (this.floatMenu != null && this.floatMenu.menu == menu) {
            this.floatMenu.menu.dispose();
            if (this.floatMenu.menu.disposeFocus != null) {
               this.setNextControllerFocus(this.floatMenu.menu.disposeFocus);
            }

            this.floatMenu = null;
            ControllerInput.submitNextRefreshFocusEvent();
         }
      });
      this.updateMouseOver();
      ControllerInput.submitNextRefreshFocusEvent();
   }

   public boolean hasFloatMenu() {
      return this.floatMenu != null;
   }

   public void openControllerKeyboard(final FormTypingComponent typingComponent) {
      if (this.visibleKeyboard != null) {
         this.visibleKeyboard.menu.dispose();
      }

      final AtomicReference<FloatMenu> menuRef = new AtomicReference<>();
      ComponentFloatMenu menu = new ComponentFloatMenu(typingComponent, new ControllerKeyboardForm(typingComponent) {
         @Override
         public void submitEnter(InputEvent event) {
            typingComponent.submitControllerEnter(event);
            menuRef.get().remove();
         }
      });
      menuRef.set(menu);
      this.visibleKeyboard = new FormManager.CurrentFloatMenu(menu, 0, 0, GlobalData.getCurrentGameLoop());
      menu.init(this.visibleKeyboard.drawX, this.visibleKeyboard.drawY, () -> {
         if (this.visibleKeyboard != null && this.visibleKeyboard.menu == menu) {
            this.visibleKeyboard.menu.dispose();
            if (this.visibleKeyboard.menu.disposeFocus != null) {
               this.setNextControllerFocus(this.visibleKeyboard.menu.disposeFocus);
            }

            this.visibleKeyboard = null;
            ControllerInput.submitNextRefreshFocusEvent();
         }

         typingComponent.setTyping(false);
      });
      this.updateMouseOver();
      ControllerInput.submitNextRefreshFocusEvent();
   }

   public boolean isControllerKeyboardOpen() {
      return this.visibleKeyboard != null;
   }

   public void updateMouseOver(InputEvent event) {
      this.isMouseOver = this.visibleKeyboard != null && this.visibleKeyboard.menu.isMouseOver(event);
      this.isMouseOver = this.floatMenu != null && this.floatMenu.menu.isMouseOver(event);

      for (FormComponent component : this.components) {
         if (component.shouldDraw() && component.isMouseOver(event)) {
            this.isMouseOver = true;
            break;
         }
      }
   }

   public final void updateMouseOver() {
      this.updateMouseOver(InputEvent.MouseMoveEvent(WindowManager.getWindow().mousePos(), GlobalData.getCurrentGameLoop()));
   }

   public void frameTick(TickManager tickManager) {
      long currentTime = System.currentTimeMillis();

      while (!this.timeouts.isEmpty()) {
         FormManager.Timeout first = this.timeouts.getFirst();
         if (first.time >= currentTime) {
            break;
         }

         this.timeouts.removeFirst();
         first.runnable.run();
      }

      if (this.currentControllerFocus != null) {
         this.currentControllerFocus.handler.frameTickControllerFocus(tickManager, this.currentControllerFocus);
      }
   }

   public void submitInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.updateMouseOver(event);
      }

      if (this.visibleKeyboard != null && !this.visibleKeyboard.isSameStartTime(event)) {
         this.visibleKeyboard.menu.handleInputEvent(event, tickManager, perspective);
         if (event.isMouseMoveEvent() && this.visibleKeyboard.menu.isMouseOver(event)) {
            event.useMove();
         }

         if (event.isUsed()) {
            return;
         }

         if (event.state && event.getID() == 256) {
            this.visibleKeyboard.menu.remove();
            event.use();
         }
      }

      if (this.floatMenu != null && !this.floatMenu.isSameStartTime(event)) {
         this.floatMenu.menu.handleInputEvent(event, tickManager, perspective);
         if (event.isMouseMoveEvent() && this.floatMenu.menu.isMouseOver(event)) {
            event.useMove();
         }

         if (event.isUsed()) {
            return;
         }

         if (event.state && event.getID() == 256) {
            this.floatMenu.menu.remove();
            event.use();
         }
      }

      this.components.submitInputEvent(event, tickManager, perspective);
      if ((event.isMouseWheelEvent() || event.isMouseClickEvent()) && this.isMouseOver(event)) {
         event.use();
      }

      if (event.wasMouseClickEvent() && FormTypingComponent.isCurrentlyTyping()) {
         FormTypingComponent.getCurrentTypingComponent().submitUsedInputEvent(event);
      }
   }

   public void submitControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      GameWindow window = WindowManager.getWindow();
      if (event.getState() != ControllerInput.REFRESH_FOCUS || ControllerInput.isCursorVisible() && this.currentControllerFocus == null) {
         if ((event.getState() == ControllerInput.AIM || event.getState() == ControllerInput.CURSOR) && ControllerInput.isCursorVisible()) {
            InputPosition mousePos = window.mousePos();
            this.submitInputEvent(InputEvent.MouseMoveEvent(mousePos, tickManager), tickManager, perspective);
            this.setControllerFocus(null);
         } else if (!event.isUsed()) {
            if ((event.getState() == ControllerInput.ATTACK || event.getState() == ControllerInput.MENU_SELECT) && ControllerInput.isCursorVisible()) {
               InputPosition mousePos = window.mousePos();
               InputEvent mouseEvent = InputEvent.MouseButtonEvent(0, event.buttonState, mousePos, tickManager);
               if (!event.buttonState) {
                  window.getInput().stopRepeatEvent(mouseEvent);
               }

               this.submitInputEvent(mouseEvent, tickManager, perspective);
               if (mouseEvent.isUsed()) {
                  event.use();
               }
            }

            if (!event.isUsed()) {
               if (event.getState() == ControllerInput.INTERACT && ControllerInput.isCursorVisible()) {
                  InputPosition mousePos = window.mousePos();
                  InputEvent mouseEvent = InputEvent.MouseButtonEvent(1, event.buttonState, mousePos, tickManager);
                  if (!event.buttonState) {
                     window.getInput().stopRepeatEvent(mouseEvent);
                  }

                  this.submitInputEvent(mouseEvent, tickManager, perspective);
                  if (mouseEvent.isUsed()) {
                     event.use();
                  }
               }

               if (!event.isUsed()) {
                  if (this.visibleKeyboard != null) {
                     this.visibleKeyboard.menu.handleControllerEvent(event, tickManager, perspective);
                     if ((event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU) && event.buttonState) {
                        this.visibleKeyboard.menu.remove();
                        event.use();
                     }
                  } else if (this.floatMenu != null) {
                     this.floatMenu.menu.handleControllerEvent(event, tickManager, perspective);
                     if ((event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU) && event.buttonState) {
                        this.floatMenu.menu.remove();
                        event.use();
                     }
                  } else {
                     this.components.submitControllerEvent(event, tickManager, perspective);
                  }

                  if (!event.isUsed()) {
                     int dir;
                     if ((event.getState() != ControllerInput.MENU_UP || !event.buttonState) && !event.isRepeatEvent(ControllerInput.MENU_UP)) {
                        if ((event.getState() != ControllerInput.MENU_RIGHT || !event.buttonState) && !event.isRepeatEvent(ControllerInput.MENU_RIGHT)) {
                           if ((event.getState() != ControllerInput.MENU_DOWN || !event.buttonState) && !event.isRepeatEvent(ControllerInput.MENU_DOWN)) {
                              if ((event.getState() != ControllerInput.MENU_LEFT || !event.buttonState) && !event.isRepeatEvent(ControllerInput.MENU_LEFT)) {
                                 dir = -1;
                              } else {
                                 event.startRepeatEvents(ControllerInput.MENU_LEFT);
                                 dir = 3;
                              }
                           } else {
                              event.startRepeatEvents(ControllerInput.MENU_DOWN);
                              dir = 2;
                           }
                        } else {
                           event.startRepeatEvents(ControllerInput.MENU_RIGHT);
                           dir = 1;
                        }
                     } else {
                        event.startRepeatEvents(ControllerInput.MENU_UP);
                        dir = 0;
                     }

                     if (dir != -1) {
                        event.use();
                        LinkedList<ControllerFocus> list = null;
                        if (this.currentControllerFocus != null) {
                           list = new LinkedList<>();
                           if (this.visibleKeyboard != null) {
                              this.visibleKeyboard
                                 .menu
                                 .addNextControllerFocus(
                                    list,
                                    this.visibleKeyboard.drawX,
                                    this.visibleKeyboard.drawY,
                                    null,
                                    new Rectangle(window.getHudWidth(), window.getHudHeight()),
                                    false
                                 );
                           } else if (this.floatMenu != null) {
                              this.floatMenu
                                 .menu
                                 .addNextControllerFocus(
                                    list, this.floatMenu.drawX, this.floatMenu.drawY, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false
                                 );
                           } else {
                              this.components.addNextControllerComponents(list, 0, 0, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false);
                           }

                           if (list.stream().anyMatch(c -> c.handler == this.currentControllerFocus.handler)) {
                              if (this.currentControllerFocus.handler.handleControllerNavigate(dir, event, tickManager, perspective)) {
                                 return;
                              }

                              if (this.currentControllerFocus.customNavigationHandler != null
                                 && this.currentControllerFocus.customNavigationHandler.handleNavigate(dir, event, tickManager, perspective)) {
                                 return;
                              }
                           }
                        }

                        if (list == null) {
                           list = new LinkedList<>();
                           if (this.visibleKeyboard != null) {
                              this.visibleKeyboard
                                 .menu
                                 .addNextControllerFocus(
                                    list,
                                    this.visibleKeyboard.drawX,
                                    this.visibleKeyboard.drawY,
                                    null,
                                    new Rectangle(window.getHudWidth(), window.getHudHeight()),
                                    false
                                 );
                           } else if (this.floatMenu != null) {
                              this.floatMenu
                                 .menu
                                 .addNextControllerFocus(
                                    list, this.floatMenu.drawX, this.floatMenu.drawY, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false
                                 );
                           } else {
                              this.components.addNextControllerComponents(list, 0, 0, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false);
                           }
                        }

                        ControllerFocus next;
                        if (this.currentControllerFocus == null) {
                           InputPosition mousePos = window.mousePos();
                           next = list.stream()
                              .min(Comparator.comparingDouble(c -> c.getBoundingBoxCenter().distance(mousePos.hudX, mousePos.hudY)))
                              .orElse(null);
                        } else {
                           next = ControllerFocus.getNext(dir, this, list);
                        }

                        if (next != null) {
                           this.setControllerFocus(next);
                        }
                     }
                  }
               }
            }
         }
      } else {
         if (ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER) && Input.lastInputIsController) {
            LinkedList<ControllerFocus> list = new LinkedList<>();
            if (this.visibleKeyboard != null) {
               this.visibleKeyboard
                  .menu
                  .addNextControllerFocus(
                     list, this.visibleKeyboard.drawX, this.visibleKeyboard.drawY, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false
                  );
            } else if (this.floatMenu != null) {
               this.floatMenu
                  .menu
                  .addNextControllerFocus(
                     list, this.floatMenu.drawX, this.floatMenu.drawY, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false
                  );
            } else {
               this.components.addNextControllerComponents(list, 0, 0, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), false);
            }

            this.refreshFocus(list);
         }
      }
   }

   public void refreshFocus(List<ControllerFocus> list) {
      boolean reset = true;
      if (this.nextControllerFocuses != null) {
         ControllerFocus best = null;

         for (ControllerFocusHandler nextHandler : this.nextControllerFocuses) {
            ControllerFocus next = list.stream().filter(c -> c.handler == nextHandler).findFirst().orElse(null);
            long nextLastTime = lastControllerFocuses.getOrDefault(nextHandler.getControllerFocusHashcode(), -1L);
            if (best == null) {
               best = next;
            } else {
               long bestLastTime = lastControllerFocuses.getOrDefault(best.handler.getControllerFocusHashcode(), -1L);
               if (bestLastTime < nextLastTime) {
                  best = next;
               }
            }
         }

         this.nextControllerFocuses = null;
         this.nextControllerFocusesSet = null;
         if (best != null) {
            this.setControllerFocus(best);
            return;
         }
      }

      if (this.currentControllerFocus != null) {
         ControllerFocus next = list.stream().filter(c -> c.handler == this.currentControllerFocus.handler).findFirst().orElse(null);
         if (next != null) {
            this.currentControllerFocus = next;
            reset = false;
         }
      }

      if (reset) {
         ControllerFocus next = list.stream().map(c -> {
            long time = lastControllerFocuses.getOrDefault(c.handler.getControllerFocusHashcode(), -1L);
            return new ObjectValue<>(c, time);
         }).filter(c -> c.value != -1L).max(Comparator.comparingLong(c -> c.value)).map(c -> (ControllerFocus)c.object).orElse(null);
         if (this.currentControllerFocus != null && next == null) {
            lastControllerFocuses.put(this.currentControllerFocus.handler.getControllerFocusHashcode(), System.currentTimeMillis());
            Comparator<ControllerFocus> comparator = Comparator.comparingDouble(c -> {
               Rectangle intersection = c.boundingBox.intersection(this.currentControllerFocus.boundingBox);
               if (intersection.isEmpty()) {
                  return 0.0;
               }

               double cArea = (double)c.boundingBox.width * c.boundingBox.height;
               double iArea = (double)intersection.width * intersection.height;
               return iArea / cArea;
            });
            comparator = comparator.thenComparingDouble(
               c -> -new Double(c.boundingBox.getCenterX(), c.boundingBox.getCenterY())
                  .distance(this.currentControllerFocus.boundingBox.getCenterX(), this.currentControllerFocus.boundingBox.getCenterY())
            );
            next = list.stream().max(comparator).orElse(null);
         }

         if (next == null) {
            next = list.stream().max(Comparator.comparingInt(f -> f.initialFocusPriority)).orElse(null);
         }

         this.setControllerFocus(next);
      }
   }

   public void prioritizeControllerFocus(ControllerFocusHandler... handlers) {
      if (handlers != null) {
         long currentTime = System.currentTimeMillis();

         for (ControllerFocusHandler handler : handlers) {
            if (handler != null) {
               lastControllerFocuses.put(handler.getControllerFocusHashcode(), currentTime--);
            }
         }
      }
   }

   public void tryPrioritizeControllerFocus(ControllerFocusHandler... handlers) {
      if (handlers != null) {
         long currentTime = System.currentTimeMillis();

         for (ControllerFocusHandler handler : handlers) {
            if (handler != null && !lastControllerFocuses.containsKey(handler.getControllerFocusHashcode())) {
               lastControllerFocuses.put(handler.getControllerFocusHashcode(), currentTime--);
            }
         }
      }
   }

   public void setControllerFocus(ControllerFocus focus) {
      if (this.currentControllerFocus != null) {
         this.currentControllerFocus.handler.onControllerUnfocused(this.currentControllerFocus);
      }

      this.currentControllerFocus = focus;
      if (focus != null && !focus.boundingBox.contains(Input.mousePos.hudX, Input.mousePos.hudY)) {
         Point center = focus.getBoundingBoxCenter();
         InputPosition position = InputPosition.fromHudPos(WindowManager.getWindow(), center.x, center.y);
         WindowManager.getWindow().getInput().setCursorPosition(position.windowX, position.windowY, null);
      }

      if (this.currentControllerFocus != null) {
         lastControllerFocuses.put(this.currentControllerFocus.handler.getControllerFocusHashcode(), System.currentTimeMillis());
         this.currentControllerFocus.handler.onControllerFocused(this.currentControllerFocus);
      }
   }

   public void setNextControllerFocus(ControllerFocusHandler... handlers) {
      boolean update = false;
      if (this.nextControllerFocuses == null) {
         this.nextControllerFocuses = new ArrayList<>(handlers.length);
         this.nextControllerFocusesSet = new HashSet<>();
      } else {
         this.nextControllerFocuses.ensureCapacity(this.nextControllerFocuses.size() + handlers.length);
      }

      for (ControllerFocusHandler handler : handlers) {
         if (this.nextControllerFocusesSet.add(handler)) {
            this.nextControllerFocuses.add(handler);
            update = true;
         }
      }

      if (update) {
         ControllerInput.submitNextRefreshFocusEvent();
      }
   }

   public boolean isControllerFocus(ControllerFocusHandler handler) {
      if (!Input.lastInputIsController || !ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER)) {
         return false;
      } else {
         return handler == null ? this.currentControllerFocus == null : this.currentControllerFocus != null && this.currentControllerFocus.handler == handler;
      }
   }

   public ControllerFocus getCurrentFocus() {
      return Input.lastInputIsController && ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER) ? this.currentControllerFocus : null;
   }

   public boolean isControllerTyping() {
      return this.visibleKeyboard != null;
   }

   public boolean isControllerTyping(ControllerFocusHandler handler) {
      return this.visibleKeyboard != null ? this.visibleKeyboard.menu.disposeFocus == handler : false;
   }

   public boolean isMouseOver() {
      return this.isMouseOver;
   }

   public boolean isMouseOver(InputEvent event) {
      if (this.visibleKeyboard != null && this.visibleKeyboard.menu.isMouseOver(event)) {
         return true;
      }

      if (this.floatMenu != null && this.floatMenu.menu.isMouseOver(event)) {
         return true;
      }

      for (FormComponent form : this.components) {
         if (form.shouldDraw() && form.isMouseOver(event)) {
            return true;
         }
      }

      return false;
   }

   public boolean isMouseOver(InputPosition pos) {
      return this.isMouseOver(InputEvent.MouseMoveEvent(pos, null));
   }

   public void draw(TickManager tickManager, PlayerMob perspective) {
      GameResources.formShader.use();
      GameWindow window = WindowManager.getWindow();

      try {
         this.components.drawComponents(tickManager, perspective, new Rectangle(window.getHudWidth(), window.getHudHeight()));
         if (this.floatMenu != null) {
            this.floatMenu.menu.draw(tickManager, perspective);
         }

         if (this.visibleKeyboard != null) {
            Renderer.initQuadDraw(window.getHudWidth(), window.getHudHeight()).color(new Color(0, 0, 0, 70)).draw(0, 0);
            this.visibleKeyboard.menu.draw(tickManager, perspective);
         }
      } finally {
         GameResources.formShader.stop();
      }

      if (drawControllerFocusBoxes || drawControllerAreaBoxes) {
         LinkedList<ControllerFocus> list = new LinkedList<>();
         if (this.floatMenu != null) {
            this.floatMenu
               .menu
               .addNextControllerFocus(
                  list, this.floatMenu.drawX, this.floatMenu.drawY, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), drawControllerAreaBoxes
               );
         } else {
            this.components.addNextControllerComponents(list, 0, 0, null, new Rectangle(window.getHudWidth(), window.getHudHeight()), drawControllerAreaBoxes);
         }

         if (drawControllerFocusBoxes) {
            for (ControllerFocus c : list) {
               if (this.currentControllerFocus == null || c.handler != this.currentControllerFocus.handler) {
                  Rectangle box = c.boundingBox;
                  Renderer.drawShape(box, false, 1.0F, 1.0F, 0.0F, 1.0F);
               }
            }
         }
      }

      Runnable drawDraggingItem = null;
      if (GlobalData.getCurrentState().isRunning() && perspective != null && perspective.isInventoryExtended()) {
         InventoryItem draggingItem = perspective.getDraggingItem();
         if (draggingItem != null) {
            ControllerFocus currentFocus = this.getCurrentFocus();
            if (currentFocus != null && !ControllerInput.isCursorVisible()) {
               Point boundingBoxCenter = currentFocus.getBoundingBoxCenter();
               drawDraggingItem = () -> draggingItem.draw(perspective, boundingBoxCenter.x - 16, boundingBoxCenter.y - 28, false, true);
            } else {
               InputPosition mousePos = window.mousePos();
               drawDraggingItem = () -> draggingItem.draw(perspective, mousePos.hudX - 16, mousePos.hudY - 16, false, true);
            }
         }
      }

      if (Input.lastInputIsController && ControllerInput.isLayerActive(ControllerInput.MENU_SET_LAYER) && this.currentControllerFocus != null) {
         Point controllerTooltipPoint = this.currentControllerFocus.getTooltipAndFloatMenuPoint();
         if (controllerTooltipPoint == null) {
            controllerTooltipPoint = this.currentControllerFocus.boundingBox.getLocation();
         }

         GameTooltipManager.setTooltipsFormFocus(controllerTooltipPoint.x, controllerTooltipPoint.y - (drawDraggingItem != null ? 4 : 0));
         if (drawDraggingItem != null) {
            GameTooltipManager.setTooltipFocusOffset(0, 4);
         }

         this.currentControllerFocus.handler.drawControllerFocus(this.currentControllerFocus);
      } else if (drawDraggingItem != null) {
         GameTooltipManager.setTooltipFocusOffset(0, 12);
      }

      if (drawDraggingItem != null) {
         drawDraggingItem.run();
      }
   }

   public FormManager.Timeout setTimeout(Runnable runnable, long time) {
      FormManager.Timeout timeout = new FormManager.Timeout(runnable, time);
      this.timeouts.add(timeout);
      this.timeouts.sort(Comparator.comparingLong(t -> t.time));
      return timeout;
   }

   public boolean clearTimeout(FormManager.Timeout timeout) {
      return this.timeouts.remove(timeout);
   }

   public void onWindowResized(GameWindow window) {
      this.components.onWindowResized(window);
   }

   public void dispose() {
      this.isDisposed = true;
      if (this.floatMenu != null) {
         this.floatMenu.menu.dispose();
      }

      if (this.visibleKeyboard != null) {
         this.visibleKeyboard.menu.dispose();
      }

      this.components.disposeComponents();
   }

   public boolean isDisposed() {
      return this.isDisposed;
   }

   public void onComponentDispose(FormComponent component) {
      if (this.floatMenu != null && this.floatMenu.menu.parent == component) {
         this.floatMenu.menu.remove();
      }

      if (this.visibleKeyboard != null && this.visibleKeyboard.menu.parent == component) {
         this.visibleKeyboard.menu.remove();
      }
   }

   private static class CurrentFloatMenu {
      public final long tick;
      public final long frame;
      public final FloatMenu menu;
      public final int drawX;
      public final int drawY;

      public CurrentFloatMenu(FloatMenu menu, int drawX, int drawY, TickManager tickManager) {
         this.menu = menu;
         this.drawX = drawX;
         this.drawY = drawY;
         this.tick = tickManager.getTotalTicks();
         this.frame = tickManager.getTotalFrames();
      }

      public boolean isSameStartTime(InputEvent event) {
         return this.frame == event.frame;
      }
   }

   public static class Timeout {
      public final Runnable runnable;
      public final long time;

      public Timeout(Runnable runnable, long time) {
         this.runnable = runnable;
         this.time = System.currentTimeMillis() + time;
      }
   }
}
