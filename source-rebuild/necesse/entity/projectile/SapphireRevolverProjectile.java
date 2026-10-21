package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ProjectileHitStuckParticle;
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

public class SapphireRevolverProjectile extends Projectile {
   protected int spriteX;

   public SapphireRevolverProjectile() {
   }

   public SapphireRevolverProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this();
      this.setLevel(owner.getLevel());
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
      this.spriteX = GameRandom.globalRandom.getIntBetween(0, 2);
   }

   @Override
   public void init() {
      super.init();
      this.particleSpeedMod = 0.03F;
      this.piercing = 2;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.spriteX);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.spriteX = reader.getNextInt();
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (mob != null) {
         if (this.amountHit == this.getTotalPiercing()) {
            this.attachTextureToTarget(mob, x, y);
         }
      } else if (this.bounced == this.getTotalBouncing()) {
         this.attachTextureToTarget(null, x, y);
      }
   }

   private void attachTextureToTarget(Mob mob, float x, float y) {
      this.getLevel()
         .entityManager
         .addParticle(
            new ProjectileHitStuckParticle(mob, this, x, y, GameRandom.globalRandom.getIntBetween(10, 20), 5000L) {
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
                  int drawX = camera.getDrawX(xx) - 2;
                  int drawY = camera.getDrawY(yx - SapphireRevolverProjectile.this.height) - 2;
                  float alpha = 1.0F;
                  long lifeCycleTime = this.getLifeCycleTime();
                  int fadeTime = 1000;
                  if (lifeCycleTime >= this.lifeTime - fadeTime) {
                     alpha = Math.abs((float)(lifeCycleTime - (this.lifeTime - fadeTime)) / fadeTime - 1.0F);
                  }

                  final TextureDrawOptions options = SapphireRevolverProjectile.this.texture
                     .initDraw()
                     .sprite(SapphireRevolverProjectile.this.spriteX, 0, 18, 32)
                     .light(light)
                     .rotate(SapphireRevolverProjectile.this.getAngle(), 8, 0)
                     .pos(drawX, drawY)
                     .alpha(alpha);
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

   @Override
   public void onMaxMoveTick() {
      if (this.isClient()) {
         this.spawnSpinningParticle();
      }
   }

   @Override
   public float tickMovement(float delta) {
      if (!this.isServer()) {
         Color color = ThemeColorRegistry.SAPPHIRE.getRandomColor();
         float dxParticle = this.dy * (this.distance / this.traveledDistance) + this.dx * 10.0F;
         float dyParticle = this.dx * (this.distance / this.traveledDistance) + this.dy * 10.0F;
         float var10002 = this.y - this.height;
         this.getLevel()
            .entityManager
            .addParticle(this.x, var10002, Particle.GType.IMPORTANT_COSMETIC)
            .lifeTime(1000)
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
            .sizeFades(22, 11)
            .color(color)
            .givesLight(198.0F, 0.3F)
            .movesFriction(dxParticle, -dyParticle, 0.8F);
         var10002 = this.y - this.height;
         this.getLevel()
            .entityManager
            .addParticle(this.x, var10002, Particle.GType.IMPORTANT_COSMETIC)
            .lifeTime(1000)
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
            .sizeFades(22, 11)
            .color(color)
            .givesLight(198.0F, 0.3F)
            .movesFriction(-dxParticle, dyParticle, 0.8F);
      }

      return super.tickMovement(delta);
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), ThemeColorRegistry.SAPPHIRE.getRandomColor(), 22.0F, 1000, this.height);
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   protected Color getWallHitColor() {
      return ThemeColorRegistry.SAPPHIRE.getRandomColor();
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, this.getWallHitColor(), this.lightSaturation);
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
         int drawY = camera.getDrawY(this.y) - 16;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(this.spriteX, 0, 18, 32)
            .light(light)
            .rotate(this.getAngle(), 8, 18)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), 18);
      }
   }
}
