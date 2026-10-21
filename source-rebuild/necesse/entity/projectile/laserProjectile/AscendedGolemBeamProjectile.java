package necesse.entity.projectile.laserProjectile;

import java.awt.Color;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.trails.Trail;
import necesse.level.maps.Level;

public class AscendedGolemBeamProjectile extends LaserProjectile {
   public AscendedGolemBeamProjectile() {
   }

   public AscendedGolemBeamProjectile(Level level, Mob owner, float x, float y, float targetX, float targetY, int distance, GameDamage damage, int knockback) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(10.0F);
      this.givesLight = true;
      this.height = 24.0F;
      this.piercing = 1000;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 3;
   }

   @Override
   public Color getParticleColor() {
      return new Color(255, 0, 255);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(255, 0, 255), 15.0F, 500, 18.0F);
   }
}
