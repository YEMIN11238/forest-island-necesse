package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import necesse.level.maps.CollisionFilter;

public abstract class FlyingAttackingFollowingMob extends AttackingFollowingMob {
   public FlyingAttackingFollowingMob(int health) {
      super(health);
   }

   @Override
   public int getFlyingHeight() {
      return 50;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return null;
   }
}
