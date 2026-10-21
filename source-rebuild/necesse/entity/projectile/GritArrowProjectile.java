package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class GritArrowProjectile extends Projectile {
   public GritArrowProjectile() {
   }

   public GritArrowProjectile(Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback) {
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.speed = speed;
      this.setDistance(distance);
   }

   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.piercing = 1;
      this.trailOffset = -10.0F;
      this.setWidth(14.0F);
   }

   @Override
   public Color getParticleColor() {
      return ParticleOption.randomizeColor(20.0F, 0.5F, 0.44F, 0.0F, 0.0F, 0.1F);
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 2;
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      particle.lifeTime(1000);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(184, 102, 40), 28.0F, 500, this.getHeight());
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
         int drawY = camera.getDrawY(this.y);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 2, 0)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }
}
