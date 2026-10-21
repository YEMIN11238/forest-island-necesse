package necesse.level.maps.light;

import java.awt.Color;
import necesse.engine.Settings;
import necesse.engine.util.GameMath;
import org.lwjgl.opengl.GL11;

public class GameLightColor extends GameLight {
   protected byte red;
   protected byte green;
   protected byte blue;

   private GameLightColor(byte red, byte green, byte blue, float level) {
      super(level);
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.level = level;
   }

   protected GameLightColor(float level) {
      super(level);
      this.red = -1;
      this.green = -1;
      this.blue = -1;
   }

   protected GameLightColor(float hue, float saturation, float level) {
      super(level);
      float x = saturation * (1.0F - Math.abs(hue / 60.0F % 2.0F - 1.0F));
      float m = 1.0F - saturation;
      float r;
      float g;
      float b;
      if (hue < 60.0F) {
         r = saturation;
         g = x;
         b = 0.0F;
      } else if (hue < 120.0F) {
         r = x;
         g = saturation;
         b = 0.0F;
      } else if (hue < 180.0F) {
         r = 0.0F;
         g = saturation;
         b = x;
      } else if (hue < 240.0F) {
         r = 0.0F;
         g = x;
         b = saturation;
      } else if (hue < 300.0F) {
         r = x;
         g = 0.0F;
         b = saturation;
      } else {
         r = saturation;
         g = 0.0F;
         b = x;
      }

      this.red = (byte)((r + m) * 255.0F);
      this.green = (byte)((g + m) * 255.0F);
      this.blue = (byte)((b + m) * 255.0F);
   }

   protected static GameLightColor fromColor(Color color, float level) {
      return new GameLightColor((byte)color.getRed(), (byte)color.getGreen(), (byte)color.getBlue(), level);
   }

   protected static GameLightColor fromColor(Color color, float saturation, float level) {
      float satInvColor = Math.abs(saturation - 1.0F) * 255.0F;
      byte red = (byte)Math.min(255.0F, satInvColor + color.getRed() * saturation);
      byte green = (byte)Math.min(255.0F, satInvColor + color.getGreen() * saturation);
      byte blue = (byte)Math.min(255.0F, satInvColor + color.getBlue() * saturation);
      return new GameLightColor(red, green, blue, level);
   }

   public GameLightColor copy() {
      return new GameLightColor(this.red, this.green, this.blue, this.level);
   }

   @Override
   public GameLight minLevelCopy(float level) {
      GameLightColor copy = new GameLightColor(level);
      copy.combine(this, 0.5F);
      return copy;
   }

   @Override
   public boolean combine(GameLight other, float sat) {
      if (!(other instanceof GameLightColor)) {
         return super.combine(other, sat);
      } else {
         GameLightColor otherC = (GameLightColor)other;
         if ((this.red != otherC.red || this.green != otherC.green || this.blue != otherC.blue) && other.level > 0.0F) {
            float thisLevel = this.getFloatLevel();
            float otherLevel = other.getFloatLevel();
            float levelDelta = Math.min(1.0F, thisLevel / otherLevel * sat * 0.5F);
            int thisCol = this.unsigned(this.red);
            int otherCol = this.unsigned(otherC.red);
            byte newCol = (byte)Math.min(255, otherCol + (int)((thisCol - otherCol) * levelDelta));
            boolean changed = newCol != this.red;
            this.red = newCol;
            thisCol = this.unsigned(this.green);
            otherCol = this.unsigned(otherC.green);
            newCol = (byte)Math.min(255, otherCol + (int)((thisCol - otherCol) * levelDelta));
            changed = changed || newCol != this.green;
            this.green = newCol;
            thisCol = this.unsigned(this.blue);
            otherCol = this.unsigned(otherC.blue);
            newCol = (byte)Math.min(255, otherCol + (int)((thisCol - otherCol) * levelDelta));
            changed = changed || newCol != this.blue;
            this.blue = newCol;
            float newLevel = Math.max(other.level, this.level);
            changed = changed || newLevel != this.level;
            this.level = newLevel;
            return changed;
         } else {
            return super.combine(other, sat);
         }
      }
   }

   @Override
   public GameLight lerp(GameLight other, float percent) {
      if (other instanceof GameLightColor) {
         GameLightColor otherC = (GameLightColor)other;
         return new GameLightColor(
            (byte)GameMath.lerp(percent, this.unsigned(this.red), this.unsigned(otherC.red)),
            (byte)GameMath.lerp(percent, this.unsigned(this.green), this.unsigned(otherC.green)),
            (byte)GameMath.lerp(percent, this.unsigned(this.blue), this.unsigned(otherC.blue)),
            GameMath.lerp(percent, this.level, otherC.level)
         );
      } else {
         return super.lerp(other, percent);
      }
   }

