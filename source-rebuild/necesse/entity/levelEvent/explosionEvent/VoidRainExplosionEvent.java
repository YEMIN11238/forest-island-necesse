package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class VoidRainExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public VoidRainExplosionEvent() {
      this(0.0F, 0.0F, 50, TheVoidMob.voidRainDamage, null);
   }

   public VoidRainExplosionEvent(float x, float y, int range, GameDamage damage, Mob owner) {
      super(x, y, range, damage, false, 0.0F, owner);
      this.targetRangeMod = 0.0F;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.magicExplosion, SoundEffect.effect(this.x, this.y).volume(1.5F).pitch(1.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 3.0F, 3.0F, true);
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 0.5F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (range <= Math.max(this.range - 125, 25)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(140, 150);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.ascendedParticle.sprite(0, 0, 20))
            .sizeFades(12, 24)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .heightMoves(0.0F, 10.0F)
            .ignoreLight(true)
            .lifeTime(lifeTime);
      }

      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.voidPuffParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 12))
            .sizeFades(24, 36)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .height(10.0F)
            .ignoreLight(true)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return DeathMessageTable.fromRange("thevoid", 5);
   }
}
