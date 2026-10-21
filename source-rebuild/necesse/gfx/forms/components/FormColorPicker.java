package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Function;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameMath;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerFocusHandler;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.events.FormEvent;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.ui.HUD;
import org.lwjgl.opengl.GL11;

public class FormColorPicker extends FormComponent implements FormPositionContainer {
   private static final int HUE_BAR_HEIGHT = 30;
   private static final int HUE_BAR_PADDING = 10;
   public Color backgroundColor = GameBackground.form.getCenterColor();
   private FormPosition position;
   private final int width;
   private final int height;
   private boolean mouseDownHueBar;
   private boolean mouseDownSpectrum;
   private Point mouseDownPoint;
   private InputEvent mouseDownEvent;
   private boolean isControllerSelected;
   private boolean isControllerControllingHue;
   private float currentHue = 0.0F;
   private Color currentSelected = null;
   private Point currentPoint = null;
   protected FormEventsHandler<FormEvent<FormColorPicker>> changedEvents = new FormEventsHandler<>();
   protected ControllerFocusHandler colorSelectorHandler = new ControllerFocusHandler() {
      @Override
      public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (event.getState() == ControllerInput.MENU_SELECT) {
            if (FormColorPicker.this.isControllerFocus(this) && event.buttonState) {
               FormColorPicker.this.isControllerSelected = true;
               FormColorPicker.this.isControllerControllingHue = false;
               if (FormColorPicker.this.currentPoint == null) {
                  FormColorPicker.this.currentPoint = new Point(0, 0);
               }

               event.use();
            }
         } else if ((event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU)
            && FormColorPicker.this.isControllerSelected
            && event.buttonState) {
            FormColorPicker.this.isControllerSelected = false;
            event.use();
         }
      }

      @Override
      public boolean handleControllerNavigate(int dir, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (FormColorPicker.this.isControllerSelected && !FormColorPicker.this.isControllerControllingHue) {
            if (FormColorPicker.this.currentPoint == null) {
               FormColorPicker.this.currentPoint = new Point(0, 0);
            }

            switch (dir) {
               case 0:
                  if (FormColorPicker.this.currentPoint.y > 0) {
                     FormColorPicker.this.currentPoint.y = Math.max(FormColorPicker.this.currentPoint.y - 3, 0);
                     FormColorPicker.this.queryNewColor();
                  }

                  event.use();
                  break;
               case 1:
                  if (FormColorPicker.this.currentPoint.x < FormColorPicker.this.width) {
                     FormColorPicker.this.currentPoint.x = Math.min(FormColorPicker.this.currentPoint.x + 3, FormColorPicker.this.width - 1);
                     FormColorPicker.this.queryNewColor();
                  }

                  event.use();
                  break;
               case 2:
                  int spectrumHeight = FormColorPicker.this.getSpectrumHeight();
                  if (FormColorPicker.this.currentPoint.y < spectrumHeight) {
                     FormColorPicker.this.currentPoint.y = Math.min(FormColorPicker.this.currentPoint.y + 3, spectrumHeight - 1);
                     FormColorPicker.this.queryNewColor();
                  }

                  event.use();
                  break;
               case 3:
                  if (FormColorPicker.this.currentPoint.x > 0) {
                     FormColorPicker.this.currentPoint.x = Math.max(FormColorPicker.this.currentPoint.x - 3, 0);
                     FormColorPicker.this.queryNewColor();
                  }

                  event.use();
            }

            return true;
         } else {
            return false;
         }
      }