   @Override
   public GameLight mix(GameLight other) {
      if (other instanceof GameLightColor) {
         GameLightColor otherC = (GameLightColor)other;
         return new GameLightColor(
            (byte)((this.unsigned(this.red) + this.unsigned(otherC.red)) / 2),
            (byte)((this.unsigned(this.green) + this.unsigned(otherC.green)) / 2),
            (byte)((this.unsigned(this.blue) + this.unsigned(otherC.blue)) / 2),
            (this.level + otherC.level) / 2.0F
         );
      } else {
         return super.mix(other);
      }
   }

   @Override
   public GameLight average(GameLight... others) {
      int red = this.unsigned(this.red);
      int green = this.unsigned(this.green);
      int blue = this.unsigned(this.blue);
      float level = this.level;

      for (GameLight other : others) {
         if (other instanceof GameLightColor) {
            GameLightColor otherC = (GameLightColor)other;
            red += this.unsigned(otherC.red);
            green += this.unsigned(otherC.green);
            blue += this.unsigned(otherC.blue);
            level += otherC.level;
         } else {
            red += 255;
            green += 255;
            blue += 255;
            level += other.level;
         }
      }

      int div = others.length + 1;
      return new GameLightColor((byte)(red / div), (byte)(green / div), (byte)(blue / div), level / div);
   }

   public int getRed() {
      return this.unsigned(this.red);
   }

   public int getGreen() {
      return this.unsigned(this.green);
   }

   public int getBlue() {
      return this.unsigned(this.blue);
   }

   @Override
   public float getFloatRed() {
      return this.getRed() / 255.0F;
   }

   @Override
   public float getFloatGreen() {
      return this.getGreen() / 255.0F;
   }

   @Override
   public float getFloatBlue() {
      return this.getBlue() / 255.0F;
   }

   @Override
   public int getColorHash() {
      return (this.getRed() & 0xFF) << 16 | (this.getGreen() & 0xFF) << 8 | this.getBlue() & 0xFF;
   }

   private int unsigned(byte value) {
      return value & 0xFF;
   }

   @Override
   public String toString() {
      return "L["
         + this.unsigned(this.red)
         + ","
         + this.unsigned(this.green)
         + ","
         + this.unsigned(this.blue)
         + ","
         + GameMath.toDecimals(this.getLevel(), 2)
         + "]";
   }

   @Override
   public Runnable getGLColorSetter(float red, float green, float blue, float alpha) {
      float mod = this.getFloatLevel();
      float r;
      float g;
      float b;
      if (Settings.lights == Settings.LightSetting.Color) {
         r = this.getFloatRed() * mod;
         g = this.getFloatGreen() * mod;
         b = this.getFloatBlue() * mod;
      } else {
         r = mod;
         g = mod;
         b = mod;
      }

      return () -> GL11.glColor4f(red * r, green * g, blue * b, alpha);
   }

   @Override
   public float[] getAdvColor() {
      float mod = this.getFloatLevel();
      float r;
      float g;
      float b;
      if (Settings.lights == Settings.LightSetting.Color) {
         r = this.getFloatRed() * mod;
         g = this.getFloatGreen() * mod;
         b = this.getFloatBlue() * mod;
      } else {
         r = mod;
         g = mod;
         b = mod;
      }

      return new float[]{r, g, b, 1.0F, r, g, b, 1.0F, r, g, b, 1.0F, r, g, b, 1.0F};
   }

   @Override
   public float[] getAdvColor(GameLight l2, GameLight l3, GameLight l4, float alpha) {
      float l1Mod = this.getFloatLevel();
      float l2Mod = l2.getFloatLevel();
      float l3Mod = l3.getFloatLevel();
      float l4Mod = l4.getFloatLevel();
      return Settings.lights == Settings.LightSetting.Color
         ? new float[]{
            this.getFloatRed() * l1Mod,
            this.getFloatGreen() * l1Mod,
            this.getFloatBlue() * l1Mod,
            alpha,
            l2.getFloatRed() * l2Mod,
            l2.getFloatGreen() * l2Mod,
            l2.getFloatBlue() * l2Mod,
            alpha,
            l3.getFloatRed() * l3Mod,
            l3.getFloatGreen() * l3Mod,
            l3.getFloatBlue() * l3Mod,
            alpha,
            l4.getFloatRed() * l4Mod,
            l4.getFloatGreen() * l4Mod,
            l4.getFloatBlue() * l4Mod,
            alpha
         }
         : new float[]{l1Mod, l1Mod, l1Mod, alpha, l2Mod, l2Mod, l2Mod, alpha, l3Mod, l3Mod, l3Mod, alpha, l4Mod, l4Mod, l4Mod, alpha};
   }
}
