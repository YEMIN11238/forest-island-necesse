package necesse.entity.levelEvent;

import java.awt.Color;
import java.awt.Shape;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.postProcessing.effects.AbstractShockwaveEffect;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.CirclePolygon;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.mobAbilityLevelEvent.GroundEffectEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.LevelObjectHit;

public class TheVoidBlackHoleGroundEvent extends GroundEffectEvent {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   protected MobHitCooldowns hitCooldowns = new MobHitCooldowns();
   protected int tickCounter;
   private long startTime;
   private GameDamage damage;
   private int lifetime = 10000;
   private int beforeActiveTime = 1000;
   private int range = 150;
   protected SoundPlayer windSound;

   public TheVoidBlackHoleGroundEvent() {
   }

   public TheVoidBlackHoleGroundEvent(Mob owner, int x, int y, GameDamage damage, int beforeActiveTime, int lifetime, GameRandom uniqueIDRandom) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
      this.beforeActiveTime = beforeActiveTime;
      this.lifetime = lifetime;
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.beforeActiveTime = reader.getNextInt();
      this.lifetime = reader.getNextInt();
      this.x = reader.getNextInt();
      this.y = reader.getNextInt();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.beforeActiveTime);
      writer.putNextInt(this.lifetime);
      writer.putNextInt(this.x);
      writer.putNextInt(this.y);
   }

   @Override
   public void init() {
      super.init();
      this.startTime = this.getTime();
      this.tickCounter = 0;
      if (this.isClient() && this.lifetime != 0) {
         PostProcessingEffects.addShockwaveEffect(new AbstractShockwaveEffect() {
            @Override
            public int getDrawX(GameCamera camera) {
               return camera.getDrawX(TheVoidBlackHoleGroundEvent.this.x);
            }

            @Override
            public int getDrawY(GameCamera camera) {
               return camera.getDrawY(TheVoidBlackHoleGroundEvent.this.y);
            }

            @Override
            public float getCurrentDistance() {
               return 5.0F;
            }

            @Override
            public float getSize() {
               return TheVoidBlackHoleGroundEvent.this.getBlackHoleSize();
            }

            @Override
            public float getEasingScale() {
               return 1.75F;
            }

            @Override
            public float getEasingPower() {
               return 1.5F;
            }

            @Override
            public boolean shouldRemove() {
               return TheVoidBlackHoleGroundEvent.this.isOver();
            }
         });
      }
   }

   private float getBlackHoleSize() {
      float lifetimePercentage = (float)(this.getTime() - this.startTime) / this.lifetime;
      if (lifetimePercentage < 0.9) {
         return this.range / 2.0F * lifetimePercentage;
      } else {
         return lifetimePercentage > 0.9F ? this.range / 2.0F * ((0.1F - (lifetimePercentage - 0.9F)) * 10.0F) : 50.0F;
      }
   }

   @Override
   public Shape getHitBox() {
      int range = this.range - 10;
      return range <= 0 ? null : new CirclePolygon(this.x, this.y, range, (int)(range * 0.75F), 8);
   }

   @Override
   public void clientHit(Mob target) {
      if (this.beforeActiveTime <= this.getTime() - this.startTime && !((float)(this.getTime() - this.startTime) > this.lifetime * 0.9F)) {
         this.startTargetHitCooldown(target);
         this.hitCooldowns.startCooldown(target);
      }
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (this.beforeActiveTime <= this.getTime() - this.startTime) {
         if (clientSubmitted || this.hitCooldowns.canHit(target)) {
            target.isServerHit(this.damage, 0.0F, 0.0F, 0.0F, this.owner);
            this.hitCooldowns.startCooldown(target);
         }
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(new GameDamage(1.0F), this.owner);
   }

   @Override
   public boolean canHit(Mob mob) {
      return super.canHit(mob) && this.hitCooldowns.canHit(mob);
   }

   @Override
   public void clientTick() {
      if (this.windSound == null || this.windSound.isDone()) {
         this.windSound = SoundManager.playSound(GameResources.wind1, SoundEffect.effect(this.x, this.y).falloffDistance(700).volume(0.0F));
         if (this.windSound != null) {
            this.windSound.fadeIn(2.0F);
            this.windSound.effect.volume(0.4F);
         }
      }

      if (this.windSound != null) {
         this.windSound.refreshLooping(1.0F);
      }

      if (this.owner instanceof TheVoidMob && ((TheVoidMob)this.owner).isInDeathAnimation()) {
         this.over();
      }

      this.tickCounter++;
      this.range++;
      if (this.tickCounter > 20 * (this.lifetime / 1000)) {
         this.over();
      } else {
         super.clientTick();
      }

      this.addTickParticles();
   }

   @Override
   public void serverTick() {
      if (this.owner instanceof TheVoidMob && ((TheVoidMob)this.owner).isInDeathAnimation()) {
         this.over();
      }

      this.tickCounter++;
      this.range++;
      if (this.tickCounter > 20 * (this.lifetime / 1000)) {
         this.over();
      } else {
         super.serverTick();
      }
   }

   private void addTickParticles() {
      GameRandom random = GameRandom.globalRandom;
      AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
      float distance = 65.0F;

      for (int i = 0; i < 5; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameMath.sin(currentAngle.get()) * distance + random.getIntBetween(-5, 5),
               this.y + GameMath.cos(currentAngle.get()) * distance + random.getIntBetween(-5, 5) * 0.85F,
               this.particleTypeSwitcher.next()
            )
            .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
            .height(0.0F)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * 50.0F / 250.0F, Float::sum);
               float distY = distance * lifePercent * 0.85F;
               pos.x = this.x + GameMath.sin(angle) * (distance * lifePercent);
               pos.y = this.y + GameMath.cos(angle) * distY * 0.85F;
            })
            .color((options, lifeTime, timeAlive, lifePercent) -> {
               float clampedLifePercent = Math.max(0.0F, Math.min(1.0F, lifePercent));
               options.color(new Color((int)(0.0F + 255.0F * clampedLifePercent), 0, (int)(0.0F + 231.0F * clampedLifePercent)));
            })
            .ignoreLight(true)
            .lifeTime(1000)
            .sizeFades(50, 24);
      }

      float lifetimePercentage = (float)(this.getTime() - this.startTime) / this.lifetime;
      if (this.beforeActiveTime < this.getTime() - this.startTime && !((float)(this.getTime() - this.startTime) > this.lifetime * 0.9F)) {
         AtomicReference<Float> currentAngleOuterCircle = new AtomicReference<>(random.nextFloat() * 360.0F);
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameMath.sin(currentAngleOuterCircle.get()) * this.range,
               this.y + GameMath.cos(currentAngleOuterCircle.get()) * this.range * 0.75F,
               Particle.GType.CRITICAL
            )
            .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
            .color(new Color(255, 0, 231))
            .ignoreLight(true)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngleOuterCircle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
               pos.x = this.x + GameMath.sin(angle) * this.range * (1.0F - lifePercent);
               pos.y = this.y + GameMath.cos(angle) * this.range * 0.75F * (1.0F - lifePercent);
            })
            .lifeTime(1000)
            .sizeFades(16, 24);

         for (int i = 0; i < 9; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameMath.sin(currentAngleOuterCircle.get()) * this.range,
                  this.y + GameMath.cos(currentAngleOuterCircle.get()) * this.range * 0.75F,
                  Particle.GType.CRITICAL
               )
               .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
               .color(new Color(255, 0, 231))
               .ignoreLight(true)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float angle = currentAngleOuterCircle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
                  pos.x = this.x + GameMath.sin(angle) * this.range;
                  pos.y = this.y + GameMath.cos(angle) * this.range * 0.75F;
               })
               .lifeTime(1000)
               .sizeFades(16, 24);
         }
      }

      if (lifetimePercentage < 0.9F) {
         AtomicReference<Float> currentAngleOuterCircle = new AtomicReference<>(random.nextFloat() * 360.0F);
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameMath.sin(currentAngleOuterCircle.get()) * 55.0F,
               this.y + GameMath.cos(currentAngleOuterCircle.get()) * 55.0F,
               Particle.GType.CRITICAL
            )
            .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
            .color(new Color(255, 0, 231))
            .ignoreLight(true)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngleOuterCircle.accumulateAndGet(delta * 5.0F / 250.0F, Float::sum);
               pos.x = this.x + GameMath.sin(angle) * (this.range / 2.0F * lifetimePercentage) * 1.1F;
               pos.y = this.y + GameMath.cos(angle) * (this.range / 2.0F * lifetimePercentage) * 1.1F;
            })
            .lifeTime(1000)
            .sizeFades(16, 24);
      }
   }
}
