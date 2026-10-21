package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class GhostSkullExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public GhostSkullExplosionEvent() {
      this(0.0F, 0.0F, new GameDamage(100.0F), null);
   }

   public GhostSkullExplosionEvent(float x, float y, GameDamage damage, Mob owner) {
      super(x, y, 60, damage, false, 0.0F, owner);
      this.sendCustomData = false;
      this.sendOwnerData = true;
      this.knockback = 0;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.fadedeath3, SoundEffect.effect(this.x, this.y).volume(1.0F).pitch(1.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 400, 50, 3.0F, 3.0F, true);
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (range <= Math.max(this.range, 25)) {
         GameRandom random = GameRandom.globalRandom;
         float dx = dirX * random.getIntBetween(30, 40);
         float dy = dirY * random.getIntBetween(30, 40) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
            .sizeFades(30, 50)
            .movesFriction(dx * 0.05F, dy * 0.05F, 1.8F)
            .color(new Color(176 - random.getIntBetween(10, 50), 234 - random.getIntBetween(10, 50), 190 - random.getIntBetween(10, 50)))
            .heightMoves(0.0F, 10.0F)
            .alpha(0.5F)
            .lifeTime(lifeTime);
      }
   }
}
