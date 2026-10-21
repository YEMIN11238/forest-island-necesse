package necesse.engine.postProcessing.effects;

import necesse.gfx.camera.GameCamera;
import necesse.gfx.shader.ShockwaveShader;

public interface AbstractShockwaveEffect {
   int getDrawX(GameCamera var1);

   int getDrawY(GameCamera var1);

   float getCurrentDistance();

   float getSize();

   float getEasingScale();

   float getEasingPower();

   boolean shouldRemove();

   default void setupShader(ShockwaveShader shader, GameCamera camera) {
      shader.pass1i("drawX", this.getDrawX(camera));
      shader.pass1i("drawY", this.getDrawY(camera));
      shader.pass1f("waveDistance", this.getCurrentDistance());
      shader.pass1f("waveSize", this.getSize());
      shader.pass1f("easingScale", this.getEasingScale());
      shader.pass1f("easingPower", this.getEasingPower());
   }
}
