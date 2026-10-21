package necesse.gfx.gameFont;

import java.awt.Color;
import java.util.function.BooleanSupplier;
import necesse.engine.Settings;
import org.lwjgl.opengl.GL11;

public class FontBasicOptions {
   public static final float[] defaultColorArray = getColorArray(Color.WHITE);
   public static final float[] defaultStrokeColorArray = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   private float[] defaultColor = new float[defaultColorArray.length];
   private float[] defaultStrokeColor = new float[defaultStrokeColorArray.length];
   private BooleanSupplier pixelFont = () -> Settings.pixelFont;
   private int size;
   private float[] color;
   private float[] strokeColor;
   private float[] shadowColor;
   private int[] shadowOffset = new int[]{0, 4};

   public static float[] getColorArray(Color color) {
      return new float[]{color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F};
   }

   public FontBasicOptions(int size) {
      System.arraycopy(defaultColorArray, 0, this.defaultColor, 0, this.defaultColor.length);
      System.arraycopy(defaultStrokeColorArray, 0, this.defaultStrokeColor, 0, this.defaultStrokeColor.length);
      this.size = size;
      this.color = null;
      this.strokeColor = null;
   }

   public FontBasicOptions(FontBasicOptions copy) {
      this.defaultColor = copyArray(copy.defaultColor);
      this.defaultStrokeColor = copyArray(copy.defaultStrokeColor);
      this.size = copy.size;
      this.color = copyArray(copy.color);
      this.strokeColor = copyArray(copy.strokeColor);
   }

   public FontBasicOptions copy() {
      return new FontBasicOptions(this);
   }

   protected static float[] copyArray(float[] arr) {
      if (arr == null) {
         return null;
      }

      float[] newArr = new float[arr.length];
      System.arraycopy(arr, 0, newArr, 0, newArr.length);
      return newArr;
   }

   public int getSize() {
      return this.size;
   }

   public FontBasicOptions size(int size) {
      this.size = size;
      return this;
   }

   public boolean isPixelFont() {
      return this.pixelFont.getAsBoolean();
   }

   public FontBasicOptions forcePixelFont() {
      this.pixelFont = () -> true;
      return this;
   }

   public FontBasicOptions forceNonPixelFont() {
      this.pixelFont = () -> false;
      return this;
   }

   public boolean hasColor() {
      return this.color != null;
   }

   public float[] getColor() {
      return this.color == null ? this.defaultColor : this.color;
   }

   public Color getColorObj() {
      float[] color = this.getColor();
      return color == null ? null : new Color(color[0], color[1], color[2], color[3]);
   }

   public void applyGLColor() {
      float[] color = this.getColor();
      GL11.glColor4f(color[0], color[1], color[2], color[3]);
   }

   public float[] getStrokeColor() {
      return this.strokeColor == null ? this.defaultStrokeColor : this.strokeColor;
   }

   public Color getStrokeColorObj() {
      float[] color = this.getStrokeColor();
      return color == null ? null : new Color(color[0], color[1], color[2], color[3]);
   }

   public void applyGLStrokeColor() {
      float[] color = this.getColor();
      float[] strokeColor = this.getStrokeColor();
      GL11.glColor4f(strokeColor[0], strokeColor[1], strokeColor[2], color[3]);
   }

   public FontBasicOptions color(float[] color) {
      this.color = color;
      return this;
   }

   public FontBasicOptions colorf(float red, float green, float blue, float alpha) {
      return this.color(new float[]{red, green, blue, alpha});
   }

   public FontBasicOptions colorf(float red, float green, float blue) {
      return this.color(new float[]{red, green, blue, 1.0F});
   }

   public FontBasicOptions color(Color color) {
      return this.color(getColorArray(color));
   }

   public FontBasicOptions color(int red, int green, int blue, int alpha) {
      return this.color(new Color(red, green, blue, alpha));
   }

   public FontBasicOptions color(int red, int green, int blue) {
      return this.color(new Color(red, green, blue));
   }

