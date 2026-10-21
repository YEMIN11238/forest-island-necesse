package necesse.entity.mobs.friendly;

public interface RopeClearerMob {
   boolean shouldClearAfterTime(long var1);

   default void clearRopeFromMob(FriendlyRopableMob mob) {
      mob.removeRope(false);
      if (mob.buyPrice != null) {
         mob.shouldEscape = true;
         mob.canDespawn = true;
      }
   }
}
