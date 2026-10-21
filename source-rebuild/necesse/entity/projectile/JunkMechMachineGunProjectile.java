package necesse.entity.projectile;

import java.awt.Color;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.bulletProjectile.BulletProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;

public class JunkMechMachineGunProjectile extends BulletProjectile implements RicochetableProjectile {
   public JunkMechMachineGunProjectile() {
      this.height = 15.0F;
   }

   public JunkMechMachineGunProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this();
      this.setLevel(owner.getLevel());
      this.applyData(x, y, targetX, targetY, speed, distance, damage, knockback, owner);
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), new Color(227, 120, 64), 22.0F, 50, this.getHeight());
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   protected Color getWallHitColor() {
      return new Color(227, 120, 64);
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 35.0F, this.lightSaturation);
   }
}
