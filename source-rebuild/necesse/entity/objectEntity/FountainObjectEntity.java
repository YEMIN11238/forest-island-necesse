package necesse.entity.objectEntity;

import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class FountainObjectEntity extends ObjectEntity {
   protected float waterSoundPitch = 1.0F;

   public FountainObjectEntity(Level level, int x, int y) {
      this(level, "fountain", x, y);
   }

   public FountainObjectEntity(Level level, String type, int x, int y) {
      super(level, type, x, y);
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         this.waterSoundPitch = GameRandom.globalRandom.getFloatBetween(0.9F, 1.04F);
      }
   }

   @Override
   public boolean shouldPlayAmbientSound() {
      return true;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.runningWater).volume(0.15F).basePitch(this.waterSoundPitch).pitchVariance(0.0F);
   }
}
