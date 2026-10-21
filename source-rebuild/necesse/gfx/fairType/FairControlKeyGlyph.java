package necesse.gfx.fairType;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.util.FloatDimension;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FairControlKeyGlyph implements FairGlyph {
   public final FontOptions fontOptions;
   public final Control control;
   private int lastKey;
   private boolean lastIsController;
   private TextureDrawOptionsEnd controllerGlyph;
   private boolean controllerNotBound;
   private int width;
   private int height;
   private boolean isHovering;

   public FairControlKeyGlyph(FontOptions fontOptions, Control control) {
      this.fontOptions = fontOptions;
      this.control = control;
      this.lastKey = control.getKey();
      this.updateDimensions();
   }

   public FairControlKeyGlyph(Control control) {
      this(new FontOptions(16), control);
   }

   @Override
   public FloatDimension getDimensions() {
      if (this.lastKey != this.control.getKey() || this.lastIsController != Input.lastInputIsController) {
         this.updateDimensions();
         this.lastKey = this.control.getKey();
         this.lastIsController = Input.lastInputIsController;
      }

      return new FloatDimension(this.width, this.height);
   }

   @Override
   public void updateDimensions() {
      if (Input.lastInputIsController && this.control.controllerState != null) {
         GameTexture glyph = ControllerInput.getStateGlyph(this.control.controllerState);
         if (glyph != null) {
            this.controllerGlyph = glyph.initDraw().size(16, false);
         } else {
            this.controllerNotBound = true;
         }
      } else {
         this.controllerNotBound = false;
         this.controllerGlyph = null;
      }

      if (this.controllerGlyph != null) {
         this.width = this.controllerGlyph.getWidth();
      } else if (this.controllerNotBound) {
         this.width = Control.getControlIconWidth(this.fontOptions, null, null, "?", null);
      } else {
         this.width = Control.getControlIconWidth(this.fontOptions, null, this.control, Input.getName(this.control.getKey()), null);
      }

      this.height = Math.max(16, this.fontOptions.getSize());
   }

   @Override
   public void handleInputEvent(float drawX, float drawY, InputEvent event) {
      if (event.isMouseMoveEvent()) {
         Dimension dim = this.getDimensions().toInt();
         this.isHovering = new Rectangle((int)drawX, (int)drawY - dim.height - 4, dim.width, dim.height).contains(event.pos.hudX, event.pos.hudY);
      }
   }

   @Override
   public void draw(float x, float y, Color defaultColor) {
      String keyName = Input.getName(this.control.getKey());
      if (this.controllerGlyph != null) {
         this.controllerGlyph.draw((int)x, (int)y - this.height);
      } else if (this.controllerNotBound) {
         Control.drawControlIcon(this.fontOptions, (int)x + 1, (int)y - this.height - 2, null, null, "?", null);
      } else {
         Control.drawControlIcon(this.fontOptions, (int)x + 1, (int)y - this.height - 2, null, this.control, keyName, null);
      }

      if (this.isHovering) {
         GameTooltipManager.addTooltip(new StringTooltips(keyName), TooltipLocation.FORM_FOCUS);
      }
   }

   @Override
   public FairGlyph getTextBoxCharacter() {
      return this;
   }

   @Override
   public String getParseString() {
      return TypeParsers.getInputParseString(this.control);
   }
}
