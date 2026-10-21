package necesse.engine;

import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameRandom;

public class CameraShakeValues {
   private final GameRandom random = new GameRandom();
   public final int duration;
   public final int frequency;
   private final int samples;
   public final float xIntensity;
   public final float yIntensity;
   private final float[] xValues;
   private final float[] yValues;

   public CameraShakeValues(int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff, Float prevShake) {
      this.duration = duration;
      this.frequency = frequency;
      this.xIntensity = xIntensity;
      this.yIntensity = yIntensity;
      this.samples = duration / frequency + 2;
      this.xValues = this.generateValues(xIntensity, prevShake.x, addFallOff);
      this.yValues = this.generateValues(yIntensity, prevShake.y, addFallOff);
   }

   public CameraShakeValues(int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff) {
      this(duration, frequency, xIntensity, yIntensity, addFallOff, new Float());
   }

   private float[] generateValues(float intensity, float startPosition, boolean addFallOff) {
      float[] values = new float[this.samples];

      for (int i = 1; i < values.length - 1; i++) {
         float reverseProgress = 1.0F;
         if (addFallOff) {
            reverseProgress = Math.abs((float)i / values.length - 1.0F);
         }

         values[i] = (float)this.random.nextGaussian() * intensity * reverseProgress;
      }

      values[0] = startPosition;
      values[this.samples - 1] = 0.0F;
      return values;
   }

   public boolean isShaking(long timeProgress) {
      return timeProgress < this.duration;
   }

   public Float getCurrentShake(long timeProgress) {
      if (timeProgress >= 0L && timeProgress < this.duration) {
         float progress = (float)timeProgress / this.duration;
         int valueIndex = (int)(progress * (this.samples - 1));
         Float currentShake = new Float(this.xValues[valueIndex], this.yValues[valueIndex]);
         Float nextShake = new Float(this.xValues[valueIndex + 1], this.yValues[valueIndex + 1]);
         float timePerValue = (float)this.duration / (this.samples - 1);
         float pointProgress = ((float)timeProgress - valueIndex * timePerValue) / timePerValue;
         float cosProgress = (float)((Math.cos(Math.PI * (1.0F + pointProgress)) + 1.0) / 2.0);
         return new Float(currentShake.x + (nextShake.x - currentShake.x) * cosProgress, currentShake.y + (nextShake.y - currentShake.y) * cosProgress);
      } else {
         return new Float();
      }
   }

   public Float getCurrentShake(long startTime, long currentTime) {
      return this.getCurrentShake(currentTime - startTime);
   }
}
