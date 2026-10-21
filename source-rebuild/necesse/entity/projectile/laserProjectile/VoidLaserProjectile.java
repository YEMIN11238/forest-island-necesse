package necesse.entity.projectile.laserProjectile;

import java.awt.Color;
import necesse.entity.trails.Trail;

public class VoidLaserProjectile extends LaserProjectile {
   @Override
   public void init() {
      super.init();
      this.givesLight = true;
      this.height = 18.0F;
      this.bouncing = 1000;
      this.piercing = 1000;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 3;
   }

   @Override
   public Color getParticleColor() {
      return new Color(75, 0, 25);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(75, 0, 25), 12.0F, 500, 18.0F);
   }
}
