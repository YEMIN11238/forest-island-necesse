package necesse.entity.projectile.bulletProjectile;

import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;

public class HandGunBulletProjectile extends BulletProjectile {
   public HandGunBulletProjectile() {
   }

   public HandGunBulletProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      super(x, y, targetX, targetY, speed, distance, damage, knockback, owner);
   }
}
