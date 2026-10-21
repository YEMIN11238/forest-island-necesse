package necesse.entity.projectile;

import necesse.entity.levelEvent.explosionEvent.BombExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;

public class IronBombProjectile extends BombProjectile {
   public IronBombProjectile() {
   }

   public IronBombProjectile(float x, float y, float targetX, float targetY, int speed, int distance, GameDamage damage, Mob owner) {
      super(x, y, targetX, targetY, speed, distance, damage, owner);
   }

   @Override
   public int getFuseTime() {
      return 3000;
   }

   @Override
   public float getParticleAngle() {
      return 290.0F;
   }

   @Override
   public float getParticleDistance() {
      return 12.0F;
   }

   @Override
   public ExplosionEvent getExplosionEvent(float x, float y) {
      float toolTier = Math.max(2.0F, this.getOwnerToolTier());
      return new BombExplosionEvent(x, y, 140, new GameDamage(200.0F, 1000.0F), true, false, toolTier, this.getOwner());
   }
}
