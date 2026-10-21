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
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class ElectricOrbExplosionLevelEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public ElectricOrbExplosionLevelEvent() {
      this(0.0F, 0.0F, 100, new GameDamage(100.0F), false, 0.0F, null);
      this.knockback = 0;
   }

   public ElectricOrbExplosionLevelEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
      this.knockback = 0;
      this.hitsOwner = false;
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(10.0F);
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.fizz, SoundEffect.effect(this.x, this.y).volume(1.5F).pitch(1.5F));
      SoundManager.playSound(GameResources.electricExplosion, SoundEffect.effect(this.x, this.y).volume(1.5F).pitch(1.0F));
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
      GameRandom rnd = GameRandom.globalRandom;
      if (rnd.getChance(0.2F)) {
         this.level
            .entityManager
            .addParticle(x + 4.0F, y - 10.0F, this.explosionTypeSwitcher.next())
            .sprite(GameResources.magicSparkParticles.sprite(rnd.nextInt(4), 0, 22))
            .sizeFades(10, 25)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .color(new Color(95, 205, 228))
            .givesLight(190.0F, 0.9F)
            .height(10.0F)
            .onProgress(
               0.4F,
               p -> {
                  if (rnd.getChance(1.0F)) {
                     Float norm = GameMath.normalize(dirX, dirY);
                     this.level
                        .entityManager
                        .addParticle(p.x + norm.x, p.y + norm.y, Particle.GType.IMPORTANT_COSMETIC)
                        .smokeColor()
                        .heightMoves(10.0F, 40.0F)
                        .lifeTime(lifeTime);
                  }
               }
            )
            .lifeTime(lifeTime);
      }
   }
}
