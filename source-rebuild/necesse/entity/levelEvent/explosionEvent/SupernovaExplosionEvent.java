package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.SunlightChampionMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class SupernovaExplosionEvent extends ExplosionEvent implements Attacker {
   private int particleBuffer;
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public SupernovaExplosionEvent() {
      this(0.0F, 0.0F, 650, SunlightChampionMob.supernovaDamage, 0.0F, null);
   }

   public SupernovaExplosionEvent(float x, float y, int range, GameDamage damage, float toolTier, Mob owner) {
      super(x, y, range, damage, false, toolTier, owner);
      this.targetRangeMod = 0.0F;
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(10.0F);
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(2.5F).pitch(1.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 3.0F, 3.0F, true);
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 1.5F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (this.particleBuffer < 10) {
         this.particleBuffer++;
      } else {
         this.particleBuffer = 0;
         if (range <= Math.max(this.range - 125, 25)) {
            float dx = dirX * GameRandom.globalRandom.getIntBetween(140, 150);
            float dy = dirY * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
            this.getLevel()
               .entityManager
               .addParticle(x, y, this.explosionTypeSwitcher.next())
               .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
               .sizeFades(70, 100)
               .givesLight(53.0F, 1.0F)
               .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
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
               .heightMoves(0.0F, 10.0F)
               .lifeTime(lifeTime * 3);
         }
      }
   }
}
