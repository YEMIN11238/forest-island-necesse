package necesse.engine.sound;

import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.util.GameRandom;

public class SoundSettings {
   public static final int defaultFallOffDistance = 750;
   public static final float defaultVolume = 1.0F;
   public static final float defaultBasePitch = 1.0F;
   public static final float defaultPitchVariance = 0.05F;
   public final GameSound[] sounds;
   protected SoundSettings.UnsetVar<Integer> fallOffDistance = new SoundSettings.UnsetVar<>(750);
   protected SoundSettings.UnsetVar<Float> volume = new SoundSettings.UnsetVar<>(1.0F);
   protected SoundSettings.UnsetVar<Float> basePitch = new SoundSettings.UnsetVar<>(1.0F);
   protected SoundSettings.UnsetVar<Float> pitchVariance = new SoundSettings.UnsetVar<>(0.05F);
   protected SoundSettings.UnsetVar<SoundCooldown> cooldown = new SoundSettings.UnsetVar<>(null);

   public SoundSettings(GameSound sound) {
      if (sound == null) {
         this.sounds = null;
      } else {
         this.sounds = new GameSound[]{sound};
      }
   }

   public SoundSettings(GameSound sound1, GameSound... otherSounds) {
      if (sound1 == null) {
         this.sounds = null;
      } else {
         this.sounds = new GameSound[otherSounds.length + 1];
         this.sounds[0] = sound1;
         System.arraycopy(otherSounds, 0, this.sounds, 1, otherSounds.length);
      }
   }

   public SoundSettings volume(float volume) {
      this.volume.setValue(volume);
      return this;
   }

   public SoundSettings basePitch(float basePitch) {
      this.basePitch.setValue(basePitch);
      return this;
   }

   public SoundSettings pitchVariance(float pitchVariance) {
      this.pitchVariance.setValue(pitchVariance);
      return this;
   }

   public SoundSettings fallOffDistance(int fallOffDistance) {
      this.fallOffDistance.setValue(fallOffDistance);
      return this;
   }

   public SoundSettings cooldown(SoundCooldown cooldown) {
      this.cooldown.setValue(cooldown);
      return this;
   }

   public SoundSettings setVolumeIfNotSet(float volume) {
      if (!this.volume.hasBeenSet()) {
         this.volume.setValue(volume);
      }

      return this;
   }

   public SoundSettings setBasePitchIfNotSet(float basePitch) {
      if (!this.basePitch.hasBeenSet()) {
         this.basePitch.setValue(basePitch);
      }

      return this;
   }

   public SoundSettings setPitchVarianceIfNotSet(float pitchVariance) {
      if (!this.pitchVariance.hasBeenSet()) {
         this.pitchVariance.setValue(pitchVariance);
      }

      return this;
   }

   public SoundSettings setFallOffDistanceIfNotSet(int fallOffDistance) {
      if (!this.fallOffDistance.hasBeenSet()) {
         this.fallOffDistance.setValue(fallOffDistance);
      }

      return this;
   }

   public SoundSettings setCooldownIfNotSet(SoundCooldown cooldown) {
      if (!this.cooldown.hasBeenSet()) {
         this.cooldown.setValue(cooldown);
      }

      return this;
   }

   public GameSound getRandomSound() {
      return this.sounds != null && this.sounds.length != 0 ? this.sounds[GameRandom.globalRandom.nextInt(this.sounds.length)] : null;
   }

   public float getVolume() {
      return this.volume.getValue();
   }

   public float getBasePitch() {
      return this.basePitch.getValue();
   }

   public float getPitchVariance() {
      return this.pitchVariance.getValue();
   }

   public float getFinalRandomPitch() {
      float variance = this.pitchVariance.getValue();
      return variance <= 0.0F ? this.basePitch.getValue() : GameRandom.globalRandom.getFloatOffset(this.basePitch.getValue(), variance);
   }

   public int getFallOffDistance() {
      return this.fallOffDistance.getValue();
   }

   public SoundCooldown getCooldown() {
      return this.cooldown.getValue();
   }

   public SoundEffect applySettings(PositionSoundEffect effect) {
      return effect.volume(this.getVolume()).pitch(this.getFinalRandomPitch()).falloffDistance(this.getFallOffDistance());
   }

   public void play(PositionSoundEffect soundEffect) {
      SoundManager.playSound(this, soundEffect);
   }

   public void play(PrimitiveSoundEmitter emitter) {
      SoundManager.playSound(this, emitter);
   }

   public void play(float x, float y) {
      SoundManager.playSound(this, x, y);
   }

   public static class UnsetVar<T> {
      private boolean hasBeenSet = false;
      protected T value;

      public UnsetVar(T defaultValue) {
         this.value = defaultValue;
      }

      public SoundSettings.UnsetVar<T> setValue(T newValue) {
         this.value = newValue;
         this.hasBeenSet = true;
         return this;
      }

      public T getValue() {
         return this.value;
      }

      public boolean hasBeenSet() {
         return this.hasBeenSet;
      }
   }
}
