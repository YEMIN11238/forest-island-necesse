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

public class RebornExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public RebornExplosionEvent() {
      this(0.0F, 0.0F, 200, new GameDamage(300.0F), null);
      this.hitsOwner = false;
   }

   public RebornExplosionEvent(float x, float y, int range, GameDamage damage, Mob owner) {
      super(x, y, range, damage, false, 0.0F, owner);
      this.targetRangeMod = 0.0F;
      this.hitsOwner = false;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.fireShot, SoundEffect.effect(this.x, this.y).volume(1.5F).pitch(0.5F));
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
            .color(
               (options, lifeTime1, timeAlive, lifePercent) -> {
                  Color startColor = new Color(223, 220, 80);
                  Color endColor = new Color(223, 80, 28);
                  options.color(
                     new Color(
                        GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                        GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                        GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                     )
                  );
               }
            )
            .sizeFades(12, 24)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .heightMoves(0.0F, 10.0F)
            .ignoreLight(true)
            .givesLight(35.0F, 0.8F)
            .lifeTime(lifeTime);
      }

      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .color(
               (options, lifeTime1, timeAlive, lifePercent) -> {
                  Color startColor = new Color(223, 220, 80);
                  Color endColor = new Color(223, 80, 28);
                  options.color(
                     new Color(
                        GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                        GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                        GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                     )
                  );
               }
            )
            .sizeFades(24, 36)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .height(10.0F)
            .ignoreLight(true)
            .givesLight(35.0F, 0.8F)
            .lifeTime(lifeTime);
      }
   }
}