      @Override
      public void drawControllerFocus(ControllerFocus current) {
         Rectangle box = current.boundingBox;
         int padding = 5;
         box = new Rectangle(box.x - padding, box.y - padding, box.width + padding * 2, box.height + padding * 2);
         Color color = FormColorPicker.this.isControllerSelected && !FormColorPicker.this.isControllerControllingHue
            ? FormColorPicker.this.getInterfaceStyle().controllerFocusBoundsHighlightColor
            : FormColorPicker.this.getInterfaceStyle().controllerFocusBoundsColor;
         HUD.selectBoundOptions(color, true, box).draw();
         if (!FormColorPicker.this.isControllerSelected || FormColorPicker.this.isControllerControllingHue) {
            GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
         }
      }
   };
   protected ControllerFocusHandler hueSelectorHandler = new ControllerFocusHandler() {
      @Override
      public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (event.getState() == ControllerInput.MENU_SELECT) {
            if (FormColorPicker.this.isControllerFocus(this) && event.buttonState) {
               FormColorPicker.this.isControllerSelected = true;
               FormColorPicker.this.isControllerControllingHue = true;
               event.use();
            }
         } else if ((event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU)
            && FormColorPicker.this.isControllerSelected
            && event.buttonState) {
            FormColorPicker.this.isControllerSelected = false;
            event.use();
         }
      }

      @Override
      public boolean handleControllerNavigate(int dir, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (FormColorPicker.this.isControllerSelected && FormColorPicker.this.isControllerControllingHue) {
            switch (dir) {
               case 1:
                  if (FormColorPicker.this.currentHue < 1.0F) {
                     FormColorPicker.this.currentHue = Math.min(FormColorPicker.this.currentHue + 0.02F, 1.0F);
                     FormColorPicker.this.queryNewColor();
                  }

                  event.use();
                  break;
               case 3:
                  if (FormColorPicker.this.currentHue > 0.0F) {
                     FormColorPicker.this.currentHue = Math.max(FormColorPicker.this.currentHue - 0.02F, 0.0F);
                     FormColorPicker.this.queryNewColor();
                  }

                  event.use();
            }

            return true;
         } else {
            return false;
         }
      }

      @Override
      public void drawControllerFocus(ControllerFocus current) {
         Rectangle box = current.boundingBox;
         int padding = 5;
         box = new Rectangle(box.x - padding, box.y - padding, box.width + padding * 2, box.height + padding * 2);
         Color color = FormColorPicker.this.isControllerSelected && FormColorPicker.this.isControllerControllingHue
            ? FormColorPicker.this.getInterfaceStyle().controllerFocusBoundsHighlightColor
            : FormColorPicker.this.getInterfaceStyle().controllerFocusBoundsColor;
         HUD.selectBoundOptions(color, true, box).draw();
         if (!FormColorPicker.this.isControllerSelected || !FormColorPicker.this.isControllerControllingHue) {
            GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
         }
      }
   };

   public FormColorPicker(int x, int y, int width, int height) {
      if (height < 30) {
         throw new IllegalArgumentException("Height must be greater than 30");
      }

      this.position = new FormFixedPosition(x, y);
      this.width = width;
      this.height = height;
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         event = InputEvent.MouseMoveEvent(WindowManager.getWindow().mousePos(), tickManager);
         if (this.mouseDownHueBar) {
            int x = GameMath.limit(this.mouseDownPoint.x + event.pos.hudX - this.mouseDownEvent.pos.hudX, 0, this.width);
            this.currentHue = getHueAt(x, this.width);
            this.queryNewColor();
         } else if (this.mouseDownSpectrum) {
            int x = GameMath.limit(this.mouseDownPoint.x + event.pos.hudX - this.mouseDownEvent.pos.hudX, 0, this.width);
            int y = GameMath.limit(this.mouseDownPoint.y + event.pos.hudY - this.mouseDownEvent.pos.hudY, 0, this.getSpectrumHeight());
            this.currentPoint = new Point(x, y);
            this.queryNewColor();
         }
      }

      if (event.getID() == -100) {
         if (!event.state) {
            if (this.mouseDownHueBar) {
               event.use();
               this.mouseDownHueBar = false;
               if (event.pos.hudX != Integer.MIN_VALUE && event.pos.hudY != Integer.MIN_VALUE) {
                  int x = GameMath.limit(event.pos.hudX - this.getX(), 0, this.width);
                  this.currentHue = getHueAt(x, this.width);
                  this.queryNewColor();
               }
            } else if (this.mouseDownSpectrum) {
               event.use();
               this.mouseDownSpectrum = false;
               if (event.pos.hudX != Integer.MIN_VALUE && event.pos.hudY != Integer.MIN_VALUE) {
                  int x = GameMath.limit(event.pos.hudX - this.getX(), 0, this.width);
                  int y = GameMath.limit(event.pos.hudY - this.getY(), 0, this.getSpectrumHeight());
                  this.currentPoint = new Point(x, y);
                  this.queryNewColor();
               }
            }
         } else if (this.isMouseOverHueBar(event)) {
            event.use();
            this.mouseDownHueBar = true;
            this.mouseDownEvent = InputEvent.MouseMoveEvent(WindowManager.getWindow().mousePos(), tickManager);
            int x = GameMath.limit(event.pos.hudX - this.getX(), 0, this.width);
            this.mouseDownPoint = new Point(x, 0);
            this.currentHue = getHueAt(x, this.width);
            this.queryNewColor();
         } else if (this.isMouseOverSpectrum(event)) {
            event.use();
            this.mouseDownSpectrum = true;
            this.mouseDownEvent = InputEvent.MouseMoveEvent(WindowManager.getWindow().mousePos(), tickManager);
            int x = GameMath.limit(event.pos.hudX - this.getX(), 0, this.width);
            int y = GameMath.limit(event.pos.hudY - this.getY(), 0, this.getSpectrumHeight());
            this.mouseDownPoint = new Point(x, y);
            this.currentPoint = new Point(x, y);
            this.queryNewColor();
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      this.colorSelectorHandler.handleControllerEvent(event, tickManager, perspective);
      this.hueSelectorHandler.handleControllerEvent(event, tickManager, perspective);
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      ControllerFocus.add(
         list,
         area,
         this.colorSelectorHandler,
         new Rectangle(this.getX(), this.getY(), this.width, this.getSpectrumHeight()),
         currentXOffset,
         currentYOffset,
         this.controllerInitialFocusPriority,
         customNavigationHandler
      );
      ControllerFocus.add(
         list,
         area,
         this.hueSelectorHandler,
         new Rectangle(this.getX(), this.getY() + this.getSpectrumHeight(), this.width, 30),
         currentXOffset,
         currentYOffset,
         this.controllerInitialFocusPriority,
         customNavigationHandler
      );
   }

   protected void queryNewColor() {
      if (this.currentPoint != null) {
         Color oldColor = this.currentSelected;
         this.currentSelected = getColorAt(this.currentPoint.x, this.currentPoint.y, this.width, this.getSpectrumHeight(), this.currentHue);
         if (!this.currentSelected.equals(oldColor)) {
            FormEvent<FormColorPicker> event = new FormEvent<>(this);
            this.changedEvents.onEvent(event);
         }
      }
   }

   public FormColorPicker onChanged(FormEventListener<FormEvent<FormColorPicker>> listener) {
      this.changedEvents.addListener(listener);
      return this;
   }

   public Color getSelectedColor() {
      return this.currentSelected;
   }

   public void setSelectedColor(Color color) {
      if (color == null) {
         this.currentHue = 0.0F;
         this.currentPoint = null;
         this.currentSelected = null;
      } else {
         float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
         this.currentHue = hsb[0];
         float s = hsb[1];
         float v = hsb[2];
         float invV = Math.abs(v - 1.0F);
         int x = (int)(s * this.width);
         int y = (int)(invV * this.getSpectrumHeight());
         this.currentPoint = new Point(x, y);
         this.currentSelected = color;
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      drawSatValSpectrum(this.getX(), this.getY(), this.width, this.getSpectrumHeight(), this.currentHue);
      int hueY = this.getHueBarY();
      drawHueBar(this.getX(), hueY, this.width, this.getHueBarHeight());
      int selectedHueX = this.getX() + (int)(this.currentHue * this.width);
      int selectedHueRadius = this.getHueBarHeight() / 2 + 5;
      int selectedHueCenterX = GameMath.limit(selectedHueX, this.getX() + selectedHueRadius - 2, this.getX() + this.width - selectedHueRadius + 2);
      Renderer.drawCircle(selectedHueCenterX, hueY + this.getHueBarHeight() / 2, selectedHueRadius, 30, this.backgroundColor, true);
      Renderer.drawCircle(
         selectedHueCenterX, hueY + this.getHueBarHeight() / 2, selectedHueRadius - 2, 30, Color.getHSBColor(this.currentHue, 1.0F, 1.0F), true
      );
      if (this.currentPoint != null) {
         Renderer.drawCircle(this.getX() + this.currentPoint.x, this.getY() + this.currentPoint.y, 10, 30, this.backgroundColor, true);
         Renderer.drawCircle(this.getX() + this.currentPoint.x, this.getY() + this.currentPoint.y, 8, 30, this.currentSelected, true);
      }
   }

   public static Color getColorAt(int x, int y, int width, int height, float hue) {
      x = GameMath.limit(x, 0, width);
      y = GameMath.limit(y, 0, height);
      float sat = (float)x / width;
      float val = Math.abs((float)y / height - 1.0F);
      return Color.getHSBColor(hue, sat, val);
   }

   public static void drawSatValSpectrum(int x, int y, int width, int height, float hue) {
      hue = GameMath.limit(hue, 0.0F, 1.0F);
      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(1);

      for (int i = 0; i <= width; i++) {
         float currentPerc = (float)i / width;
         Color top = Color.getHSBColor(hue, currentPerc, 1.0F);
         GL11.glColor4f(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x + i, y);
         Color bot = Color.getHSBColor(hue, currentPerc, 0.0F);
         GL11.glColor4f(bot.getRed() / 255.0F, bot.getGreen() / 255.0F, bot.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x + i, y + height);
      }

      GL11.glEnd();
   }

   public static float getHueAt(int x, int width) {
      x = GameMath.limit(x, 0, width);
      return (float)x / width;
   }

   public static void drawHueBar(int x, int y, int width, int height) {
      drawHueBar(x, y, width, height, hue -> Color.getHSBColor(hue, 1.0F, 1.0F));
   }

   public static void drawHueBar(int x, int y, int width, int height, Function<Float, Color> hueToColor) {
      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(1);

      for (int i = 0; i <= width; i++) {
         float currentPerc = (float)i / width;
         Color c = hueToColor.apply(currentPerc);
         GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
         GL11.glVertex2f(x + i, y);
         GL11.glVertex2f(x + i, y + height);
      }

      GL11.glEnd();
   }

   protected int getSpectrumHeight() {
      return this.height - 30;
   }

   protected int getHueBarHeight() {
      return 10;
   }

   protected int getHueBarY() {
      return this.getY() + this.height - this.getHueBarHeight() - 10;
   }

   protected boolean isMouseOverSpectrum(InputEvent event) {
      return event.isMoveUsed()
         ? false
         : new Rectangle(this.getX(), this.getY(), this.width, this.getSpectrumHeight()).contains(event.pos.hudX, event.pos.hudY);
   }

   protected boolean isMouseOverHueBar(InputEvent event) {
      return event.isMoveUsed()
         ? false
         : new Rectangle(this.getX(), this.getHueBarY(), this.width, this.getHueBarHeight()).contains(event.pos.hudX, event.pos.hudY);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), this.width, this.height));
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   public static float[] RGBtoHSL(float r, float g, float b) {
      float min = Math.min(r, Math.min(g, b));
      float max = Math.max(r, Math.max(g, b));
      float h = 0.0F;
      if (max == min) {
         h = 0.0F;
      } else if (max == r) {
         h = (60.0F * (g - b) / (max - min) + 360.0F) % 360.0F;
      } else if (max == g) {
         h = 60.0F * (b - r) / (max - min) + 120.0F;
      } else if (max == b) {
         h = 60.0F * (r - g) / (max - min) + 240.0F;
      }

      float l = (max + min) / 2.0F;
      float s;
      if (max == min) {
         s = 0.0F;
      } else if (l <= 0.5F) {
         s = (max - min) / (max + min);
      } else {
         s = (max - min) / (2.0F - max - min);
      }

      return new float[]{h / 360.0F, s, l};
   }

   public static float[] HSLtoRGB(float h, float s, float l) {
      float q;
      if (l < 0.5) {
         q = l * (1.0F + s);
      } else {
         q = l + s - s * l;
      }

      float p = 2.0F * l - q;
      float r = Math.max(0.0F, HueToRGB(p, q, h + 0.33333334F));
      float g = Math.max(0.0F, HueToRGB(p, q, h));
      float b = Math.max(0.0F, HueToRGB(p, q, h - 0.33333334F));
      r = Math.min(r, 1.0F);
      g = Math.min(g, 1.0F);
      b = Math.min(b, 1.0F);
      return new float[]{r, g, b};
   }

   public static float[] toHSL(Color color) {
      return RGBtoHSL(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F);
   }

   public static Color fromHSL(float h, float s, float l) {
      return fromHSL(h, s, l, 1.0F);
   }

   public static Color fromHSL(float h, float s, float l, float alpha) {
      float[] rgb = HSLtoRGB(h, s, l);
      return new Color(rgb[0], rgb[1], rgb[2], alpha);
   }

   private static float HueToRGB(float p, float q, float h) {
      if (h < 0.0F) {
         h++;
      }

      if (h > 1.0F) {
         h--;
      }

      if (6.0F * h < 1.0F) {
         return p + (q - p) * 6.0F * h;
      } else if (2.0F * h < 1.0F) {
         return q;
      } else {
         return 3.0F * h < 2.0F ? p + (q - p) * 6.0F * (0.6666667F - h) : p;
      }
   }
}
