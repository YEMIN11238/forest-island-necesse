package necesse.entity.mobs;

import necesse.engine.sound.SoundPlayer;

public class BoneSpikeMob extends ExplosiveSpikeMob {
   public BoneSpikeMob() {
   }

   public BoneSpikeMob(Mob mobOwner, GameDamage damage, long startCrackingTime) {
      super(mobOwner, damage, startCrackingTime);
   }

   @Override
   protected SoundPlayer playStartCrackingSound() {
      return super.playStartCrackingSound();
   }
}
