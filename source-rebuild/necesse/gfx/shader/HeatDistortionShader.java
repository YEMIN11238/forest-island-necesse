package necesse.gfx.shader;

import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.shader.shaderVariable.ShaderFloatVariable;
import necesse.gfx.shader.shaderVariable.ShaderIntVariable;

public class HeatDistortionShader extends GameShader {
   public HeatDistortionShader(boolean addVariables) {
      super("vertScene", "fragHeatDistortion");
      if (addVariables) {
         this.addVariable(new ShaderFloatVariable("radiusX", 0.0F, 500.0F, 100));
         this.addVariable(new ShaderFloatVariable("radiusY", 0.0F, 500.0F, 100));
         this.addVariable(new ShaderIntVariable("time", 0, 60000));
         this.addVariable(new ShaderFloatVariable("speed", 0.0F, 500.0F, 100));
      }
   }

   @Override
   public void use() {
      super.use();
      GameWindow window = WindowManager.getWindow();
      this.pass1i("screenWidth", window.getSceneWidth());
      this.pass1i("screenHeight", window.getSceneHeight());
      this.pass1i("noiseTexture", 1);
   }
}
