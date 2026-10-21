package necesse.gfx.shader;

import necesse.engine.window.WindowManager;

public class RectangleShader extends GameShader {
   public RectangleShader() {
      super("vertScene", "fragRectangle");
   }

   public void use(int x, int y, int width, int height) {
      this.use();
      y = Math.abs(y - WindowManager.getWindow().getCurrentBuffer().getHeight() + height);
      this.pass1i("x", x);
      this.pass1i("y", y);
      this.pass1i("w", width);
      this.pass1i("h", height);
   }
}
