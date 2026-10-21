package necesse.engine.util.tween;

import java.awt.Color;
import necesse.engine.util.GameMath;
import necesse.gfx.Renderer;

public class ColorTween extends ValueTween<Color, ColorTween> {
   private ColorTween.InterpolationMode interpolationMode = ColorTween.InterpolationMode.Linear;

   public ColorTween(double duration, Color initialValue, Color endValue) {
      super(duration, initialValue, endValue);
   }

   public ColorTween(Color initialValue) {
      super(initialValue);
   }

   public ColorTween(ColorTween existingTween, double duration, Color endValue) {
      super(existingTween, duration, endValue);
      this.interpolationMode = existingTween.interpolationMode;
   }

   public static void drawTweenDebug(ColorTween tween, double posX, double posY, int width, int height) {
      for (int i = 0; i < width; i++) {
         Color color = tween.updateAndGet((double)i / width);
         Renderer.drawLine((int)posX + i, (int)posY, (int)(posX + i), (int)(posY + height), color);
      }
   }

   public ColorTween.InterpolationMode getInterpolationMode() {
      return this.interpolationMode;
   }

   public ColorTween setInterpolationMode(ColorTween.InterpolationMode mode) {
      this.interpolationMode = mode;
      return this;
   }

   @Override
   protected void tween(double percent) {
      switch (this.interpolationMode) {
         case Linear:
            this.tweenLinear(percent);
            break;
         case Hue:
            this.tweenHue(percent, false);
            break;
         case HueLong:
            this.tweenHue(percent, true);
      }
   }

   protected void tweenLinear(double percent) {
      int startRed = this.startValue.getRed();
      int startGreen = this.startValue.getGreen();
      int startBlue = this.startValue.getBlue();
      int startAlpha = this.startValue.getAlpha();
      int endRed = this.endValue.getRed();
      int endGreen = this.endValue.getGreen();
      int endBlue = this.endValue.getBlue();
      int endAlpha = this.endValue.getAlpha();
      int red = GameMath.limit((int)Math.round(startRed + percent * (endRed - startRed)), 0, 255);
      int green = GameMath.limit((int)Math.round(startGreen + percent * (endGreen - startGreen)), 0, 255);
      int blue = GameMath.limit((int)Math.round(startBlue + percent * (endBlue - startBlue)), 0, 255);
      int alpha = GameMath.limit((int)Math.round(startAlpha + percent * (endAlpha - startAlpha)), 0, 255);
      this.setValue(new Color(red, green, blue, alpha));
   }

   protected void tweenHue(double percent, boolean forceLong) {
      float[] startHSB = Color.RGBtoHSB(this.startValue.getRed(), this.startValue.getGreen(), this.startValue.getBlue(), null);
      float[] endHSB = Color.RGBtoHSB(this.endValue.getRed(), this.endValue.getGreen(), this.endValue.getBlue(), null);
      float startHue = startHSB[0];
      float endHue = endHSB[0];
      float deltaHue = endHue - startHue;
      if (Math.abs(startHue + 1.0F - endHue) < Math.abs(deltaHue)) {
         startHue++;
         deltaHue = endHue - startHue;
      } else if (Math.abs(startHue - endHue - 1.0F) < Math.abs(deltaHue)) {
         endHue++;
         deltaHue = endHue - startHue;
      }

      if (startHue > endHue) {
         deltaHue = -(startHue - endHue);
      }

      if (forceLong) {
         if (deltaHue < 0.0F) {
            deltaHue++;
         } else {
            deltaHue--;
         }
      }

      float hue = (float)(startHue + deltaHue * percent);
      float saturation = (float)(startHSB[1] + percent * (endHSB[1] - startHSB[1]));
      float brightness = (float)(startHSB[2] + percent * (endHSB[2] - startHSB[2]));
      int startAlpha = this.startValue.getAlpha();
      int alpha = GameMath.limit((int)Math.round(startAlpha + percent * (this.endValue.getAlpha() - startAlpha)), 0, 255);
      int colorValue = Color.HSBtoRGB(hue, saturation, brightness);
      this.setValue(new Color(colorValue >> 16 & 0xFF, colorValue >> 8 & 0xFF, colorValue & 0xFF, alpha));
   }

   public ColorTween newTween(double duration, Color endValue) {
      return new ColorTween(this, duration, endValue);
   }

   public enum InterpolationMode {
      Linear,
      Hue,
      HueLong;
   }
}
