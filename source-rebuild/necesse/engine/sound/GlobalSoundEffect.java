package necesse.engine.sound;

import java.util.function.Supplier;

public class GlobalSoundEffect extends SoundEffect {
   protected Supplier<Float> settingVolumeMod;
   protected float pitch = 1.0F;

   GlobalSoundEffect(Supplier<Float> settingVolumeMod) {
      this.settingVolumeMod = settingVolumeMod;
   }

   public GlobalSoundEffect pitch(float pitch) {
      this.pitch = pitch;
      return this;
   }

   @Override
   public void updateSound(SoundPlayer player, int source, float sourceVolumeMod, SoundEmitter listener) {
      float volume = this.settingVolumeMod.get() * this.volume * sourceVolumeMod;
      player.alSetPitch(this.pitch);
      player.alSetGain(volume);
      player.alSetPosition(getSoundPosition(new float[]{0.0F, 0.0F, 0.0F}));
   }

   public GlobalSoundEffect volume(float volume) {
      this.volume = volume;
      return this;
   }

   @Override
   public PrimitiveSoundEmitter getEmitter() {
      return null;
   }
}
