package necesse.gfx.drawOptions;

import java.util.ArrayList;
import java.util.List;

public class DrawOptionsList extends ArrayList<DrawOptions> implements DrawOptions {
   public DrawOptionsList(List<DrawOptions> options) {
      super(options);
   }

   public DrawOptionsList(int initialCapacity) {
      super(initialCapacity);
   }

   public DrawOptionsList() {
   }

   @Override
   public void draw() {
      this.forEach(DrawOptions::draw);
   }
}
