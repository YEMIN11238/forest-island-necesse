package necesse.gfx.gameTooltips;

import java.awt.Color;
import java.util.function.Supplier;
import necesse.engine.Settings;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.input.controller.ControllerInputState;
import necesse.gfx.GameColor;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;

public class InputTooltip implements GameTooltips {
   public final Control control;
   public final ControllerInputState controllerState;
   private TextureDrawOptionsEnd controllerGlyph;
   private boolean controllerNotBound;
   public final String modifierKey;
   public final int inputKey;
   public final FairTypeDrawOptions tooltip;
   public final String tooltipString;
   public final int width;
   public final int height;
   public final String keyName;
   public final FontOptions fontOptions;

   protected InputTooltip(Control control, ControllerInputState controllerState, String modifierKey, int inputKey, String tooltip, float alpha) {
      this.control = control;
      if (control != null && Input.lastInputIsController) {
         this.controllerState = control.controllerState;
      } else {
         this.controllerState = controllerState;
      }

      this.modifierKey = modifierKey;
      this.inputKey = inputKey;
      this.tooltipString = tooltip;
      this.fontOptions = new FontOptions(Settings.tooltipTextSize).outline().alphaf(alpha);
      if (tooltip != null && !tooltip.isEmpty()) {
         FairType type = new FairType().append(this.fontOptions, tooltip).applyParsers(StringTooltips.getParsers(this.fontOptions));
         this.tooltip = type.getDrawOptions(FairType.TextAlign.LEFT, -1, true, false);
      } else {
         this.tooltip = null;
      }

      this.keyName = Input.getName(inputKey);
      this.width = this.calcWidth();
      this.height = 20;
   }

   public InputTooltip(String modifierKey, int inputKey, String tooltip, float alpha) {
      this(null, null, modifierKey, inputKey, tooltip, alpha);
   }

   public InputTooltip(int inputKey, String tooltip, float alpha) {
      this(null, inputKey, tooltip, alpha);
   }

   public InputTooltip(String modifierKey, int inputKey, String tooltip) {
      this(modifierKey, inputKey, tooltip, 1.0F);
   }

   public InputTooltip(int inputKey, String tooltip) {
      this(null, inputKey, tooltip);
   }

   public InputTooltip(String modifierKey, Control control, String tooltip, float alpha) {
      this(control, null, modifierKey, control.getKey(), tooltip, alpha);
   }

   public InputTooltip(Control control, String tooltip, float alpha) {
      this(null, control, tooltip, alpha);
   }

   public InputTooltip(String modifierKey, Control control, String tooltip) {
      this(modifierKey, control, tooltip, 1.0F);
   }

   public InputTooltip(Control control, String tooltip) {
      this(null, control, tooltip);
   }

   public InputTooltip(String modifierKey, ControllerInputState controllerState, String tooltip, float alpha) {
      this(null, controllerState, modifierKey, -1, tooltip, alpha);
   }

   public InputTooltip(ControllerInputState controllerState, String tooltip, float alpha) {
      this(null, controllerState, tooltip, alpha);
   }

   public InputTooltip(String modifierKey, ControllerInputState controllerState, String tooltip) {
      this(modifierKey, controllerState, tooltip, 1.0F);
   }

   public InputTooltip(ControllerInputState controllerState, String tooltip) {
      this(null, controllerState, tooltip);
   }

   private int calcWidth() {
      if (this.controllerState != null) {
         GameTexture glyph = ControllerInput.getStateGlyph(this.controllerState);
         if (glyph != null) {
            this.controllerGlyph = glyph.initDraw().size(16, false);
         } else {
            this.controllerNotBound = true;
         }
      }

      if (this.controllerGlyph != null) {
         return this.controllerGlyph.getWidth() + (this.tooltip == null ? 0 : 4 + this.tooltip.getBoundingBox().width);
      } else if (this.controllerNotBound) {
         return Control.getControlIconWidth(this.fontOptions, this.modifierKey, null, "?", this.tooltip);
      } else {
         return this.control != null
            ? Control.getControlIconWidth(this.fontOptions, this.modifierKey, this.control, this.keyName, this.tooltip)
            : Control.getControlIconWidth(this.fontOptions, this.modifierKey, this.inputKey, this.keyName, this.tooltip);
      }
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public void draw(int x, int y, Supplier<Color> defaultColor) {
      FontOptions fontOptions = new FontOptions(this.fontOptions);
      Color color = defaultColor == null ? GameColor.DEFAULT_COLOR.get() : defaultColor.get();
      color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(color.getAlpha() * fontOptions.getAlpha()));
      fontOptions.defaultColor(color);
      if (this.controllerGlyph != null) {
         this.controllerGlyph.draw(x, y);
         if (this.tooltip != null) {
            this.tooltip.draw(x + this.controllerGlyph.getWidth() + 4, y, color);
         }
      } else if (this.controllerNotBound) {
         Control.drawControlIcon(fontOptions, x, y, this.modifierKey, null, "?", this.tooltip);
      } else if (this.control != null) {
         Control.drawControlIcon(fontOptions, x, y, this.modifierKey, this.control, this.keyName, this.tooltip);
      } else {
         Control.drawControlIcon(fontOptions, x, y, this.modifierKey, this.inputKey, this.keyName, this.tooltip);
      }
   }

   @Override
   public int getDrawOrder() {
      return Integer.MIN_VALUE;
   }

   @Override
   public boolean matchesSearch(String search) {
      if (this.controllerState != null && this.controllerState.getDisplayName().translate().toLowerCase().contains(search)) {
         return true;
      } else if (this.control != null && this.control.text.translate().toLowerCase().contains(search)) {
         return true;
      } else {
         return this.tooltipString != null && this.tooltipString.toLowerCase().contains(search) ? true : this.keyName.toLowerCase().contains(search);
      }
   }
}
