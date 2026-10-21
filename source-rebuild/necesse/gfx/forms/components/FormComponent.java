package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.forms.ComponentPriorityManager;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerFocusHandler;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.ui.GameInterfaceStyle;

public abstract class FormComponent implements Comparable<FormComponent>, ControllerFocusHandler {
   public int zIndex;
   public boolean canBePutOnTopByClick = true;
   private long priorityKey;
   private boolean isDisposed;
   protected GameInterfaceStyle style;
   private FormManager manager;
   private FormComponent parentComponent;
   private final ArrayList<ControllerFocusHandler> nextFocus = new ArrayList<>();
   private final ArrayList<ControllerFocusHandler> prioritizeFocus = new ArrayList<>();
   private final ArrayList<ControllerFocusHandler> tryPrioritizeFocus = new ArrayList<>();
   private ComponentPriorityManager priorityManager;
   public ControllerFocusHandler controllerUpFocus;
   public ControllerFocusHandler controllerDownFocus;
   public ControllerFocusHandler controllerLeftFocus;
   public ControllerFocusHandler controllerRightFocus;
   public Object controllerFocusHashcode;
   public int controllerInitialFocusPriority;
   private boolean isInitialized;

   public final void setManager(FormManager manager, FormComponent parentComponent) {
      if (manager != null && this.manager != manager) {
         if (this.manager != null) {
            throw new IllegalStateException("Cannot change component manager");
         }

         this.manager = manager;
         this.parentComponent = parentComponent;
         this.init();
      }
   }

   public FormManager getManager() {
      return this.manager;
   }

   public FormComponent getParentComponent() {
      return this.parentComponent;
   }

   public Point getScreenPosition(boolean acceptScroll) {
      Point position = new Point();
      if (this instanceof FormPositionContainer) {
         FormPositionContainer myPosition = (FormPositionContainer)this;
         position.x = position.x + myPosition.getX();
         position.y = position.y + myPosition.getY();
      }

      FormComponent currentComponent = this;

      while (true) {
         FormComponent currentParent = currentComponent.getParentComponent();
         if (currentParent == null) {
            return position;
         }

         if (currentParent instanceof FormPositionContainer) {
            position = ((FormPositionContainer)currentParent).appendChildPosition(currentComponent, position, acceptScroll);
            if (position == null) {
               return null;
            }
         }

         currentComponent = currentParent;
      }
   }

   public final void setPriorityManager(ComponentPriorityManager manager) {
      if (manager != null && this.priorityManager != manager) {
         if (this.priorityManager != null) {
            throw new IllegalStateException("Cannot change component priority manager");
         }

         this.priorityManager = manager;
      }
   }

   public boolean tryPutOnTop() {
      if (this.priorityManager != null) {
         this.priorityKey = this.priorityManager.getNextPriorityKey();
         return true;
      } else {
         return false;
      }
   }

   protected void init() {
      this.isInitialized = true;
      if (!this.nextFocus.isEmpty()) {
         this.getManager().setNextControllerFocus(this.nextFocus.toArray(new ControllerFocusHandler[0]));
         this.nextFocus.clear();
      }

      if (!this.prioritizeFocus.isEmpty()) {
         this.getManager().prioritizeControllerFocus(this.prioritizeFocus.toArray(new ControllerFocusHandler[0]));
         this.prioritizeFocus.clear();
      }

      if (!this.tryPrioritizeFocus.isEmpty()) {
         this.getManager().tryPrioritizeControllerFocus(this.tryPrioritizeFocus.toArray(new ControllerFocusHandler[0]));
         this.tryPrioritizeFocus.clear();
      }
   }

   public boolean isInitialized() {
      return this.isInitialized;
   }

   public abstract void handleInputEvent(InputEvent var1, TickManager var2, PlayerMob var3);

   @Override
   public boolean handleControllerNavigate(int dir, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      switch (dir) {
         case 0:
            if (this.controllerUpFocus != null) {
               if (this.controllerUpFocus != this) {
                  this.manager.setNextControllerFocus(this.controllerUpFocus);
               }

               return true;
            }
            break;
         case 1:
            if (this.controllerRightFocus != null) {
               if (this.controllerRightFocus != this) {
                  this.manager.setNextControllerFocus(this.controllerRightFocus);
               }

               return true;
            }
            break;
         case 2:
            if (this.controllerDownFocus != null) {
               if (this.controllerDownFocus != this) {
                  this.manager.setNextControllerFocus(this.controllerDownFocus);
               }

               return true;
            }
            break;
         case 3:
            if (this.controllerLeftFocus != null) {
               if (this.controllerLeftFocus != this) {
                  this.manager.setNextControllerFocus(this.controllerLeftFocus);
               }

               return true;
            }
      }

      return false;
   }

