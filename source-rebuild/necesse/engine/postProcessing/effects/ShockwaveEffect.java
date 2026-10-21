package necesse.engine.postProcessing.effects;

import necesse.entity.Entity;
import necesse.gfx.camera.GameCamera;

public abstract class ShockwaveEffect implements AbstractShockwaveEffect {
   public int centerX;
   public int centerY;
   public float maxDistance;
   public float fadeInDistance;
   public float fadeOutDistance;
   public float size;
   public int speed;
   public long spawnTime;

   public ShockwaveEffect(int centerX, int centerY, float maxDistance, float fadeInDistance, float fadeOutDistance, float size, int speed) {
      this.centerX = centerX;
      this.centerY = centerY;
      this.maxDistance = maxDistance;
      this.fadeInDistance = fadeInDistance;
      this.fadeOutDistance = fadeOutDistance;
      this.size = size;
      this.speed = speed;
      this.spawnTime = this.getCurrentTime();
   }

   @Override
   public int getDrawX(GameCamera camera) {
      return camera.getDrawX(this.centerX);
   }

   @Override
   public int getDrawY(GameCamera camera) {
      return camera.getDrawY(this.centerY);
   }

   @Override
   public float getCurrentDistance() {
      long timeSinceSpawn = this.getCurrentTime() - this.spawnTime;
      return Entity.getPositionAfterMillis(this.speed, (float)timeSinceSpawn);
   }

   @Override
   public float getSize() {
      float currentDistance = this.getCurrentDistance();
      float currentSize = this.size;
      if (currentDistance < this.fadeInDistance) {
         currentSize *= currentDistance / this.fadeInDistance;
      }

      if (this.maxDistance > 0.0F && currentDistance > this.maxDistance - this.fadeOutDistance) {
         currentSize *= (this.maxDistance - currentDistance) / this.fadeOutDistance;
      }

      return currentSize;
   }

   public abstract long getCurrentTime();

   @Override
   public float getEasingScale() {
      return 1.0F;
   }

   @Override
   public float getEasingPower() {
      return 2.0F;
   }

   @Override
   public boolean shouldRemove() {
      return this.getCurrentDistance() > this.maxDistance;
   }
}
