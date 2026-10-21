package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.theVoid.TheVoidMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class MiniRocketExplosionEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public MiniRocketExplosionEvent() {
      this(0.0F, 0.0F, 50, TheVoidMob.voidRainDamage, null);
   }

   public MiniRocketExplosionEvent(float x, float y, int range, GameDamage damage, Mob owner) {
      super(x, y, range, damage, false, 0.0F, owner);
      this.targetRangeMod = 0.0F;
      this.knockback = 10;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.explosionLight, SoundEffect.effect(this.x, this.y).volume(0.5F).pitch(2.0F));
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 0.5F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (range <= Math.max(this.range - 125, 25)) {
         float dx = dirX * GameRandom.globalRandom.getIntBetween(50, 75);
         float dy = dirY * GameRandom.globalRandom.getIntBetween(50, 75) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.explosionTypeSwitcher.next())
            .color(new Color(255, 233, 73))
            .sizeFades(12, 24)
            .movesFriction(dx * 0.05F, dy * 0.05F, 0.8F)
            .heightMoves(0.0F, 10.0F)
            .ignoreLight(true)
            .lifeTime(100);
      }

      if (GameRandom.globalRandom.getChance(0.2F)) {
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
            .lifeTime(lifeTime);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return DeathMessageTable.fromRange("thevoid", 5);
   }
}
