package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.explosionEvent.PatchworkGrenadeExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRegistry;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class PatchworkGrenadeProjectile extends Projectile {
   public PatchworkGrenadeProjectile() {
   }

   public PatchworkGrenadeProjectile(Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.speed = speed;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.setDistance(distance);
      this.setOwner(owner);
   }

   public PatchworkGrenadeProjectile(Level level, Mob owner, float targetX, float targetY, float speed, int distance, GameDamage damage) {
      this(level, owner, owner.x, owner.y, targetX, targetY, speed, distance, damage);
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getWorldEntity().getTime();
      this.isSolid = false;
      this.canHitMobs = false;
      this.trailOffset = 0.0F;
      this.canBreakObjects = false;
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      float travelPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      float travelPercInv = Math.abs(travelPerc - 1.0F);
      float heightF = GameMath.sin(travelPerc * 180.0F);
      this.height = (int)(heightF * this.distance / 5.0F + 25.0F * travelPercInv);
      return out;
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), ThemeColorRegistry.RED.getRandomColor(), 22.0F, 100, this.height);
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isServer()) {
         this.triggerExplosion();
      }
   }

   public void triggerExplosion() {
      ResilienceOnHitProjectileModifier resilienceGain = null;
      if (this.modifier instanceof ResilienceOnHitProjectileModifier) {
         resilienceGain = (ResilienceOnHitProjectileModifier)this.modifier;
      }

      PatchworkGrenadeExplosionEvent e = new PatchworkGrenadeExplosionEvent(this.x, this.y, 100, this.getDamage(), this.getAttackOwner(), resilienceGain);
      this.getLevel().entityManager.events.add(e);
   }

   @Override
   protected void spawnDeathParticles() {
      Color particleColor = this.getParticleColor();
      if (particleColor != null) {
         for (int i = 0; i < 10; i++) {
            int angle = GameRandom.globalRandom.nextInt(360);
            Float dir = GameMath.getAngleDir(angle);
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, Particle.GType.CRITICAL)
               .movesConstant(GameRandom.globalRandom.getIntBetween(20, 50) * dir.x, GameRandom.globalRandom.getIntBetween(20, 50) * dir.y)
               .color(this.getParticleColor())
               .height(this.getHeight());
         }
      }

      java.lang.Float pitch = GameRandom.globalRandom.getOneOf(0.9F, 0.95F, 1.0F);
      SoundManager.playSound(GameResources.blunthit, SoundEffect.effect(this.x, this.y).volume(0.7F).pitch(pitch).falloffDistance(1400));
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
         int drawX = camera.getDrawX(this.x) - 8;
         int drawY = camera.getDrawY(this.y) - 18;
         int anim = GameUtils.getAnim(this.getTime(), 3, 300);
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(anim, 0, 24, 34)
            .light(light)
            .rotate((float)this.getTime(), 12, 17)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, (float)this.getTime(), 18);
      }
   }

   @Override
   protected SoundSettings getSpawnSound() {
      return new SoundSettings(GameResources.spit).volume(0.9F).fallOffDistance(1200);
   }
}
