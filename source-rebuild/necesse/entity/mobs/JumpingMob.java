package necesse.entity.mobs;

import necesse.entity.mobs.jumping.JumpingMobInterface;
import necesse.entity.mobs.jumping.JumpingMobStats;

public class JumpingMob extends AttackAnimMob implements JumpingMobInterface {
   protected JumpingMobStats jumpStats = new JumpingMobStats(this);

   public JumpingMob(int health) {
      super(health);
   }

   @Override
   protected void calcAcceleration(float speed, float friction, float moveX, float moveY, float delta) {
      boolean inLiquid = this.inLiquid();
      if (inLiquid) {
         super.calcAcceleration(speed, friction, moveX, moveY, delta);
      } else {
         this.tickJump(moveX, moveY);
         super.calcAcceleration(speed, friction, 0.0F, 0.0F, delta);
      }
   }

   @Override
   public JumpingMobStats getJumpStats() {
      return this.jumpStats;
   }
}
