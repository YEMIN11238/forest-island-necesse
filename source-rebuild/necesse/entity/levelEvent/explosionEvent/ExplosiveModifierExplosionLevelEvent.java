package necesse.entity.levelEvent.explosionEvent;

import java.awt.geom.Point2D.Float;
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

public class ExplosiveModifierExplosionLevelEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public ExplosiveModifierExplosionLevelEvent() {
      this(0.0F, 0.0F, 100, new GameDamage(100.0F), false, 0.0F, null);
   }

   public ExplosiveModifierExplosionLevelEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
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
   protected float getDistanceMod(float targetDistance) {
      return 1.0F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x + 4.0F, y - 10.0F, this.explosionTypeSwitcher.next())
            .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
            .sizeFades(25, 40)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .flameColor(20.0F)
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
