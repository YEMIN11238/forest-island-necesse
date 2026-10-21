package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.VoidWizard;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class VoidWizardWaveProjectile extends Projectile {
   public VoidWizardWaveProjectile() {
   }

   public VoidWizardWaveProjectile(Mob owner, int x, int y, int targetX, int targetY, GameDamage damage) {
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.speed = 100.0F;
      this.setDistance(1500);
   }

   @Override
   public void init() {
      super.init();
      this.piercing = 9999;
      this.setWidth(45.0F, true);
      this.isSolid = true;
      this.givesLight = true;
      this.particleRandomOffset = 8.0F;
   }

   @Override
   protected CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().addFilter(tp -> tp.object().object.isWall || tp.object().object.isRock);
   }

   @Override
   public Color getParticleColor() {
      return VoidWizard.getWizardProjectileColor(this.getOwner());
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light.minLevelCopy(Math.min(light.getLevel() + 100.0F, 150.0F)))
            .rotate(this.getAngle() - 135.0F, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY);
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }
}
