package necesse.gfx.drawOptions;

import java.awt.Rectangle;

public interface DrawOptionsBox extends DrawOptions {
   Rectangle getBoundingBox();

   static DrawOptionsBox concat(final DrawOptionsBox... drawOptionsBoxes) {
      if (drawOptionsBoxes.length == 0) {
         throw new IllegalArgumentException("Must have at least one argument");
      } else {
         return new DrawOptionsBox() {
            @Override
            public void draw() {
               for (DrawOptionsBox drawOptionsBox : drawOptionsBoxes) {
                  drawOptionsBox.draw();
               }
            }

            @Override
            public Rectangle getBoundingBox() {
               Rectangle box = null;

               for (DrawOptionsBox drawOptionsBox : drawOptionsBoxes) {
                  if (box == null) {
                     box = drawOptionsBox.getBoundingBox();
                  } else {
                     box = box.union(drawOptionsBox.getBoundingBox());
                  }
               }

               return box;
            }
         };
      }
   }
}
