package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRegistry;

public class SoulseedExplosionEvent extends ExplosionEvent implements Attacker {
   protected Color particleColor;
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public SoulseedExplosionEvent() {
      this(0.0F, 0.0F, 60, new GameDamage(100.0F), false, 0.0F, null);
   }

   public SoulseedExplosionEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
      this.particleColor = this.setParticleColor();
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(10.0F);
   }

   @Override
   protected float getDistanceMod(float targetDistance) {
      return 1.0F;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(0.5F).pitch(1.8F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 2.0F, 2.0F, true);
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return super.canHitMob(target) && target != this.ownerMob;
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 0.7F;
   }

   protected Color setParticleColor() {
      return ThemeColorRegistry.SOULSEED.getRandomColor();
   }

   @Override
   public void clientTick() {
      if (this.tickCounter == 0) {
         this.playExplosionEffects();
      }

      int adjustedRange = this.range - this.range / 10;
      float lastRange = Math.max(0.0F, adjustedRange * ((float)(this.tickCounter - 2) / this.maxTicks));
      float range = adjustedRange * ((float)this.tickCounter / this.maxTicks);
      int particles = (int)this.getParticleCount(range, lastRange);
      spawnExplosionParticles(
         this.level,
         this.x,
         this.y,
         particles,
         lastRange,
         range * this.getParticleRangeModifier(),
         (level, x, y, dirX, dirY, lifeTime, currentRange) -> this.spawnExplosionParticle(x, y, dirX, dirY, lifeTime, currentRange)
      );
      this.tickCounter++;
      if (this.tickCounter > this.maxTicks) {
         this.over();
      }
   }

   protected void spawnCenterParticles(float x, float y, float dirX, float dirY, int lifeTime, float range) {
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      int moveMin = (int)(range * 0.05F);
      int moveMax = (int)(range * 0.4F);
      if (GameRandom.globalRandom.getChance(0.2F)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(moveMin, moveMax);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(moveMin, moveMax) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .sizeFades(14, 40)
            .movesFriction(dx * 0.04F, dy * 0.02F, 0.4F)
            .color(ThemeColorRegistry.SMOKE.getRandomColor())
            .heightMoves(0.0F, 8.0F)
            .lifeTime(lifeTime);
      }

      if (GameRandom.globalRandom.getChance(1.0F)) {
         this.level
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
            .sizeFades(4, GameMath.max(18, moveMax))
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .color(ThemeColorRegistry.SOULSEED.getRandomColor())
            .height(8.0F)
            .givesLight(75.0F, 0.5F)
            .lifeTime(lifeTime);
      }
   }
}
