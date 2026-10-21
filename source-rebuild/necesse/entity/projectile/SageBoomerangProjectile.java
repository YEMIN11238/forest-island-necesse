package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.ParticleOption;
import necesse.entity.projectile.boomerangProjectile.BoomerangProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SageBoomerangProjectile extends BoomerangProjectile {
   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.setWidth(14.0F);
      this.bouncing = 8;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return super.getExtraSpinningParticles() + 2;
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      particle.givesLight(200.0F, 0.7F);
      particle.lifeTime(1000);
   }

   @Override
   public Color getParticleColor() {
      return ParticleOption.randomizeColor(200.0F, 0.5F, 0.44F, 0.0F, 0.0F, 0.1F);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(65, 105, 151), 25.0F, 700, 18.0F);
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
         GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         float angle = this.getAngle();
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(angle, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, angle, this.shadowTexture.getHeight() / 2);
      }
   }

   @Override
   public float getAngle() {
      return super.getAngle() * 1.5F;
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.dragonsReboundBlue).volume(0.1F);
   }
}