   public abstract void addNextControllerFocus(List<ControllerFocus> var1, int var2, int var3, ControllerNavigationHandler var4, Rectangle var5, boolean var6);

   public abstract void draw(TickManager var1, PlayerMob var2, Rectangle var3);

   public abstract List<Rectangle> getHitboxes();

   protected static List<Rectangle> singleBox(Rectangle box) {
      return Collections.singletonList(box);
   }

   protected static List<Rectangle> multiBox(Rectangle... boxes) {
      return Arrays.asList(boxes);
   }

   public Rectangle getBoundingBox() {
      List<Rectangle> boxes = this.getHitboxes();
      if (boxes.isEmpty()) {
         return new Rectangle();
      }

      Rectangle out = null;

      for (Rectangle box : boxes) {
         if (!box.isEmpty()) {
            if (out == null) {
               out = box;
            } else {
               out = out.union(box);
            }
         }
      }

      return out == null ? new Rectangle() : out;
   }

   public boolean shouldUseMouseEvents() {
      return true;
   }

   public boolean shouldDraw() {
      return true;
   }

   public boolean shouldSkipRenderBoxCheck() {
      return false;
   }

   public void overrideStyle(GameInterfaceStyle style) {
      this.style = style;
   }

   public void inheritStyle(FormComponent other) {
      if (other != null) {
         this.style = other.style;
      }
   }

   public GameInterfaceStyle getInterfaceStyle() {
      return this.style != null ? this.style : Settings.UI;
   }

   public boolean isMouseOver(InputEvent event) {
      return event.isMoveUsed() ? false : this.getHitboxes().stream().filter(Objects::nonNull).anyMatch(r -> r.contains(event.pos.hudX, event.pos.hudY));
   }

   public void setNextControllerFocus(ControllerFocusHandler... handlers) {
      if (handlers != null) {
         FormManager manager = this.getManager();
         if (manager != null) {
            manager.setNextControllerFocus(handlers);
         } else {
            this.nextFocus.addAll(Arrays.asList(handlers));
         }
      }
   }

   public void prioritizeControllerFocus(ControllerFocusHandler... handlers) {
      if (handlers != null) {
         FormManager manager = this.getManager();
         if (manager != null) {
            manager.prioritizeControllerFocus(handlers);
         } else {
            this.prioritizeFocus.addAll(Arrays.asList(handlers));
         }
      }
   }

   public FormComponent prioritizeControllerFocus() {
      this.prioritizeControllerFocus(this);
      return this;
   }

   public void tryPrioritizeControllerFocus(ControllerFocusHandler... handlers) {
      if (handlers != null) {
         FormManager manager = this.getManager();
         if (manager != null) {
            manager.tryPrioritizeControllerFocus(handlers);
         } else {
            this.tryPrioritizeFocus.addAll(Arrays.asList(handlers));
         }
      }
   }

   public FormComponent tryPrioritizeControllerFocus() {
      this.tryPrioritizeControllerFocus(this);
      return this;
   }

   public boolean isControllerFocus(ControllerFocusHandler handler) {
      FormManager manager = this.getManager();
      return manager != null && manager.isControllerFocus(handler);
   }

   public ControllerFocus getControllerFocus() {
      FormManager manager = this.getManager();
      return manager != null ? manager.getCurrentFocus() : null;
   }

   public ControllerFocusHandler getControllerFocusHandler() {
      ControllerFocus controllerFocus = this.getControllerFocus();
      return controllerFocus != null ? controllerFocus.handler : null;
   }

   public boolean isControllerFocus() {
      return this.isControllerFocus(this);
   }

   @Override
   public int getControllerFocusHashcode() {
      return this.controllerFocusHashcode != null ? this.controllerFocusHashcode.hashCode() : ControllerFocusHandler.super.getControllerFocusHashcode();
   }

   public static void playStaticTickSound() {
      SoundManager.playSound(GameResources.tick, SoundEffect.ui());
   }

