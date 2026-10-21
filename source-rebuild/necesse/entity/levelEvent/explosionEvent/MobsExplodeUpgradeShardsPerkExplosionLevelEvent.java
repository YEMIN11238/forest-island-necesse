package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class MobsExplodeUpgradeShardsPerkExplosionLevelEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public MobsExplodeUpgradeShardsPerkExplosionLevelEvent() {
      this(0.0F, 0.0F, 100, new GameDamage(100.0F), false, 0.0F, null);
   }

   public MobsExplodeUpgradeShardsPerkExplosionLevelEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
      this.knockback = 0;
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(10.0F);
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return false;
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(
         GameResources.magicExplosion, SoundEffect.effect(this.x, this.y).volume(1.0F).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F))
      );
      this.level
         .entityManager
         .events
         .addHidden(
            new WaitForSecondsEvent(0.2F) {
               @Override
               public void onWaitOver() {
                  SoundManager.playSound(
                     GameResources.magicExplosion,
                     SoundEffect.effect(MobsExplodeUpgradeShardsPerkExplosionLevelEvent.this.x, MobsExplodeUpgradeShardsPerkExplosionLevelEvent.this.y)
                        .volume(0.85F)
                        .pitch(GameRandom.globalRandom.getFloatBetween(1.2F, 1.4F))
                  );
                  this.level
                     .entityManager
                     .events
                     .addHidden(
                        new WaitForSecondsEvent(0.2F) {
                           @Override
                           public void onWaitOver() {
                              SoundManager.playSound(
                                 GameResources.magicExplosion,
                                 SoundEffect.effect(
                                       MobsExplodeUpgradeShardsPerkExplosionLevelEvent.this.x, MobsExplodeUpgradeShardsPerkExplosionLevelEvent.this.y
                                    )
                                    .volume(0.75F)
                                    .pitch(GameRandom.globalRandom.getFloatBetween(1.4F, 1.6F))
                              );
                           }
                        }
                     );
               }
            }
         );
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
            .color(new Color(16, 120, 255, 215))
            .movesConstant(dirX * 0.25F, dirY * 0.25F)
            .height(10.0F)
            .givesLight(75.0F, 0.5F)
            .heightMoves(0.0F, 20.0F)
            .onProgress(
               0.05F,
               p -> {
                  Float norm = GameMath.normalize(dirX, dirY);
                  this.level
                     .entityManager
                     .addParticle(p.x + norm.x * 15.0F, p.y + norm.y * 15.0F, Particle.GType.IMPORTANT_COSMETIC)
                     .rotates()
                     .height(10.0F)
                     .color(new Color(16, 183, 255, 226))
                     .movesConstant(dirX * 0.25F, dirY * 0.25F)
                     .heightMoves(0.0F, 20.0F)
                     .onProgress(
                        0.1F,
                        p2 -> {
                           Float norm2 = GameMath.normalize(dirX, dirY);
                           this.level
                              .entityManager
                              .addParticle(p2.x + norm2.x * 20.0F, p2.y + norm2.y * 20.0F, Particle.GType.IMPORTANT_COSMETIC)
                              .rotates()
                              .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
                              .height(10.0F)
                              .color(new Color(90, 195, 253, 226))
                              .movesConstant(dirX * 0.25F, dirY * 0.25F)
                              .heightMoves(0.0F, 20.0F)
                              .lifeTime(2000);
                        }
                     )
                     .lifeTime(2000);
               }
            )
            .lifeTime(2000);
      }
   }
}
