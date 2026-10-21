package necesse.gfx.shader;

import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.Renderer;

public class TestShader extends GameShader {
   public TestShader() {
      super("vertScene", "fragTest");
   }

   @Override
   public void use() {
      super.use();
      GameWindow window = WindowManager.getWindow();
      this.pass1i("displayWidth", window.getSceneWidth());
      this.pass1i("displayHeight", window.getSceneHeight());
   }

   public void drawTest(int x, int y, int width, int height) {
      this.use();
      Renderer.initQuadDraw(width, height).draw(x, y);
      this.stop();
   }
}
