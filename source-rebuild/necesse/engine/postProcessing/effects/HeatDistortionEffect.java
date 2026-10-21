package necesse.engine.postProcessing.effects;

import necesse.gfx.camera.GameCamera;

public abstract class HeatDistortionEffect extends AbstractPositionHeatDistortionEffect {
   public int centerX;
   public int centerY;

   public HeatDistortionEffect(int centerX, int centerY, float radiusX, float radiusY, float speed, int totalTime) {
      super(radiusX, radiusY, speed, totalTime);
      this.centerX = centerX;
      this.centerY = centerY;
   }

   @Override
   public int getDrawX(GameCamera camera) {
      return camera.getDrawX(this.centerX);
   }

   @Override
   public int getDrawY(GameCamera camera) {
      return camera.getDrawY(this.centerY);
   }
}
