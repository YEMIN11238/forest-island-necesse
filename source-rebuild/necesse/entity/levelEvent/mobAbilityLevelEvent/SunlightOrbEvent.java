package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SameNearSoundCooldown;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class SunlightOrbEvent extends GroundEffectEvent {
   private long startDelay = 1000L;
   private MobHitCooldowns hitCooldowns;
   private GameDamage damage = new GameDamage(90.0F);
   private float resilienceGain;
   private int tickCounter;
   private long startTime;
   private boolean isActive;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public SunlightOrbEvent() {
   }

   public SunlightOrbEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, GameDamage damage, long startDelay) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
      this.startDelay = startDelay;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.startDelay);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startDelay = reader.getNextLong();
   }

   @Override
   public void init() {
      super.init();
      this.hitCooldowns = new MobHitCooldowns();
      this.startTime = this.level.getWorldEntity().getTime();
      this.isActive = false;

      for (int i = 0; i < 20; i++) {
         int currentAngle = (int)(18.0F * i);
         float distance = 100.0F;
         if (this.isClient()) {
            this.getLevel()
               .entityManager
               .addParticle(this.x + GameMath.sin(currentAngle) * distance, this.y + GameMath.cos(currentAngle) * distance, Particle.GType.CRITICAL)
               .color(new Color(249, 155, 78))
               .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
               .height(0.0F)
               .movesFriction(-GameMath.sin(currentAngle) * 100.0F, -GameMath.cos(currentAngle) * 100.0F, 0.8F)
               .lifeTime((int)this.startDelay + 250)
               .sizeFades(50, 50);
         }
      }
   }

   @Override
   public Shape getHitBox() {
      int width = 180;
      int height = 136;
      return new Rectangle(this.x - width / 2, this.y - height / 2, width, height);
   }

   @Override
   public void clientHit(Mob target) {
      this.startTargetHitCooldown(target);
      this.hitCooldowns.startCooldown(target);
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
      if (clientSubmitted || this.hitCooldowns.canHit(target)) {
         target.isServerHit(this.damage, 0.0F, 0.0F, 0.0F, this.owner);
         this.hitCooldowns.startCooldown(target);
         if (target.canGiveResilience(this.owner) && this.resilienceGain != 0.0F) {
            this.owner.addResilience(this.resilienceGain);
            this.resilienceGain = 0.0F;
         }
      }
   }

   @Override
   public void clientTick() {
      this.tickCounter++;
      if (this.tickCounter > 200) {
         this.over();
      } else {
         super.clientTick();
      }

      if (this.level.getWorldEntity().getTime() - this.startTime > this.startDelay) {
         if (!this.isActive) {
            SoundManager.playSound(
               GameResources.firespell1, SoundEffect.effect(this.x, this.y).volume(1.0F).pitch(1.0F).falloffDistance(1000), new SameNearSoundCooldown(100, 200)
            );
            this.isActive = true;
         }

         for (int i = 0; i < 4; i++) {
            float var10001 = this.x;
            this.getLevel()
               .entityManager
               .addTopParticle(var10001, this.y, this.particleTypeSwitcher.next())
               .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
               .sizeFades(30, 40)
               .rotates()
               .movesFrictionAngle(this.tickCounter * 10 + i * 90, 85.0F, 0.8F)
               .color(
                  (options, lifeTime1, timeAlive, lifePercent) -> {
                     float clampedLifePercent = Math.max(0.0F, Math.min(1.0F, lifePercent));
                     options.color(
                        new Color(
                           (int)(255.0F - 55.0F * clampedLifePercent), (int)(225.0F - 200.0F * clampedLifePercent), (int)(155.0F - 125.0F * clampedLifePercent)
                        )
                     );
                  }
               )
               .givesLight(50.0F, 1.0F)
               .fadesAlphaTime(100, 50)
               .lifeTime(1000);
         }

         for (int i = 0; i < 4; i++) {
            float var3 = this.x;
            this.getLevel()
               .entityManager
               .addTopParticle(var3, this.y, this.particleTypeSwitcher.next())
               .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
               .sizeFades(20, 30)
               .rotates()
               .movesFrictionAngle(this.tickCounter * 15 - i * 90, 35.0F, 0.8F)
               .color(new Color(255, 233, 73))
               .givesLight(50.0F, 1.0F)
               .fadesAlphaTime(100, 50)
               .lifeTime(500);
         }
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 200) {
         this.over();
      } else {
         super.serverTick();
      }
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(this.damage, this.owner);
   }

   @Override
   public boolean canHit(Mob mob) {
      return !this.canDamageAnythingYet() ? false : super.canHit(mob) && this.hitCooldowns.canHit(mob);
   }

   @Override
   public boolean canHit(LevelObjectHit hit) {
      return !this.canDamageAnythingYet() ? false : super.canHit(hit);
   }

   public boolean canDamageAnythingYet() {
      return this.level.getWorldEntity().getTime() >= this.startTime + this.startDelay + 100L;
   }

   @Override
   public void over() {
      super.over();
   }
}
