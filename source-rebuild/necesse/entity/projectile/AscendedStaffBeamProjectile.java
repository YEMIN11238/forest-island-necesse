package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.followingProjectile.FollowingProjectile;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AscendedStaffBeamProjectile extends FollowingProjectile {
   private float particleBuffer;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   private SoundPlayer sound;
   protected float fullSpeed = 0.0F;

   public AscendedStaffBeamProjectile() {
   }

   public AscendedStaffBeamProjectile(
      float startX, float startY, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner
   ) {
      this();
      this.setLevel(owner.getLevel());
      this.x = startX;
      this.y = startY;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.fullSpeed = speed;
      this.setDistance(distance);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.fullSpeed);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.fullSpeed = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.setWidth(60.0F, true);
      this.turnSpeed = 50.0F;
      this.particleSpeedMod = 0.03F;
      this.piercing = 100;
      this.isSolid = false;
      this.stopsAtTarget = true;
      this.givesLight = true;
      this.lightSaturation = 171.0F;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickDebrisProjectiles();
      this.globalTick();
   }

   private void tickDebrisProjectiles() {
      Mob owner = this.getOwner();
      if (owner == null || !owner.buffManager.hasBuff(BuffRegistry.Debuffs.MANA_EXHAUSTION) || this.getLevel().tickManager().getTotalTicks() % 4L == 0L) {
         Projectile projectile = new AscendedBoltProjectile(
            this.getLevel(),
            this.x,
            this.y + 16.0F,
            GameRandom.globalRandom.getFloatBetween(0.0F, 360.0F),
            300.0F,
            100,
            this.getDamage().modFinalMultiplier(0.1F),
            this.getOwner()
         );
         projectile.getUniqueID(GameRandom.globalRandom);
         this.getLevel().entityManager.projectiles.add(projectile);
      }
   }

   protected void globalTick() {
      double distFromTarget;
      if (this.targetPos != null) {
         distFromTarget = this.targetPos.distance(new java.awt.geom.Point2D.Float(this.x, this.y));
      } else if (this.target != null) {
         distFromTarget = new java.awt.geom.Point2D.Float(this.x, this.y).distance(new java.awt.geom.Point2D.Float(this.target.x, this.target.y));
      } else {
         distFromTarget = new java.awt.geom.Point2D.Float(this.x, this.y).distance(new java.awt.geom.Point2D.Float(this.targetX, this.targetY));
      }

      float newSpeed = (float)GameMath.limit(distFromTarget * 1.2F, 0.0, this.fullSpeed);
      if (this.speed != newSpeed) {
         this.speed = newSpeed;
      }

      Mob owner = this.getOwner();
      if (owner != null && owner.buffManager.hasBuff(BuffRegistry.Debuffs.MANA_EXHAUSTION)) {
         this.hitCooldowns.hitCooldown = 1000;
      } else {
         this.hitCooldowns.hitCooldown = 250;
      }
   }

   @Override
   public float getOriginalSpeed() {
      return this.fullSpeed;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.sound == null || this.sound.isDone()) {
         this.sound = SoundManager.playSound(GameResources.laserBeam1, SoundEffect.effect(this).falloffDistance(1000).volume(0.0F));
         if (this.sound != null) {
            this.sound.fadeIn(1.0F);
            this.sound.effect.volume(0.5F);
         }
      }

      if (this.sound != null) {
         this.sound.refreshLooping(1.0F);
      }

      this.globalTick();
      this.getLevel()
         .entityManager
         .addParticle(this, GameRandom.globalRandom.getIntBetween(-20, 20), 0.0F, Particle.GType.COSMETIC)
         .sprite(GameResources.ascendedParticle.sprite(0, 0, 20))
         .ignoreLight(true)
         .sizeFades(20, 40)
         .movesConstant(GameRandom.globalRandom.getIntBetween(-20, 20), GameRandom.globalRandom.getIntBetween(-20, -40))
         .height(16.0F)
         .lifeTime(500);
      float particlesPerSecond = 80.0F;
      this.particleBuffer += particlesPerSecond / 20.0F;

      while (this.particleBuffer >= 1.0F) {
         this.particleBuffer--;
         GameRandom random = GameRandom.globalRandom;
         AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
         float distance = 75.0F;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameMath.sin(currentAngle.get()) * distance + random.getIntBetween(-5, 5),
               this.y + GameMath.cos(currentAngle.get()) * distance + random.getIntBetween(-5, 5) * 0.85F,
               this.particleTypeSwitcher.next()
            )
            .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
            .height(16.0F)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
               float distY = distance * lifePercent * 0.85F;
               pos.x = this.x + GameMath.sin(angle) * (distance * lifePercent);
               pos.y = this.y + GameMath.cos(angle) * distY * 0.85F;
            })
            .color((options, lifeTime, timeAlive, lifePercent) -> {
               float clampedLifePercent = Math.max(0.0F, Math.min(1.0F, lifePercent));
               options.color(new Color(255, (int)(255.0F - 255.0F * clampedLifePercent), (int)(255.0F - 24.0F * clampedLifePercent)));
            })
            .ignoreLight(true)
            .lifeTime(1000)
            .sizeFades(50, 24);
      }
   }

   @Override
   public void onMaxMoveTick() {
      if (this.isClient()) {
         this.spawnSpinningParticle();
      }
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
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2 + 16;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight();
         int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 2, 200);
         final TextureDrawOptions options = this.texture.initDraw().sprite(anim, 0, 32, 640).light(light.minLevelCopy(150.0F)).pos(drawX, drawY);
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture
            .initDraw()
            .light(light.minLevelCopy(150.0F))
            .pos(drawX - this.shadowTexture.getWidth() / 2 + 16, drawY + this.texture.getHeight() - this.shadowTexture.getHeight() / 2 - 16);
         tileList.add(tm -> shadowOptions.draw());
      }
   }
}
