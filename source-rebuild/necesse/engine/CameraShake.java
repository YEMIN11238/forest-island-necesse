package necesse.engine;

import java.awt.geom.Point2D.Float;
import necesse.engine.sound.PositionSoundEffect;
import necesse.engine.sound.PrimitiveSoundEmitter;
import necesse.engine.sound.SoundEmitter;

public class CameraShake {
   public final long startTime;
   public final CameraShakeValues values;
   public PrimitiveSoundEmitter emitter;
   public int minDistance = 50;
   public int listenDistance = 1500;
   public float falloffExponent = 0.01F;
   private boolean removed;

   public CameraShake(long startTime, int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff, Float prevShake) {
      this.startTime = startTime;
      this.values = new CameraShakeValues(duration, frequency, xIntensity, yIntensity, addFallOff, prevShake);
   }

   public CameraShake(long startTime, int duration, int frequency, float xIntensity, float yIntensity, boolean addFallOff) {
      this(startTime, duration, frequency, xIntensity, yIntensity, addFallOff, new Float());
   }

   public CameraShake from(PrimitiveSoundEmitter emitter) {
      this.emitter = emitter;
      return this;
   }

   public CameraShake from(PrimitiveSoundEmitter emitter, int minDistance, int listenDistance) {
      this.emitter = emitter;
      this.minDistance = minDistance;
      this.listenDistance = listenDistance;
      return this;
   }

   public CameraShake from(PrimitiveSoundEmitter emitter, int minDistance, int listenDistance, float falloffExponent) {
      this.emitter = emitter;
      this.minDistance = minDistance;
      this.listenDistance = listenDistance;
      this.falloffExponent = falloffExponent;
      return this;
   }

   public Float getCurrentShake(long currentTime) {
      return this.values.getCurrentShake(this.startTime, currentTime);
   }

   public float getDistanceIntensity(SoundEmitter listenerPos) {
      float distance = 0.0F;
      if (this.emitter != null && listenerPos != null) {
         distance = this.emitter.getSoundDistance(listenerPos.getSoundPositionX(), listenerPos.getSoundPositionY());
      }

      return PositionSoundEffect.getGain(this.falloffExponent, distance, this.minDistance, this.listenDistance);
   }

   public Float getCurrentShake(long currentTime, SoundEmitter listenerPos) {
      Float currentShake = this.getCurrentShake(currentTime);
      float distanceIntensity = this.getDistanceIntensity(listenerPos);
      return new Float(currentShake.x * distanceIntensity, currentShake.y * distanceIntensity);
   }

   public void remove() {
      this.removed = true;
   }

   public boolean isOver(long currentTime) {
      return this.removed || !this.values.isShaking(currentTime - this.startTime);
   }
}
