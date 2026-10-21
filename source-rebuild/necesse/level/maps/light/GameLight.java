package necesse.level.maps.light;

import necesse.engine.util.GameMath;
import org.lwjgl.opengl.GL11;

public class GameLight {
   protected float level;
   private static final int whiteHash = 16777215;

   public GameLight(float level) {
      this.level = level;
   }

   public GameLight copy() {
      return new GameLight(this.level);
   }

   public GameLight minLevelCopy(float level) {
      if (this.level < level) {
         GameLight copy = this.copy();
         copy.level = level;
         return copy;
      } else {
         return this;
      }
   }

   public boolean combine(GameLight other) {
      return this.combine(other, 1.0F);
   }

   public boolean combine(GameLight other, float sat) {
      if (this.level < other.level) {
         this.level = other.level;
         return true;
      } else {
         return false;
      }
   }

   public GameLight lerp(GameLight other, float percent) {
      return new GameLight(GameMath.lerp(percent, this.level, other.level));
   }

   public GameLight mix(GameLight other) {
      return new GameLight((this.level + other.level) / 2.0F);
   }

   public GameLight average(GameLight... others) {
      float level = this.level;

      for (GameLight other : others) {
         level += other.level;
      }

      return new GameLight(level / (others.length + 1));
   }

   public boolean setLevel(float level) {
      if (this.level == level) {
         return false;
      }

      this.level = level;
      return true;
   }

   public float getLevel() {
      return this.level;
   }

   public float getFloatLevel() {
      return Math.min(1.0F, this.getLevel() / 150.0F);
   }

   public float getFloatRed() {
      return 1.0F;
   }

   public float getFloatGreen() {
      return 1.0F;
   }

   public float getFloatBlue() {
      return 1.0F;
   }

   public boolean isSameColor(GameLight other) {
      return this.getFloatRed() == other.getFloatRed() && this.getFloatGreen() == other.getFloatGreen() && this.getFloatBlue() == other.getFloatBlue();
   }

   public int getColorHash() {
      return 16777215;
   }

   @Override
   public String toString() {
      return "L[" + GameMath.toDecimals(this.getLevel(), 2) + "]";
   }

   public Runnable getGLColorSetter(float red, float green, float blue, float alpha) {
      float mod = this.getFloatLevel();
      return () -> GL11.glColor4f(red * mod, green * mod, blue * mod, alpha);
   }

   public float[] getAdvColor() {
      float mod = this.getFloatLevel();
      return new float[]{mod, mod, mod, 1.0F, mod, mod, mod, 1.0F, mod, mod, mod, 1.0F, mod, mod, mod, 1.0F};
   }

   public float[] getAdvColor(GameLight l2, GameLight l3, GameLight l4, float alpha) {
      float l1Mod = this.getFloatLevel();
      float l2Mod = l2.getFloatLevel();
      float l3Mod = l3.getFloatLevel();
      float l4Mod = l4.getFloatLevel();
      return new float[]{l1Mod, l1Mod, l1Mod, alpha, l2Mod, l2Mod, l2Mod, alpha, l3Mod, l3Mod, l3Mod, alpha, l4Mod, l4Mod, l4Mod, alpha};
   }
}
