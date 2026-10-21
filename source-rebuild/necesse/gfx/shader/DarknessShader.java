package necesse.gfx.shader;

import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.shader.shaderVariable.ShaderFloatVariable;
import necesse.gfx.shader.shaderVariable.ShaderIntVariable;

public class DarknessShader extends GameShader {
   public int midScreenX;
   public int midScreenY;

   public DarknessShader() {
      super("vertScene", "fragDarkness");
      this.addVariable(new ShaderFloatVariable("intensity", 0.0F, 1.0F, 100));
      this.addVariable(new ShaderIntVariable("range", 0, 1000));
   }

   @Override
   public void use() {
      super.use();
      GameWindow window = WindowManager.getWindow();
      this.pass1i("screenWidth", window.getSceneWidth());
      this.pass1i("screenHeight", window.getSceneHeight());
      this.pass1i("midX", this.midScreenX);
      this.pass1i("midY", this.midScreenY);
   }
}
