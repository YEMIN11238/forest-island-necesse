package necesse.entity.mobs.hostile.bosses;

import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;

public class SageHead extends FlyingSpiritsHead {
   public SageHead() {
      super(FlyingSpiritsHead.Variant.SAGE);
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         SoundManager.playSound(GameResources.sagebegin, SoundEffect.effect(this).volume(1.6F).falloffDistance(4000));
      }
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.sagehurt).volume(0.4F).fallOffDistance(1500);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.sagedeath).fallOffDistance(3000);
   }
}
