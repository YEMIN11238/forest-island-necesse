package necesse.engine.postProcessing.effects;

import necesse.engine.window.GameWindow;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.shader.HeatDistortionShader;

public abstract class AbstractHeatDistortionEffect {
   public long spawnTime = this.getCurrentTime();
   protected boolean shouldRemove;

   public abstract int getDrawX(GameCamera var1);

   public abstract int getDrawY(GameCamera var1);

   public abstract float getEllipseRadiusX();

   public abstract float getEllipseRadiusY();

   public abstract float getSpeed();

   public abstract float getAlpha();

   public boolean shouldRemove() {
      return this.shouldRemove;
   }

   public void remove() {
      this.shouldRemove = true;
   }

   public abstract long getCurrentTime();

   public int getTimeSinceSpawn() {
      return (int)(this.getCurrentTime() - this.spawnTime);
   }

   public int getTimeLeft(int totalTime) {
      return totalTime - this.getTimeSinceSpawn();
   }

   public boolean isWithinScreen(GameWindow window, GameCamera camera) {
      int drawX = this.getDrawX(camera);
      int drawY = this.getDrawY(camera);
      float radiusX = this.getEllipseRadiusX();
      float radiusY = this.getEllipseRadiusY();
      return drawX + radiusX > 0.0F && drawX - radiusX < window.getSceneWidth() && drawY + radiusY > 0.0F && drawY - radiusY < window.getSceneHeight();
   }

   public void setupShader(HeatDistortionShader shader, GameCamera camera) {
      shader.pass1i("drawX", this.getDrawX(camera));
      shader.pass1i("drawY", this.getDrawY(camera));
      shader.pass1f("radiusX", this.getEllipseRadiusX());
      shader.pass1f("radiusY", this.getEllipseRadiusY());
      shader.pass1f("speed", this.getSpeed());
      shader.pass1f("alpha", this.getAlpha());
      shader.pass1i("time", this.getTimeSinceSpawn());
   }
}
