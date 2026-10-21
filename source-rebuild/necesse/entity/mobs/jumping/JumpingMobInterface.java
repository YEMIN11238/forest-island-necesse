package necesse.entity.mobs.jumping;

import java.util.function.BiConsumer;
import necesse.engine.network.packet.PacketMobJump;
import necesse.engine.network.server.Server;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;

public interface JumpingMobInterface {
   JumpingMobStats getJumpStats();

   default void tickJump(float moveX, float moveY) {
      JumpingMobStats stats = this.getJumpStats();
      if (stats.mob.isServer()) {
         this.tickJump(moveX, moveY, (dx, dy) -> {
            ((JumpingMobInterface)stats.mob).runJump(dx, dy);
            Server server = stats.mob.getLevel().getServer();
            server.network.sendToClientsWithEntity(new PacketMobJump(stats.mob, dx, dy), stats.mob);
         });
      }
   }

   default void tickJump(float moveX, float moveY, BiConsumer<Float, Float> onJump) {
      JumpingMobStats stats = this.getJumpStats();
      if ((moveX != 0.0F || moveY != 0.0F)
         && stats.lastJumpTime + stats.lastJumpAnimationTime + stats.getJumpCooldown() < stats.mob.getWorldEntity().getLocalTime()) {
         java.awt.geom.Point2D.Float normalize = GameMath.normalize(moveX, moveY);
         float jumpStrength = stats.getJumpStrength();
         onJump.accept(normalize.x * jumpStrength, normalize.y * jumpStrength);
      }
   }

   default int getJumpAnimationFrame(int frames) {
      JumpingMobStats stats = this.getJumpStats();
      long jumpTime = Math.max(0L, stats.mob.getWorldEntity().getLocalTime() - stats.lastJumpTime);
      return jumpTime < stats.lastJumpAnimationTime ? GameUtils.getAnim(jumpTime, frames, stats.lastJumpAnimationTime) : 0;
   }

   default void runJump(float dx, float dy) {
      JumpingMobStats stats = this.getJumpStats();
      stats.mob.dx += dx;
      stats.mob.dy += dy;
      if (!stats.mob.isAttacking) {
         stats.mob.setFacingDir(dx, dy);
      }

      stats.lastJumpTime = stats.mob.getWorldEntity().getLocalTime();
      stats.lastJumpAnimationTime = stats.getJumpAnimationTime();
      this.onJump();
   }

   default void onJump() {
   }

   default void setJumpAnimationTime(int milliseconds) {
      this.getJumpStats().setJumpAnimationTime(milliseconds);
   }

   default void setJumpCooldown(int milliseconds) {
      this.getJumpStats().setJumpCooldown(milliseconds);
   }

   default void setJumpStrength(float strength) {
      this.getJumpStats().setJumpStrength(strength);
   }
}
