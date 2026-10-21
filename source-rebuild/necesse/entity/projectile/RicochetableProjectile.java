package necesse.entity.projectile;

import necesse.engine.registries.ProjectileRegistry;
import necesse.entity.mobs.Mob;

public interface RicochetableProjectile {
   default Projectile getRicochetProjectile(float startX, float startY, float targetX, float targetY, Mob potentialTarget) {
      Projectile projectile = (Projectile)this;
      if (this.shouldRicochetPredictTarget() && potentialTarget != null) {
         projectile.setTargetPrediction(potentialTarget, -20.0F);
         projectile.moveDist(20.0);
      }

      int piercingLeft = projectile.getTotalPiercing() - projectile.amountHit();
      return !this.shouldRicochetIfPiercing() && piercingLeft > 1
         ? null
         : ProjectileRegistry.getProjectile(
            projectile.getID(),
            potentialTarget.getLevel(),
            startX,
            startY,
            targetX,
            targetY,
            projectile.speed,
            projectile.distance,
            projectile.getDamage(),
            projectile.knockback,
            projectile.getOwner()
         );
   }

   default boolean shouldRicochetIfPiercing() {
      return false;
   }

   default boolean shouldRicochetPredictTarget() {
      return true;
   }
}
