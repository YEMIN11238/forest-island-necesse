package necesse.entity.particle;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.tween.Easings;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class SplashResidueParticle extends ParticleOption {
   public SplashResidueParticle(Level level, float x, float y, int height, Color color) {
      super(x, y);
      if (!level.isServer()) {
         spawnParticles(level, x, y, height, color);
      }
   }

   public SplashResidueParticle(Level level, float x, float y, Color color) {
      this(level, x, y, 64, color);
   }

   public SplashResidueParticle(Level level, float x, float y) {
      this(level, x, y, Color.lightGray);
   }

   public static void spawnParticles(Level level, float x, float y, int height, Color color) {
      fillCircle(height / 12.0F, height / 60.0F * 4.0F, (index, angle, point) -> {
         float particleSizeFactor = GameRandom.globalRandom.getFloatBetween(15.0F, 35.0F);
         float particleSize = height / 60.0F * particleSizeFactor;
         float actualParticleSize = GameMath.limit(particleSize / 2.0F, 10.0F, 126.0F);
         fillCircle(particleSize / 2.0F - actualParticleSize / 2.0F, actualParticleSize / 2.0F, (subIndex, subAngle, subPoint) -> {
            float startHeight = point.y + subPoint.y;
            addBaseParticle(level, x + point.x + subPoint.x, y, startHeight, actualParticleSize, color, (int)(900.0F + particleSizeFactor * 10.0F));
         });
      });
   }

   protected static int fillCircle(float radius, float pointRadius, SplashResidueParticle.CirclePointConsumer consumer) {
      float currentAngle = 360.0F;
      float currentRadius = 0.0F;

      int particleCount;
      for (particleCount = 0; currentRadius <= radius; particleCount++) {
         float x = GameMath.cos(currentAngle) * currentRadius;
         float y = GameMath.sin(currentAngle) * currentRadius;
         if (currentAngle >= 360.0F) {
            currentAngle -= 360.0F;
            currentRadius += pointRadius * 0.75F;
         }

         currentAngle += (float)(pointRadius * 1.5F / (currentRadius * Math.PI / 180.0));
         consumer.accept(particleCount, currentAngle, new Float(x, y));
      }

      return particleCount;
   }

   protected static ParticleOption addBaseParticle(Level level, float x, float y, float startHeight, float size, Color color, int lifeTime) {
      return level.entityManager
         .addParticle(x, y, null)
         .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
         .size((options, currentLifeTime, timeAlive, lifePercent) -> {
            int actualSize = (int)(Easings.BackOut.ease(1.0F - lifePercent) * size);
            options.size(actualSize, actualSize);
         })
         .height(startHeight)
         .fadesAlpha(0.1F, 0.35F)
         .color(color)
         .lifeTime(lifeTime);
   }

   @FunctionalInterface
   protected interface CirclePointConsumer {
      void accept(int var1, float var2, Float var3);
   }
}
