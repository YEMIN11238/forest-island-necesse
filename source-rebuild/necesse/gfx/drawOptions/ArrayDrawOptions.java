package necesse.gfx.drawOptions;

import java.util.List;

public class ArrayDrawOptions implements DrawOptions {
   private DrawOptions[] options;

   public ArrayDrawOptions(DrawOptions... options) {
      this.options = options;
   }

   public ArrayDrawOptions(List<DrawOptions> options) {
      this(options.toArray(new DrawOptions[options.size()]));
   }

   @Override
   public void draw() {
      for (DrawOptions option : this.options) {
         option.draw();
      }
   }
}
