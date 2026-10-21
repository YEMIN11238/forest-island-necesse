package necesse.entity.projectile;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.ParticleOption;
import necesse.entity.particle.fireworks.FireworksExplosion;
import necesse.entity.particle.fireworks.FireworksPath;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class NightPiercerArrowProjectile extends Projectile {
   public static FireworksExplosion piercerPopExplosion = new FireworksExplosion(FireworksExplosion.popPath);

   public NightPiercerArrowProjectile() {
   }

   public NightPiercerArrowProjectile(
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
   }

   @Override
   public void init() {
      super.init();
      this.height = 18.0F;
      this.piercing = 0;
      this.setWidth(6.0F, false);
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      float targetX;
      float targetY;
      if (mob != null) {
         targetX = mob.x;
         targetY = mob.y;
      } else {
         targetX = x;
         targetY = y;
      }

      int range = 70;
      if (!this.isServer()) {
         FireworksExplosion explosion = new FireworksExplosion(FireworksPath.sphere(GameRandom.globalRandom.getIntBetween(range - 10, range)));
         explosion.colorGetter = (particle, progress, random) -> ParticleOption.randomizeColor(310.0F, 0.5F, 0.4F, 20.0F, 0.1F, 0.1F);
         explosion.trailChance = 0.5F;
         explosion.particles = 50;
         explosion.lifetime = 500;
         explosion.popOptions = piercerPopExplosion;
         explosion.particleLightHue = 310.0F;
         explosion.explosionSound = (pos, height, random) -> SoundManager.playSound(
            GameResources.fireworkExplosion, SoundEffect.effect(pos.x, pos.y).pitch(random.getOneOf(0.95F, 1.0F, 1.05F)).volume(0.6F).falloffDistance(1500)
         );
         explosion.spawnExplosion(this.getLevel(), targetX, targetY, this.getHeight(), GameRandom.globalRandom);
      }

      if (!this.isClient()) {
         Rectangle targetBox = new Rectangle((int)targetX - range, (int)targetY - range, range * 2, range * 2);
         this.streamTargets(this.getOwner(), targetBox)
            .filter(m -> this.canHit(m) && m.getDistance(targetX, targetY) <= range)
            .forEach(m -> m.isServerHit(this.getDamage(), m.x - x, m.y - y, this.knockback, this));
      }
   }

   @Override
   public void applyDamage(Mob mob, float x, float y) {
   }

   @Override
   public Color getParticleColor() {
      return new Color(108, 37, 92);
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(73, 27, 78), 12.0F, 500, this.getHeight());
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

   static {
      piercerPopExplosion.particles = 1;
      piercerPopExplosion.lifetime = 200;
      piercerPopExplosion.minSize = 6;
      piercerPopExplosion.maxSize = 10;
      piercerPopExplosion.trailChance = 0.0F;
      piercerPopExplosion.popChance = 0.0F;
      piercerPopExplosion.colorGetter = (particle, progress, random) -> ParticleOption.randomizeColor(310.0F, 0.8F, 0.7F, 20.0F, 0.1F, 0.1F);
      piercerPopExplosion.explosionSound = null;
   }
}
