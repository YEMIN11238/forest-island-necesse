package necesse.entity.projectile;

import necesse.entity.levelEvent.explosionEvent.DynamiteExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;

public class DynamiteStickProjectile extends BombProjectile {
   public DynamiteStickProjectile() {
   }

   public DynamiteStickProjectile(float x, float y, float targetX, float targetY, int speed, int distance, GameDamage damage, Mob owner) {
      super(x, y, targetX, targetY, speed, distance, damage, owner);
   }

   @Override
   public void init() {
      super.init();
      this.stopsRotatingOnStationary = true;
   }

   @Override
   public int getFuseTime() {
      return 4000;
   }

   @Override
   public float getParticleAngle() {
      return 220.0F;
   }

   @Override
   public float getParticleDistance() {
      return 14.0F;
   }

   @Override
   public ExplosionEvent getExplosionEvent(float x, float y) {
      float toolTier = Math.max(6.0F, this.getOwnerToolTier() + 1.0F);
      return new DynamiteExplosionEvent(x, y, 230, new GameDamage(400.0F, 1000.0F), true, false, toolTier, this.getOwner());
   }
}
