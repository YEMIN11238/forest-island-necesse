package necesse.entity.projectile.bulletProjectile;

import java.awt.Color;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.LevelObjectHit;

public class SharkToothBulletProjectile extends BulletProjectile {
   public SharkToothBulletProjectile() {
   }

   public SharkToothBulletProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      super(x, y, targetX, targetY, speed, distance, damage, knockback, owner);
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (!this.isClient()) {
         if (mob != null) {
            ActiveBuff ab = new ActiveBuff(BuffRegistry.Debuffs.BLEED, mob, 3.0F, this.getOwner());
            mob.addBuff(ab, true);
         }
      }
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), new Color(157, 61, 48), 22.0F, 100, this.getHeight());
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   protected Color getWallHitColor() {
      return new Color(156, 61, 48);
   }
}