   public void playTickSound() {
      playStaticTickSound();
   }

   public void onWindowResized(GameWindow window) {
   }

   public void dispose() {
      if (!this.isDisposed) {
         this.isDisposed = true;
         FormManager manager = this.getManager();
         if (manager != null) {
            manager.onComponentDispose(this);
         }
      }
   }

   public final boolean isDisposed() {
      return this.isDisposed;
   }

   public int compareTo(FormComponent o) {
      int compare = Integer.compare(this.zIndex, o.zIndex);
      return compare == 0 ? Long.compare(this.priorityKey, o.priorityKey) : compare;
   }

   public static void drawWidthComponent(
      GameSprite leftSprite, GameSprite midSprite, GameSprite rightSprite, int x, int y, int width, Color drawColor, boolean isVertical
   ) {
      if (isVertical) {
         x += leftSprite.width;
      }

      if (width <= leftSprite.width + rightSprite.width) {
         int leftSection = Math.min(width / 2, leftSprite.width);
         int rightSection = width - leftSection;
         leftSprite.initDrawSection(0, leftSection, 0, leftSprite.spriteHeight)
            .size(leftSection, leftSprite.height)
            .mirror(false, isVertical)
            .color(drawColor)
            .rotate(isVertical ? 90.0F : 0.0F, 0, 0)
            .draw(x, y);
         if (rightSprite.mirrorX) {
            rightSprite.initDrawSection(0, rightSection, 0, rightSprite.spriteHeight)
               .size(rightSection, rightSprite.height)
               .mirror(false, isVertical)
               .color(drawColor)
               .rotate(isVertical ? 90.0F : 0.0F, 0, 0)
               .draw(isVertical ? x : x + leftSection, isVertical ? y + leftSection : y);
         } else {
            rightSprite.initDrawSection(rightSprite.width - rightSection, rightSprite.width, 0, rightSprite.spriteHeight)
               .size(rightSection, rightSprite.height)
               .mirror(false, isVertical)
               .color(drawColor)
               .rotate(isVertical ? 90.0F : 0.0F, 0, 0)
               .draw((isVertical ? x : x + leftSection) - (rightSprite.width - rightSection), isVertical ? y + leftSection : y);
         }
      } else {
         leftSprite.initDraw().mirror(false, isVertical).color(drawColor).rotate(isVertical ? 90.0F : 0.0F, 0, 0).draw(x, y);
         rightSprite.initDraw()
            .mirror(false, isVertical)
            .color(drawColor)
            .rotate(isVertical ? 90.0F : 0.0F, 0, 0)
            .draw(isVertical ? x : x + width - rightSprite.width, isVertical ? y + width - rightSprite.width : y);
         int i = leftSprite.width;

         while (i < width - rightSprite.width) {
            int maxWidth = Math.min(midSprite.width, width - rightSprite.width - i);
            float ratio = (float)maxWidth / midSprite.width;
            int endX = (int)(midSprite.spriteWidth * ratio);
            midSprite.initDrawSection(0, endX, 0, midSprite.spriteHeight)
               .size(maxWidth, midSprite.height)
               .mirror(false, isVertical)
               .color(drawColor)
               .rotate(isVertical ? 90.0F : 0.0F, 0, 0)
               .draw(isVertical ? x : x + i, isVertical ? y + i : y);
            i += midSprite.width;
         }
      }
   }

   public static void drawWidthComponent(GameSprite endSprite, GameSprite midSprite, int x, int y, int width, Color drawColor, boolean isVertical) {
      drawWidthComponent(endSprite, midSprite, endSprite.mirrorX(), x, y, width, drawColor, isVertical);
   }

   public static void drawWidthComponent(GameSprite endSprite, GameSprite midSprite, int x, int y, int width, Color drawColor) {
      drawWidthComponent(endSprite, midSprite, x, y, width, drawColor, false);
   }

   public static void drawWidthComponent(GameSprite endSprite, GameSprite midSprite, int x, int y, int width, boolean isVertical) {
      drawWidthComponent(endSprite, midSprite, x, y, width, Color.WHITE, isVertical);
   }

   public static void drawWidthComponent(GameSprite endSprite, GameSprite midSprite, int x, int y, int width) {
      drawWidthComponent(endSprite, midSprite, x, y, width, Color.WHITE);
   }
}
