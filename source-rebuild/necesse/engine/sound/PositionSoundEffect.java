package necesse.engine.sound;

import java.util.function.Supplier;

public class PositionSoundEffect extends GlobalSoundEffect {
   protected int minDistance = 75;
   protected int listenDistance = 750;
   protected float falloffExponent = 0.01F;
   protected PrimitiveSoundEmitter emitter;

   PositionSoundEffect(Supplier<Float> settingVolumeMod, PrimitiveSoundEmitter emitter) {
      super(settingVolumeMod);
      this.emitter = emitter;
   }

   public PositionSoundEffect pitch(float pitch) {
      super.pitch(pitch);
      return this;
   }

   public PositionSoundEffect volume(float volume) {
      super.volume(volume);
      return this;
   }

   public PositionSoundEffect minFalloffDistance(int distance) {
      this.minDistance = distance;
      return this;
   }

   public PositionSoundEffect falloffDistance(int distance) {
      this.listenDistance = distance;
      return this;
   }

   public PositionSoundEffect falloffExponent(float falloffExponent) {
      this.falloffExponent = falloffExponent;
      return this;
   }

   public PositionSoundEffect emitter(PrimitiveSoundEmitter emitter) {
      this.emitter = emitter;
      return this;
   }

   public static float getGain(float falloffExponent, float distance, int minDistance, int listenDistance) {
      return distance < 0.0F ? 0.0F : (float)Math.pow(falloffExponent, Math.max(0.0F, (distance - minDistance) / listenDistance));
   }

   @Override
   public void updateSound(SoundPlayer player, int source, float sourceVolumeMod, SoundEmitter listener) {
      PrimitiveSoundEmitter emitter = this.getEmitter();
      float volumeMod = this.settingVolumeMod.get() * this.volume * sourceVolumeMod;
      player.alSetPitch(this.pitch);
      float distance = emitter.getSoundDistance(listener.getSoundPositionX(), listener.getSoundPositionY());
      float gain = getGain(this.falloffExponent, distance, this.minDistance, this.listenDistance);
      player.alSetGain(volumeMod * gain);
      java.awt.geom.Point2D.Float dir = emitter.getSoundDirection(listener.getSoundPositionX(), listener.getSoundPositionY());
      if (dir != null && distance > this.minDistance) {
         float dirMod = 1.0F;
         if (distance - this.minDistance < 150.0F) {
            dirMod = (distance - this.minDistance) / 150.0F;
         }

         player.alSetPosition(getSoundPosition(new float[]{dir.x * dirMod, dir.y * dirMod, 1.0F}));
      } else {
         player.alSetPosition(getSoundPosition(new float[]{0.0F, 0.0F, 1.0F}));
      }
   }

   @Override
   public PrimitiveSoundEmitter getEmitter() {
      return this.emitter;
   }
}
