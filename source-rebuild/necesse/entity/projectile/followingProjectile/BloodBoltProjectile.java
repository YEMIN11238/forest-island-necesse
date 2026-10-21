package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BloodBoltProjectile extends FollowingProjectile {
   @Override
   public void init() {
      super.init();
      this.turnSpeed = 0.5F;
      this.givesLight = true;
      this.height = 18.0F;
      this.piercing = 0;
      this.bouncing = 2;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 1;
   }

   @Override
   public Color getParticleColor() {
      return new Color(127, 0, 0);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(127, 0, 0), 6.0F, 500, 18.0F);
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 100.0F) {
         this.findTarget(m -> m.isHostile, 160.0F, 160.0F);
      }
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
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), 0);
      }
   }
}
