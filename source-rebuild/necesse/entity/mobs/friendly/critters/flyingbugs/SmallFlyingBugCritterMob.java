package necesse.entity.mobs.friendly.critters.flyingbugs;

import java.awt.Rectangle;
import necesse.entity.mobs.misc.NetableMob;

public abstract class SmallFlyingBugCritterMob extends FlyingBugCritterMob implements NetableMob {
   public SmallFlyingBugCritterMob(int health) {
      super(health);
      this.spriteRes = 32;
      this.collision = new Rectangle(-7, -5, 14, 10);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -28, 32, 34);
   }

   @Override
   public boolean canTakeDamage() {
      return false;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
   }

   @Override
   public void playHitDeathSound() {
   }

   @Override
   public void playDeathSound() {
   }

   @Override
   public void playHurtSound() {
   }
}
