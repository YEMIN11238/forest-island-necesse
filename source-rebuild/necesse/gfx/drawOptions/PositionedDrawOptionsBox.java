package necesse.gfx.drawOptions;

import java.awt.Rectangle;

public interface PositionedDrawOptionsBox {
   Rectangle getBoundingBox(int var1, int var2);

   void draw(int var1, int var2);

   default DrawOptionsBox toDrawOptionsBox(final int drawX, final int drawY) {
      return new DrawOptionsBox() {
         @Override
         public Rectangle getBoundingBox() {
            return PositionedDrawOptionsBox.this.getBoundingBox(drawX, drawY);
         }

         @Override
         public void draw() {
            PositionedDrawOptionsBox.this.draw(drawX, drawY);
         }
      };
   }
}
