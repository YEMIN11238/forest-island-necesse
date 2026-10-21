package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class NightRazorBoomerangProjectile extends FollowingProjectile {
   public NightRazorBoomerangProjectile() {
      this.isBoomerang = true;
   }

   @Override
   public void init() {
      super.init();
      if (this.getOwner() == null) {
         this.remove();
      }

      this.returningToOwner = false;
      this.spawnTime = this.getWorldEntity().getTime();
      this.trailOffset = 0.0F;
      this.setWidth(16.0F, true);
      this.height = 18.0F;
      this.bouncing = 10;
      this.turnSpeed = 0.5F;
      this.givesLight = true;
      this.lightSaturation = 1.0F;
   }

   @Override
   protected SoundSettings getMoveSound() {
      return new SoundSettings(GameResources.swing2).volume(0.6F);
   }

   @Override
   public Color getParticleColor() {
      return new Color(108, 37, 92);
   }

   @Override
   protected int getExtraSpinningParticles() {
      return 1;
   }

   @Override
   protected void spawnDeathParticles() {
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(108, 37, 92), 30.0F, 400, this.height);
   }

   @Override
   protected Color getWallHitColor() {
      return new Color(108, 37, 92);
   }

   @Override
   public void updateTarget() {
      if (this.traveledDistance > 40.0F) {
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
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         float angle = (float)(this.getWorldEntity().getTime() - this.spawnTime);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light.minLevelCopy(100.0F))
            .rotate(angle * 1.5F, this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), this.shadowTexture.getHeight() / 2);
      }
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.nightRazor).volume(0.4F);
   }
}
