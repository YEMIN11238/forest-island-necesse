package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MoonlightJavelinProjectile extends Projectile {
   public MoonlightJavelinProjectile() {
   }

   public MoonlightJavelinProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
      this.heightBasedOnDistance = true;
   }

   @Override
   public void init() {
      super.init();
      this.piercing = 3;
      this.height = 16.0F;
      this.width = 6.0F;
      this.trailOffset = -5.0F;
   }

   @Override
   public Color getParticleColor() {
      return new Color(200, 212, 255);
   }

   @Override
   protected void modifySpinningParticle(ParticleOption particle) {
      particle.sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 22))
         .sizeFades(44, 22)
         .ignoreLight(true)
         .givesLight(247.0F, 0.65F)
         .lifeTime(600);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(200, 212, 255), 15.0F, 400, this.getHeight());
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
         int drawX = camera.getDrawX(this.x) - 16;
         int drawY = camera.getDrawY(this.y) - 32;
         int spriteX = GameUtils.getAnim(this.getWorldEntity().getTime(), 4, 400);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(spriteX, 0, 32, 64)
            .light(light.minLevelCopy(150.0F))
            .rotate(this.getAngle(), 16, 32)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture
            .initDraw()
            .sprite(spriteX, 0, 32, 64)
            .light(light.minLevelCopy(50.0F))
            .alpha(0.33F)
            .rotate(this.getAngle(), 16, 32)
            .pos(drawX, drawY);
         tileList.add(tm -> shadowOptions.draw());
      }
   }

   @Override
   public void playHitSound(float x, float y) {
      SoundManager.playSound(GameResources.electricExplosion, SoundEffect.effect(this).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F)));
   }
}
