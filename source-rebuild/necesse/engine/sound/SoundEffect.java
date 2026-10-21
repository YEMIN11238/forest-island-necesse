package necesse.engine.sound;

import java.nio.FloatBuffer;
import necesse.engine.Settings;
import org.lwjgl.BufferUtils;

public abstract class SoundEffect {
   protected float volume = 1.0F;

   public abstract void updateSound(SoundPlayer var1, int var2, float var3, SoundEmitter var4);

   public SoundEffect volume(float volume) {
      this.volume = volume;
      return this;
   }

   public float getVolume() {
      return this.volume;
   }

   public abstract PrimitiveSoundEmitter getEmitter();

   protected static FloatBuffer getSoundPosition(float[] soundArray) {
      FloatBuffer buffer = BufferUtils.createFloatBuffer(3);
      buffer.put(soundArray);
      buffer.rewind();
      return buffer;
   }

   public static GlobalSoundEffect music() {
      return new GlobalSoundEffect(() -> Settings.masterVolume * Settings.musicVolume * 0.75F);
   }

   public static GlobalSoundEffect ui() {
      return new GlobalSoundEffect(() -> Settings.masterVolume * Settings.UIVolume);
   }

   public static GlobalSoundEffect globalEffect() {
      return new GlobalSoundEffect(() -> Settings.masterVolume * Settings.effectsVolume);
   }

   public static PositionSoundEffect effect(PrimitiveSoundEmitter emitter) {
      return new PositionSoundEffect(() -> Settings.masterVolume * Settings.effectsVolume, emitter);
   }

   public static PositionSoundEffect effect(float soundX, float soundY) {
      return effect(SoundPlayer.SimpleEmitter(soundX, soundY));
   }

   public static GlobalSoundEffect globalWeather() {
      return new GlobalSoundEffect(() -> Settings.masterVolume * Settings.weatherVolume);
   }

   public static PositionSoundEffect weather(PrimitiveSoundEmitter emitter) {
      return new PositionSoundEffect(() -> Settings.masterVolume * Settings.weatherVolume, emitter);
   }

   public static PositionSoundEffect weather(float soundX, float soundY) {
      return effect(SoundPlayer.SimpleEmitter(soundX, soundY));
   }
}
