package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import necesse.engine.network.packet.PacketMountMobJump;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.jumping.JumpingMobInterface;
import necesse.entity.mobs.jumping.JumpingMobStats;

public class MountFollowingJumpingMob extends MountFollowingMob implements JumpingMobInterface {
   protected JumpingMobStats jumpStats = new JumpingMobStats(this);

   public MountFollowingJumpingMob(int health) {
      super(health);
   }

   @Override
   protected void calcAcceleration(float speed, float friction, float moveX, float moveY, float delta) {
      boolean inLiquid = this.inLiquid();
      if (inLiquid) {
         super.calcAcceleration(speed, friction, moveX, moveY, delta);
      } else {
         Mob mounter = this.getRider();
         boolean clientControlled = mounter != null && mounter.isPlayer;
         if (!clientControlled) {
            this.tickJump(moveX, moveY);
         } else if (this.isClient() && this.getLevel().getClient().getPlayer() == mounter) {
            this.tickJump(moveX, moveY, (dx, dy) -> {
               this.runJump(dx, dy);
               this.getLevel().getClient().network.sendPacket(new PacketMountMobJump(this, dx, dy));
            });
         }

         super.calcAcceleration(speed, friction, 0.0F, 0.0F, delta);
      }
   }

   @Override
   public JumpingMobStats getJumpStats() {
      return this.jumpStats;
   }
}
