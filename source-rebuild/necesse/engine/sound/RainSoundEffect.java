package necesse.engine.sound;

import java.util.function.Supplier;

public class RainSoundEffect extends GlobalSoundEffect {
   private final float dx;
   private final float dy;
   private final float distance;
   private final float intensity;

   public RainSoundEffect(Supplier<Float> settingVolumeMod, float dx, float dy, float distance, float intensity) {
      super(settingVolumeMod);
      this.dx = dx;
      this.dy = dy;
      this.distance = distance;
      this.intensity = intensity;
   }

   @Override
   public void updateSound(SoundPlayer player, int source, float sourceVolumeMod, SoundEmitter listener) {
      player.alSetPitch(this.pitch);
      float volumeMod = this.settingVolumeMod.get() * this.volume * this.intensity * 0.5F * sourceVolumeMod;
      float gain = PositionSoundEffect.getGain(0.02F, this.distance, 20, 200);
      player.alSetGain(volumeMod * gain);
   }
}
