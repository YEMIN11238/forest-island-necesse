package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class AscendedPushExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public AscendedPushExplosionEvent() {
      this(0.0F, 0.0F, 150, 5000, null);
   }

   public AscendedPushExplosionEvent(float x, float y, int range, int knockback, Mob owner) {
      super(x, y, range, new GameDamage(1.0F), false, 0.0F, owner);
      this.knockback = knockback;
   }

   @Override
   protected GameDamage getTotalMobDamage(float mod) {
      return super.getTotalMobDamage(mod).modDamage(0.0F);
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.magicbolt3, SoundEffect.effect(this.x, this.y).falloffDistance(4000).volume(1.5F).pitch(1.5F));
      SoundManager.playSound(GameResources.magicroar, SoundEffect.effect(this.x, this.y).falloffDistance(4000).volume(1.5F).pitch(2.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 3.0F, 3.0F, true);
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (range <= Math.max(this.range - 30, 30)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(140, 150);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.ascendedShadeParticle.sprite(0, 0, 12))
            .sizeFades(24, 48)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.2F)
            .heightMoves(0.0F, 10.0F)
            .lifeTime(lifeTime / 2);
      }
   }
}
