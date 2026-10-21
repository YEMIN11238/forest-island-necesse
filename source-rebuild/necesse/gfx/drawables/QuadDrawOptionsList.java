package necesse.gfx.drawables;

import java.awt.Color;

public class QuadDrawOptionsList {
   private final GLDrawOptionsList drawOptions = new GLDrawOptionsList();

   public synchronized QuadDrawOptionsList add(int drawX, int drawY, int width, int height, float[] advColor) {
      this.drawOptions.add(new float[]{drawX, drawY, drawX + width, drawY, drawX + width, drawY + height, drawX, drawY + height}, advColor);
      return this;
   }

   public synchronized QuadDrawOptionsList add(int drawX, int drawY, int width, int height, float red, float green, float blue, float alpha) {
      return this.add(
         drawX, drawY, width, height, new float[]{red, green, blue, alpha, red, green, blue, alpha, red, green, blue, alpha, red, green, blue, alpha}
      );
   }

   public synchronized QuadDrawOptionsList add(int drawX, int drawY, int width, int height, Color color) {
      float red = color.getRed() / 255.0F;
      float green = color.getGreen() / 255.0F;
      float blue = color.getBlue() / 255.0F;
      float alpha = color.getAlpha() / 255.0F;
      return this.add(drawX, drawY, width, height, red, green, blue, alpha);
   }

   public synchronized void draw() {
      this.drawOptions.draw(7, 4);
   }
}
