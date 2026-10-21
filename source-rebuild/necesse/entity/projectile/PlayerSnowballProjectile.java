package necesse.entity.projectile;

import java.awt.Color;
import java.awt.Shape;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ProjectileHitStuckParticle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class PlayerSnowballProjectile extends Projectile {
   private int sprite;

   public PlayerSnowballProjectile() {
   }

   public PlayerSnowballProjectile(float x, float y, float targetX, float targetY, GameDamage damage, Mob owner) {
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = 100.0F;
      this.setDamage(damage);
      this.setOwner(owner);
      this.setDistance(400);
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(10.0F);
      this.height = 18.0F;
      this.spawnTime = this.getWorldEntity().getTime();
      this.heightBasedOnDistance = true;
      this.trailOffset = 0.0F;
      if (this.texture != null) {
         this.sprite = new GameRandom(this.getUniqueID()).nextInt(this.texture.getWidth() / 32);
      }
   }

   @Override
   protected Stream<Mob> streamTargets(Mob owner, Shape hitBounds) {
      return Stream.concat(
            this.getLevel().entityManager.mobs.streamInRegionsShape(hitBounds, 1),
            GameUtils.streamNetworkClients(this.getLevel()).filter(c -> !c.isDead() && c.hasSpawned()).map(sc -> sc.playerMob)
         )
         .filter(mob -> mob != owner);
   }

   @Override
   public boolean canHit(Mob mob) {
      if (!mob.canTakeDamage()) {
         return false;
      }

      Mob mounted = mob.getRider();
      return mounted == null || mounted.canTakeDamage();
   }

   @Override
   public void applyDamage(Mob mob, float x, float y) {
      if (mob.isPlayer) {
         mob.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SNOW_COVERED_DEBUFF, mob, 8000, null), true);
      } else {
         mob.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SNOW_COVERED_SLOW_DEBUFF, mob, 8000, null), true);
      }
   }

   @Override
   public void startHitCooldown(Mob target) {
   }

   @Override
   public boolean isTargetOnHitCooldown(Mob target) {
      return false;
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (this.isClient() && this.traveledDistance < this.distance) {
         this.getLevel()
            .entityManager
            .addParticle(
               new ProjectileHitStuckParticle(mob, this, x, y, mob == null ? 2.0F : 6.0F, 8000L) {
                  @Override
                  public void addDrawables(
                     Mob target,
                     float xx,
                     float yx,
                     float angle,
                     List<LevelSortedDrawable> list,
                     OrderableDrawables tileList,
                     OrderableDrawables topList,
                     Level level,
                     TickManager tickManager,
                     GameCamera camera,
                     PlayerMob perspective
                  ) {
                     GameLight light = level.getLightLevel(this);
                     float alpha = 1.0F;
                     long lifeCycleTime = this.getLifeCycleTime();
                     int fadeTime = 500;
                     if (lifeCycleTime >= this.lifeTime - fadeTime) {
                        alpha = Math.abs((float)(lifeCycleTime - (this.lifeTime - fadeTime)) / fadeTime - 1.0F);
                     }

                     int textureRes = 32;
                     int halfTextureRes = textureRes / 2;
                     int drawX = camera.getDrawX(xx) - halfTextureRes;
                     int drawY = camera.getDrawY(yx) - halfTextureRes;
                     GameTextureSection cutTexture = new GameTextureSection(GameResources.cutSnowballParticles)
                        .sprite(PlayerSnowballProjectile.this.sprite, 0, textureRes);
                     final TextureDrawOptions options = cutTexture.initDraw()
                        .alpha(alpha)
                        .light(light)
                        .rotate(PlayerSnowballProjectile.this.getAngle() - 90.0F)
                        .pos(drawX, drawY - (int)PlayerSnowballProjectile.this.getHeight());
                     EntityDrawable drawable = new EntityDrawable(this) {
                        @Override
                        public void draw(TickManager tickManager) {
                           options.draw();
                        }
                     };
                     if (target != null) {
                        topList.add(drawable);
                     } else {
                        list.add(drawable);
                     }
                  }
               },
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(227, 241, 240), 12.0F, 150, 18.0F);
   }

   @Override
   public Color getParticleColor() {
      return new Color(227, 241, 240);
   }

   @Override
   public float getParticleChance() {
      return super.getParticleChance() * 0.5F;
   }

   @Override
   protected int getExtraSpinningParticles() {
      return 0;
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
         int textureRes = 32;
         int halfTextureRes = textureRes / 2;
         int drawX = camera.getDrawX(this.x) - halfTextureRes;
         int drawY = camera.getDrawY(this.y) - halfTextureRes;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(this.sprite, 0, textureRes)
            .light(light)
            .rotate(this.rotateBasedOnTime())
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture
            .initDraw()
            .sprite(this.sprite, 0, textureRes)
            .light(light)
            .rotate(this.getAngle(), halfTextureRes, halfTextureRes)
            .pos(drawX, drawY);
         tileList.add(tm -> shadowOptions.draw());
      }
   }

   public float rotateBasedOnTime() {
      return (float)(this.getWorldEntity().getTime() - this.spawnTime);
   }

   @Override
   protected void playHitSound(float x, float y) {
      SoundManager.playSound(GameResources.snowBallHit, SoundEffect.effect(x, y).volume(0.1F).pitch(GameRandom.globalRandom.getFloatBetween(0.95F, 1.05F)));
   }
}
