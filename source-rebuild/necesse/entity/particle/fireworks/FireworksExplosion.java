package necesse.entity.particle.fireworks;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.level.maps.Level;

public class FireworksExplosion {
   public static FireworksRocketParticle.ParticleGetter<FireworksPath> popPath = (particle, progress, random) -> {
      float x = random.floatGaussian() * 10.0F;
      float y = random.floatGaussian() * 10.0F;
      float heightPart = 0.25F;
      return new FireworksPath(x, y * (1.0F - heightPart), y * heightPart);
   };
   public static FireworksExplosion simplePopExplosion = new FireworksExplosion(popPath);
   public int particles = 300;
   public int lifetime = 2000;
   public int minSize = 10;
   public int maxSize = 18;
   public float particleLightHue = ParticleOption.defaultFlameHue;
   public float particleLightSaturation = 0.7F;
   public float trailChance = 0.2F;
   public float trailSize = 5.0F;
   public int trailFadeTime = 500;
   public FireworksRocketParticle.ParticleGetter<FireworksPath> pathGetter;
   public FireworksRocketParticle.ParticleGetter<Color> colorGetter = (particle, progress, random) -> Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F);
   public float popChance = 0.5F;
   public FireworksExplosion popOptions = simplePopExplosion;
   public FireworksRocketParticle.RandomSoundPlayer explosionSound = (pos, height, random) -> SoundManager.playSound(
      GameResources.fireworkExplosion,
      SoundEffect.effect(pos.x, pos.y).pitch(random.getOneOf(0.95F, 1.0F, 1.05F)).volume(Math.max(0.0F, 1.0F - height / 1000.0F) * 1.5F).falloffDistance(2000)
   );

   public FireworksExplosion(FireworksRocketParticle.ParticleGetter<FireworksPath> pathGetter) {
      this.pathGetter = pathGetter;
   }

   public void spawnExplosion(Level level, float startX, float startY, float startHeight, GameRandom random) {
      if (this.explosionSound != null) {
         this.explosionSound.play(new Float(startX, startY), startHeight, random);
      }

      for (int i = 0; i <= this.particles; i++) {
         float fParticles = (float)i / this.particles;
         FireworksPath path = this.pathGetter.get(i, fParticles, random);
         Color color = this.colorGetter.get(i, fParticles, random);
         Trail trail;
         if (random.getChance(this.trailChance)) {
            trail = new Trail(new TrailVector(startX, startY, startX, startY, this.trailSize, startHeight), level, color, this.trailFadeTime);
            level.entityManager.addTrail(trail);
         } else {
            trail = null;
         }

         ParticleOption particle = level.entityManager
            .addParticle(startX, startY, null)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float progress = path.delta.get(lifeTime, timeAlive, lifePercent);
               pos.x = startX + path.dx * progress;
               pos.y = startY + path.dy * progress;
            })
            .height((lifeTime, timeAlive, lifePercent) -> {
               float progress = path.delta.get(lifeTime, timeAlive, lifePercent);
               return startHeight + path.dh * progress;
            })
            .color(color)
            .rotates()
            .sizeFades(this.minSize, this.maxSize)
            .givesLight(this.particleLightHue, this.particleLightSaturation)
            .rotates(200.0F, 300.0F)
            .lifeTime(this.lifetime);
         if (random.getChance(this.popChance) && this.popOptions != null) {
            particle.onProgress(random.getFloatBetween(0.65F, 0.95F), pos -> {
               float height = particle.getCurrentHeight();
               this.popOptions.spawnExplosion(level, pos.x, pos.y, height, random);
            });
         }

         if (trail != null) {
            particle.onMoveTick(
               (delta, lifeTime, timeAlive, lifePercent) -> {
                  Float particlePos = particle.getLevelPos();
                  trail.addPoint(
                     new TrailVector(particlePos.x, particlePos.y, startX, startY, this.trailSize * Math.abs(lifePercent - 1.0F), particle.getCurrentHeight()),
                     0
                  );
               }
            );
         }
      }
   }

   static {
      simplePopExplosion.particles = 1;
      simplePopExplosion.lifetime = 300;
      simplePopExplosion.minSize = 6;
      simplePopExplosion.maxSize = 10;
      simplePopExplosion.trailChance = 0.0F;
      simplePopExplosion.popChance = 0.0F;
      simplePopExplosion.colorGetter = (particle, progress, random) -> ParticleOption.randomFlameColor(random);
      simplePopExplosion.explosionSound = null;
      simplePopExplosion.explosionSound = (pos, height, random) -> {
         if (random.getChance(0.2F)) {
            SoundManager.playSound(
               GameResources.fireworkCrack,
               SoundEffect.effect(pos.x, pos.y)
                  .pitch(random.getOneOf(1.0F, 1.2F, 1.4F))
                  .volume(Math.max(0.0F, 1.0F - height / 1000.0F) / 4.0F)
                  .falloffDistance(2500)
            );
         }
      };
   }
}
