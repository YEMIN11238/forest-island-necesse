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
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;

public class PatchworkGrenadeExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   protected ResilienceOnHitProjectileModifier resilienceModifier;

   public PatchworkGrenadeExplosionEvent() {
      this(0.0F, 0.0F, 100, new GameDamage(100.0F), null, null);
   }

   public PatchworkGrenadeExplosionEvent(float x, float y, int range, GameDamage damage, Mob owner, ResilienceOnHitProjectileModifier resilienceModifier) {
      super(x, y, range, damage, false, 0.0F, owner);
      this.targetRangeMod = 0.0F;
      this.hitsOwner = false;
      this.resilienceModifier = resilienceModifier;
      this.knockback = 100;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).falloffDistance(2000).volume(0.6F).pitch(2.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 1.5F, 1.5F, true);
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 0.25F;
   }

   @Override
   protected void onMobWasHit(Mob mob, float distance) {
      super.onMobWasHit(mob, distance);
      if (this.resilienceModifier != null && !this.resilienceModifier.hasGained) {
         this.resilienceModifier.hasGained = true;
         this.ownerMob.addResilience(this.resilienceModifier.resilienceGain);
      }
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (GameRandom.globalRandom.getChance(0.2F) && range < 25.0F) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(20, 70);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(10, 60) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .sizeFades(70, 80)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .color(new Color(60, 40, 40))
            .heightMoves(0.0F, 70.0F)
            .lifeTime(lifeTime * 3);
      }

      if (range <= Math.max(this.range - 125, 25)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(140, 150);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.scrapParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 20))
            .sizeFades(30, 50)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .heightMoves(0.0F, 10.0F)
            .lifeTime(lifeTime / 2);
      }

      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.smokePuff.sprite(GameRandom.globalRandom.nextInt(4), 0, 32))
            .sizeFades(16, 32)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .heightMoves(10.0F, 30.0F)
            .color(
               (options, lifeTime1, timeAlive, lifePercent) -> {
                  float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                  Color fireColor = new Color(255, 150, 50);
                  Color smokeColor = new Color(45, 35, 35);
                  options.color(
                     new Color(
                        GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                        GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                        GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                     )
                  );
               }
            )
            .ignoreLight(true)
            .givesLight(35.0F, 0.8F)
            .lifeTime(lifeTime);
      }
   }
}
