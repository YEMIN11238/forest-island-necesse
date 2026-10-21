package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRegistry;
import necesse.level.maps.LevelObjectHit;

public class CryoStormLevelEvent extends GroundEffectEvent {
   private final float chargeDurationSeconds = 2.0F;
   private final float stormDurationSeconds = 2.5F;
   private final float falloutDurationSeconds = 3.0F;
   private final float secondsDuration = 7.5F;
   private int stormPhase = 0;
   private final int stormWidth = 1024;
   private MobHitCooldowns hitCooldowns;
   private int tickCounter;
   private GameDamage damage;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   private final GameRandom random = GameRandom.globalRandom;

   public CryoStormLevelEvent() {
   }

   public CryoStormLevelEvent(Mob owner, int x, int y, GameRandom uniqueIDRandom, GameDamage damage) {
      super(owner, x, y, uniqueIDRandom);
      this.damage = damage;
   }

   @Override
   public Shape getHitBox() {
      int height = 768;
      return new Rectangle(this.x - 512, this.y - height / 2, 1024, height);
   }

   @Override
   public void init() {
      super.init();
      this.hitCooldowns = new MobHitCooldowns(500);
      this.startChargingPhase();
   }

   protected void hitTargets(int range, boolean freeze) {
      if (!this.isClient()) {
         int rangeY = (int)(range * 0.75F);
         Rectangle rectangle = new Rectangle(this.x - range, this.y - range, range * 2, rangeY * 2);
         GameUtils.streamTargets(this.owner, rectangle).filter(this::canHit).filter(m -> {
            float dx = m.x - this.x;
            float dy = (m.y - this.y) / 0.75F;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);
            return distance <= range;
         }).forEach(m -> this.serverApplyHitBuffs(m, freeze));
      }
   }

   @Override
   public boolean canHit(Mob mob) {
      return super.canHit(mob) && !mob.isBoss() && this.hitCooldowns.canHit(mob);
   }

   public void serverApplyHitBuffs(Mob target, boolean freeze) {
      this.startTargetHitCooldown(target);
      this.hitCooldowns.startCooldown(target);
      if (!freeze) {
         target.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.GENERIC_ICE_SLOW, target, 2.8F, null), true);
      } else {
         ActiveBuff freezeBuff = new ActiveBuff(BuffRegistry.FROZEN_MOB, target, 3.0F, null);
         target.buffManager.addBuff(freezeBuff, true);
      }
   }

   protected void runWaveHit(int ticksWhenStarting, boolean freeze) {
      if (this.tickCounter == ticksWhenStarting) {
         this.hitTargets(128, freeze);
      }

      if (this.tickCounter == ticksWhenStarting + 4) {
         this.hitTargets(204, freeze);
      }

      if (this.tickCounter == ticksWhenStarting + 7) {
         this.hitTargets(341, freeze);
      }

      if (this.tickCounter == ticksWhenStarting + 10) {
         this.hitTargets(512, freeze);
      }
   }

   @Override
   public void clientHit(Mob target) {
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
   }

   @Override
   public void serverTick() {
      this.tick();
   }

   @Override
   public void clientTick() {
      if (this.isClient()) {
         this.tick();
      }
   }

   protected void tick() {
      this.tickCounter++;
      if (this.tickCounter > 150.0F) {
         this.over();
      }

      if (this.stormPhase == 0) {
         this.runChargingPhase();
         if (this.tickCounter > 40.0F) {
            this.stormPhase = 1;
            this.startStormPhase();
         }
      }

      if (this.stormPhase == 1) {
         this.runStormPhase();
         if (this.tickCounter >= 90.0F) {
            this.stormPhase = 2;
            this.endStormPhase();
         }
      }

      if (this.stormPhase == 2) {
         this.runFalloutPhase();
      }
   }

   protected void startChargingPhase() {
      this.stormPhase = 0;
      this.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.CRYO_IMMUNTY, this.owner, 4.5F, null), false);
   }

   protected void runChargingPhase() {
      if (this.isClient()) {
         this.getLevel()
            .entityManager
            .addParticle(this.x + this.random.getFloatBetween(-5.0F, 5.0F), this.y + this.random.getFloatBetween(-5.0F, 5.0F), this.particleTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(this.random.getIntBetween(0, 4), 0, 12))
            .height(0.0F)
            .movesConstant(this.random.getFloatBetween(-22.0F, 22.0F), this.random.getFloatBetween(-32.0F, -52.0F))
            .color(ThemeColorRegistry.ICE.getRandomColor())
            .lifeTime(500)
            .sizeFades(50, 24);
      }
   }

   public java.awt.geom.Point2D.Float randomPointInUnitCircle() {
      double angle = (Math.PI * 2) * this.random.nextDouble();
      double radius = Math.sqrt(this.random.nextDouble());
      float x = (float)(radius * Math.cos(angle));
      float y = (float)(radius * Math.sin(angle));
      return new java.awt.geom.Point2D.Float(x, y);
   }

   protected void startStormPhase() {
      if (!this.isClient()) {
         this.hitTargets(512, false);
      } else {
         for (int i = 0; i < 30; i++) {
            this.spawnSnowRing(() -> ThemeColorRegistry.ICE.getRandomColor(), 35, 8, 1000, i * 0.05F);
         }

         for (int i = 0; i < 800; i++) {
            float exponentialFallOff = 1.0F - (float)Math.pow(GameRandom.globalRandom.getFloatBetween(0.0F, 1.0F), 4.0);
            float rX = 512.0F * exponentialFallOff;
            float rY = rX * 0.75F;
            java.awt.geom.Point2D.Float rp = this.randomPointInUnitCircle();
            this.level
               .entityManager
               .addParticle(this.x + rp.x * rX, this.y + rp.y * rY, Particle.GType.IMPORTANT_COSMETIC)
               .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
               .height(0.0F)
               .dontRotate()
               .fadesAlpha(0.4F, 0.6F)
               .sizeFades(1, (int)(100.0F * exponentialFallOff))
               .lifeTime(7000);
         }
      }
   }

   protected void runStormPhase() {
      if (!this.isClient()) {
         this.runWaveHit(40, false);
      } else {
         if (this.tickCounter % 3 == 0) {
            this.spawnSnowRing(() -> ThemeColorRegistry.ICE.getRandomColor(), 22, 8, 2500 - this.tickCounter * 10, 1.0F);
         }

         int snowParticles = 20;

         for (int i = 0; i < snowParticles; i++) {
            float startX = this.x - 500;
            float startY = this.y + 400;
            float endX = startX + 300.0F;
            float endY = startY + 400.0F;
            float XdistPerParticle = (endX - startX) / snowParticles;
            float YdistPerParticle = (endY - startY) / snowParticles;
            float sin = (float)Math.sin(Math.PI * i / snowParticles);
            this.getLevel()
               .entityManager
               .addParticle(startX - XdistPerParticle * i, startY - YdistPerParticle * i, this.particleTypeSwitcher.next())
               .sprite(GameResources.debrisParticles.sprite(this.random.getIntBetween(0, 5), 0, 10))
               .height(10.0F)
               .movesConstant(240.0F + 24.0F * sin, -80.0F - 8.0F * sin)
               .color(ThemeColorRegistry.ICE.getRandomColor())
               .lifeTime(1300)
               .fadesAlpha(0.0F, 0.5F * sin)
               .sizeFadesInAndOut(12, 24, 0.5F);
         }
      }
   }

   protected void endStormPhase() {
      if (this.isClient()) {
         for (int i = 0; i < 30; i++) {
            this.spawnSnowRing(() -> ThemeColorRegistry.BLUE.getRandomColor(), 15, 48, 1000, i * 0.05F);
         }

         SoundManager.playSound(GameResources.stomp, SoundEffect.effect(this.x, this.y).volume(0.25F).pitch(1.3F));
      } else if (this.owner.buffManager.hasBuff(BuffRegistry.CRYO_IMMUNTY)) {
         this.owner.buffManager.removeBuff(BuffRegistry.CRYO_IMMUNTY, true);
      }
   }

   protected void runFalloutPhase() {
      if (!this.isClient()) {
         this.runWaveHit(90, true);
      }
   }

   protected void spawnSnowRing(Supplier<Color> color, int startSize, int endSize, int lifetime, float distanceMultiplier) {
      AtomicReference<Float> currentAngle = new AtomicReference<>(this.random.nextFloat() * 360.0F);

      for (int i = 0; i < 25; i++) {
         float distance = distanceMultiplier * 1024.0F * 0.6F - i;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameMath.sin(currentAngle.get()) * distance + this.random.getIntBetween(-50, 50),
               this.y + GameMath.cos(currentAngle.get()) * distance + this.random.getIntBetween(-50, 50) * 0.75F,
               this.particleTypeSwitcher.next()
            )
            .height(0.0F)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float spinFactor = 443.0F;
               float angle = currentAngle.accumulateAndGet(delta * spinFactor / 250.0F, Float::sum);
               float distY = distance * 0.75F;
               pos.x = this.x + GameMath.sin(angle) * (distance * lifePercent);
               pos.y = this.y + GameMath.cos(angle) * (distY * lifePercent);
            })
            .color(color.get())
            .lifeTime(lifetime)
            .sizeFades(startSize, endSize);
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.x = (int)this.owner.x;
      this.y = (int)this.owner.y;
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
      hit.getLevelObject().attackThrough(this.damage, this.owner);
   }
}
