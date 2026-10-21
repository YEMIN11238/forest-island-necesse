package necesse.engine.postProcessing.effects;

public abstract class AbstractPositionHeatDistortionEffect extends AbstractHeatDistortionEffect {
   public float radiusX;
   public float radiusY;
   public float speed;
   public int totalTime;
   public int fadeInTime = 500;
   public int fadeOutTime = 500;

   public AbstractPositionHeatDistortionEffect(float radiusX, float radiusY, float speed, int totalTime) {
      this.radiusX = radiusX;
      this.radiusY = radiusY;
      this.speed = speed;
      this.totalTime = totalTime;
   }

   @Override
   public float getEllipseRadiusX() {
      return this.radiusX;
   }

   @Override
   public float getEllipseRadiusY() {
      return this.radiusY;
   }

   @Override
   public float getSpeed() {
      return this.speed;
   }

   @Override
   public float getAlpha() {
      int timeSinceSpawn = this.getTimeSinceSpawn();
      if (timeSinceSpawn < this.fadeInTime) {
         return Math.max(0.0F, (float)timeSinceSpawn / this.fadeInTime);
      }

      if (this.totalTime > 0) {
         int timeLeft = this.totalTime - timeSinceSpawn;
         if (timeLeft < this.fadeOutTime) {
            return Math.max(0.0F, (float)timeLeft / this.fadeOutTime);
         }
      }

      return 1.0F;
   }

   @Override
   public boolean shouldRemove() {
      return super.shouldRemove() || this.totalTime > 0 && this.getTimeSinceSpawn() > this.totalTime;
   }
}
