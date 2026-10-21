package necesse.gfx.gameFont;

import java.awt.Color;

public class FontOptions extends FontBasicOptions {
   private boolean outline;

   public FontOptions(int size) {
      super(size);
   }

   public FontOptions(FontOptions copy) {
      super(copy);
      this.outline = copy.outline;
   }

   public FontOptions copy() {
      return new FontOptions(this);
   }

   public FontOptions size(int size) {
      super.size(size);
      return this;
   }

   public FontOptions forcePixelFont() {
      super.forcePixelFont();
      return this;
   }

   public FontOptions forceNonPixelFont() {
      super.forceNonPixelFont();
      return this;
   }

   public FontOptions color(float[] color) {
      super.color(color);
      return this;
   }

   public FontOptions colorf(float red, float green, float blue, float alpha) {
      super.colorf(red, green, blue, alpha);
      return this;
   }

   public FontOptions colorf(float red, float green, float blue) {
      super.colorf(red, green, blue);
      return this;
   }

   public FontOptions color(Color color) {
      super.color(color);
      return this;
   }

   public FontOptions color(int red, int green, int blue, int alpha) {
      super.color(red, green, blue, alpha);
      return this;
   }

   public FontOptions color(int red, int green, int blue) {
      super.color(red, green, blue);
      return this;
   }

   public FontOptions alphaf(float alpha) {
      super.alphaf(alpha);
      return this;
   }

   public FontOptions alpha(int alpha) {
      super.alpha(alpha);
      return this;
   }

   public FontOptions defaultColor(float[] color) {
      super.defaultColor(color);
      return this;
   }

   public FontOptions defaultColorf(float red, float green, float blue, float alpha) {
      super.defaultColorf(red, green, blue, alpha);
      return this;
   }

   public FontOptions defaultColorf(float red, float green, float blue) {
      super.defaultColorf(red, green, blue);
      return this;
   }

   public FontOptions defaultColor(Color color) {
      super.defaultColor(color);
      return this;
   }

   public FontOptions defaultColor(int red, int green, int blue, int alpha) {
      super.defaultColor(red, green, blue, alpha);
      return this;
   }

   public FontOptions defaultColor(int red, int green, int blue) {
      super.defaultColor(red, green, blue);
      return this;
   }

   public boolean getOutline() {
      return this.outline;
   }

   public FontOptions outline(boolean outline) {
      this.outline = outline;
      return this;
   }

   public FontOptions outline() {
      return this.outline(true);
   }

   public FontOptions clearOutline() {
      return this.outline(false);
   }

   public FontOptions outline(float[] color) {
      super.outline(color);
      return this;
   }

   public FontOptions clearOutlineColor() {
      super.clearOutlineColor();
      return this;
   }

   public FontOptions outlinef(float red, float green, float blue, float alpha) {
      super.outlinef(red, green, blue, alpha);
      return this;
   }

   public FontOptions outlinef(float red, float green, float blue) {
      super.outlinef(red, green, blue);
      return this;
   }

   public FontOptions outline(Color color) {
      super.outline(color);
      return this;
   }

   public FontOptions outline(int red, int green, int blue, int alpha) {
      super.outline(red, green, blue, alpha);
      return this;
   }

   public FontOptions outline(int red, int green, int blue) {
      super.outline(red, green, blue);
      return this;
   }

   public FontOptions clearShadow() {
      super.clearShadow();
      return this;
   }

   public FontOptions shadow(float[] color) {
      super.shadow(color);
      return this;
   }

   public FontOptions shadowOffset(int xOffset, int yOffset) {
      super.shadowOffset(xOffset, yOffset);
      return this;
   }

   public FontOptions shadow(float[] color, int xOffset, int yOffset) {
      super.shadow(color, xOffset, yOffset);
      return this;
   }

   public FontOptions shadow(float red, float green, float blue, float alpha, int xOffset, int yOffset) {
      super.shadow(red, green, blue, alpha, xOffset, yOffset);
      return this;
   }

   public FontOptions shadow(Color color, int xOffset, int yOffset) {
      super.shadow(color, xOffset, yOffset);
      return this;
   }

   public FontOptions shadow(float alpha, int xOffset, int yOffset) {
      super.shadow(alpha, xOffset, yOffset);
      return this;
   }

   public FontOptions shadow(float red, float green, float blue, float alpha) {
      super.shadow(red, green, blue, alpha);
      return this;
   }

   public FontOptions shadow(Color color) {
      super.shadow(color);
      return this;
   }

   public FontOptions shadow(float alpha) {
      super.shadow(alpha);
      return this;
   }
}
