package necesse.gfx.shader;

import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.shader.shaderVariable.ShaderFloatVariable;

public class ShockwaveShader extends GameShader {
   public ShockwaveShader(boolean addVariables) {
      super("vertScene", "fragShockwave");
      if (addVariables) {
         this.addVariable(new ShaderFloatVariable("waveDistance", 0.0F, 1000.0F, 100));
         this.addVariable(new ShaderFloatVariable("waveSize", 0.0F, 200.0F, 100));
         this.addVariable(new ShaderFloatVariable("easingScale", -1.0F, 2.0F, 100));
         this.addVariable(new ShaderFloatVariable("easingPower", -5.0F, 5.0F, 100));
      }
   }

   @Override
   public void use() {
      super.use();
      GameWindow window = WindowManager.getWindow();
      this.pass1i("screenWidth", window.getSceneWidth());
      this.pass1i("screenHeight", window.getSceneHeight());
   }
}
