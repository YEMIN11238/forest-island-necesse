package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.BloatedSpiderMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class BloatedSpiderExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public BloatedSpiderExplosionEvent() {
      this(0.0F, 0.0F, 150, BloatedSpiderMob.explosionDamage, false, 0.0F, null);
   }

   public BloatedSpiderExplosionEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
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
      if (GameRandom.globalRandom.getChance(0.7F) && range < 25.0F) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(20, 70);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(10, 60) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .sizeFades(70, 80)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .color(new Color(89, 89, 89))
            .heightMoves(0.0F, 70.0F)
            .lifeTime(lifeTime * 4);
      }

      if (range <= Math.max(this.range - 125, 25)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(140, 150);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .sizeFades(30, 50)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .color(new Color(255, 255, 255))
            .heightMoves(0.0F, 10.0F)
            .lifeTime(lifeTime * 3);
      }

      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
            .sizeFades(25, 40)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .flameColor(75.0F)
            .height(10.0F)
            .givesLight(75.0F, 0.5F)
            .onProgress(
               0.4F,
               p -> {
                  Float norm = GameMath.normalize(dirX, dirY);
                  this.level
                     .entityManager
                     .addParticle(p.x + norm.x * 20.0F, p.y + norm.y * 20.0F, Particle.GType.IMPORTANT_COSMETIC)
                     .movesConstant(dirX, dirY)
                     .smokeColor()
                     .heightMoves(10.0F, 40.0F)
                     .lifeTime(lifeTime);
               }
            )
            .lifeTime(lifeTime);
      }
   }
}
