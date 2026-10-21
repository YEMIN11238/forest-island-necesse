package necesse.entity.levelEvent.explosionEvent.splashEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.SplashResidueParticle;
import necesse.gfx.GameResources;

public abstract class SplashEvent extends ExplosionEvent {
   protected boolean isLiquid = false;

   public SplashEvent() {
      this(0.0F, 0.0F, 96, new GameDamage(0.0F), false, 0.0F, null);
   }

   public SplashEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
      this.knockback = 0;
      this.isLiquid = true;
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 0.4F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      this.level
         .entityManager
         .addParticle(x, y, Particle.GType.CRITICAL)
         .movesConstant(dirX * 0.1F, dirY * 0.1F)
         .color(this.getInnerSplashColor())
         .height(10.0F)
         .sizeFades(12, 18)
         .givesLight(250.0F, 0.3F)
         .onProgress(
            0.1F,
            p -> {
               Float norm = GameMath.normalize(dirX, dirY);
               this.level
                  .entityManager
                  .addParticle(p.x + norm.x * 20.0F, p.y + norm.y * 20.0F, Particle.GType.IMPORTANT_COSMETIC)
                  .movesConstant(dirX, dirY)
                  .color(this.getOuterSplashColor())
                  .sizeFades(10, 14)
                  .heightMoves(10.0F, 30.0F)
                  .lifeTime(lifeTime);
            }
         )
         .lifeTime((int)(lifeTime * 1.4F));
      if (this.isLiquid && GameRandom.globalRandom.getEveryXthChance(20)) {
         SplashResidueParticle puddle = new SplashResidueParticle(this.level, x, y - 5.0F, this.getInnerSplashColor());
         this.level.entityManager.addParticle(puddle, Particle.GType.IMPORTANT_COSMETIC);
      }
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(
         GameResources.splash,
         SoundEffect.effect(this.x, this.y).falloffDistance(1200).volume(0.1F).pitch(GameRandom.globalRandom.getFloatBetween(1.15F, 1.25F))
      );
      SoundManager.playSound(
         GameResources.shatter1, SoundEffect.effect(this.x, this.y).volume(0.5F).pitch(GameRandom.globalRandom.getFloatBetween(1.15F, 1.25F))
      );
   }

   @Override
   protected float getParticleRangeModifier() {
      return 0.15F;
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return super.canHitMob(target) && target != this.ownerMob;
   }

   protected abstract Color getInnerSplashColor();

   protected Color getOuterSplashColor() {
      return this.getInnerSplashColor().brighter().brighter();
   }
}