   public FontBasicOptions alphaf(float alpha) {
      this.getColor()[3] = alpha;
      return this;
   }

   public FontBasicOptions alpha(int alpha) {
      return this.alphaf(alpha / 255.0F);
   }

   public float getAlpha() {
      return this.getColor()[3];
   }

   public FontBasicOptions defaultColor(float[] color) {
      this.defaultColor = color;
      return this;
   }

   public FontBasicOptions defaultColorf(float red, float green, float blue, float alpha) {
      return this.defaultColor(new float[]{red, green, blue, alpha});
   }

   public FontBasicOptions defaultColorf(float red, float green, float blue) {
      return this.defaultColor(new float[]{red, green, blue, 1.0F});
   }

   public FontBasicOptions defaultColor(Color color) {
      return this.defaultColor(getColorArray(color));
   }

   public FontBasicOptions defaultColor(int red, int green, int blue, int alpha) {
      return this.defaultColor(new Color(red, green, blue, alpha));
   }

   public FontBasicOptions defaultColor(int red, int green, int blue) {
      return this.defaultColor(new Color(red, green, blue));
   }

   public FontBasicOptions outline(float[] color) {
      this.strokeColor = color;
      return this;
   }

   public FontBasicOptions clearOutlineColor() {
      return this.outline((float[])null);
   }

   public FontBasicOptions outlinef(float red, float green, float blue, float alpha) {
      return this.outline(new float[]{red, green, blue, alpha});
   }

   public FontBasicOptions outlinef(float red, float green, float blue) {
      return this.outline(new float[]{red, green, blue, 1.0F});
   }

   public FontBasicOptions outline(Color color) {
      return color == null ? this.outline((float[])null) : this.outline(getColorArray(color));
   }

   public FontBasicOptions outline(int red, int green, int blue, int alpha) {
      return this.outline(new Color(red, green, blue, alpha));
   }

   public FontBasicOptions outline(int red, int green, int blue) {
      return this.outline(new Color(red, green, blue));
   }

   public FontBasicOptions clearShadow() {
      this.shadowColor = null;
      return this;
   }

   public FontBasicOptions shadow(float[] color) {
      this.shadowColor = color;
      return this;
   }

   public FontBasicOptions shadowOffset(int xOffset, int yOffset) {
      this.shadowOffset = new int[]{xOffset, yOffset};
      return this;
   }

   public FontBasicOptions shadow(float[] color, int xOffset, int yOffset) {
      this.shadowColor = color;
      this.shadowOffset = new int[]{xOffset, yOffset};
      return this;
   }

   public FontBasicOptions shadow(float red, float green, float blue, float alpha, int xOffset, int yOffset) {
      return this.shadow(new float[]{red, green, blue, alpha}, xOffset, yOffset);
   }

   public FontBasicOptions shadow(Color color, int xOffset, int yOffset) {
      return this.shadow(getColorArray(color), xOffset, yOffset);
   }

   public FontBasicOptions shadow(float alpha, int xOffset, int yOffset) {
      return this.shadow(0.0F, 0.0F, 0.0F, alpha, xOffset, yOffset);
   }

   public FontBasicOptions shadow(float red, float green, float blue, float alpha) {
      return this.shadow(new float[]{red, green, blue, alpha});
   }

   public FontBasicOptions shadow(Color color) {
      return this.shadow(getColorArray(color));
   }

   public FontBasicOptions shadow(float alpha) {
      return this.shadow(0.0F, 0.0F, 0.0F, alpha);
   }

   public float[] getShadowColor() {
      return this.shadowColor;
   }

   public Color getShadowColorObj() {
      float[] color = this.getShadowColor();
      return color == null ? null : new Color(color[0], color[1], color[2], color[3]);
   }

   public void applyGLShadowColor() {
      float[] color = this.getShadowColor();
      GL11.glColor4f(color[0], color[1], color[2], color[3]);
   }

   public int[] getShadowOffset() {
      return this.shadowOffset;
   }

   public boolean hasShadow() {
      return this.shadowColor != null;
   }
}
