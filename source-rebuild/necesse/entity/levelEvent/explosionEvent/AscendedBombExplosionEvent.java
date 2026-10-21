package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.BloatedSpiderMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class AscendedBombExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public AscendedBombExplosionEvent() {
      this(0.0F, 0.0F, 150, BloatedSpiderMob.explosionDamage, false, 0.0F, null);
   }

   public AscendedBombExplosionEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(1000.0F);
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(this.x, this.y).volume(2.0F).pitch(1.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 3.0F, 3.0F, true);
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 1.5F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (range <= Math.max(this.range - 125, 25)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(140, 150);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sizeFades(10, 20)
            .color(new Color(255, 0, 231))
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .heightMoves(0.0F, 10.0F)
            .ignoreLight(true)
            .lifeTime(lifeTime * 3);
      }

      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.ascendedParticle.sprite(0, 0, 20))
            .sizeFades(25, 40)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .height(10.0F)
            .ignoreLight(true)
            .lifeTime(lifeTime);
      }
   }
}
