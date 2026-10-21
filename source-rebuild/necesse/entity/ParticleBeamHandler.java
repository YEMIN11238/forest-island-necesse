package necesse.entity;

import java.awt.Color;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Point2D.Float;
import java.util.Iterator;
import java.util.function.Supplier;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class ParticleBeamHandler {
   public ParticleTypeSwitcher pTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC);
   public int startThickness = 10;
   public int endThickness = 0;
   public int particleSizeMin = 16;
   public int particleSizeMax = 24;
   public float particleThicknessMod = 0.5F;
   public Supplier<Color> trailColor = () -> Color.WHITE;
   public Supplier<Color> particleColor = () -> Color.WHITE;
   public float particleSpeed = 50.0F;
   public float distPerParticle = 14.0F;
   public int endParticleSizeMin = 10;
   public int endParticleSizeMax = 18;
   public float height = 18.0F;
   private final Level level;
   private final Trail trail;
   private final GameLinkedList<GameLinkedList<ParticleBeamHandler.BeamParticle>> particles;

   public ParticleBeamHandler(Level level) {
      this.level = level;
      this.trail = new Trail(new TrailVector(0.0F, 0.0F, 1.0F, 0.0F, this.startThickness, this.height), level, Color.WHITE, 10000) {
         @Override
         public Color getColor() {
            return ParticleBeamHandler.this.trailColor.get();
         }
      };
      level.entityManager.addTrail(this.trail);
      this.particles = new GameLinkedList<>();
      this.particles.addLast(new GameLinkedList<>());
   }

   public ParticleBeamHandler sprite(GameSprite sprite) {
      this.trail.sprite = sprite;
      return this;
   }

   public ParticleBeamHandler particleColor(Supplier<Color> color) {
      this.particleColor = color;
      return this;
   }

   public ParticleBeamHandler particleColor(Color color) {
      return this.particleColor(() -> color);
   }

   public ParticleBeamHandler particleSize(int min, int max) {
      this.particleSizeMin = min;
      this.particleSizeMax = max;
      return this;
   }

   public ParticleBeamHandler trailColor(Supplier<Color> color) {
      this.trailColor = color;
      return this;
   }

   public ParticleBeamHandler trailColor(Color color) {
      return this.trailColor(() -> color);
   }

   public ParticleBeamHandler color(Supplier<Color> color) {
      this.particleColor(color);
      this.trailColor(color);
      return this;
   }

   public ParticleBeamHandler color(Color color) {
      this.particleColor(color);
      this.trailColor(color);
      return this;
   }

   public ParticleBeamHandler thickness(int startThickness, int endThickness) {
      this.startThickness = startThickness;
      this.endThickness = endThickness;
      return this;
   }

   public ParticleBeamHandler particleThicknessMod(float modifier) {
      this.particleThicknessMod = modifier;
      return this;
   }

   public ParticleBeamHandler speed(float speed) {
      this.particleSpeed = speed;
      return this;
   }

   public ParticleBeamHandler distPerParticle(float dist) {
      this.distPerParticle = dist;
      return this;
   }

   public ParticleBeamHandler endParticleSize(int min, int max) {
      this.endParticleSizeMin = min;
      this.endParticleSizeMax = max;
      return this;
   }

   public ParticleBeamHandler height(float height) {
      this.height = height;
      return this;
   }

   public ParticleBeamHandler drawOnTop() {
      this.trail.drawOnTop = true;
      return this;
   }

   public ParticleBeamHandler drawOnTop(int drawOrder) {
      this.trail.drawOnTop = true;
      this.trail.drawOnTopOrder = drawOrder;
      return this;
   }

   public void update(RayLinkedList<LevelObjectHit> rays, float delta) {
      GameLinkedList<Ray<LevelObjectHit>>.Element current = rays.getFirstElement();
      GameLinkedList<GameLinkedList<ParticleBeamHandler.BeamParticle>>.Element currentParticles = this.particles.getFirstElement();
      double totalDist = rays.totalDist;
      double currentDist = 0.0;
      boolean isFirst = true;

      while (true) {
         int thickness1 = this.endThickness + (int)((this.startThickness - this.endThickness) * Math.abs(currentDist / totalDist - 1.0));
         Ray<LevelObjectHit> currentRay = (Ray<LevelObjectHit>)current.object;
         TrailVector vector1 = new TrailVector(
            (float)currentRay.getX1(),
            (float)currentRay.getY1(),
            (float)(currentRay.getX2() - currentRay.getX1()),
            (float)(currentRay.getY2() - currentRay.getY1()),
            thickness1,
            this.height
         );
         if (isFirst) {
            this.trail.reset(vector1);
            isFirst = false;
         } else {
            this.trail.addPoint(vector1, true, 0);
         }

         currentDist += currentRay.dist;
         int thickness2 = this.endThickness + (int)((this.startThickness - this.endThickness) * Math.abs(currentDist / totalDist - 1.0));
         TrailVector vector2 = new TrailVector(
            (float)currentRay.getX2(),
            (float)currentRay.getY2(),
            (float)(currentRay.getX2() - currentRay.getX1()),
            (float)(currentRay.getY2() - currentRay.getY1()),
            thickness2,
            this.height
         );
         this.trail.addPoint(vector2, true, 0);
         this.refreshParticles(current, currentParticles, delta);
         if (this.level.tickManager().isGameTick()) {
            this.spawnRayParticles(current, (GameLinkedList<ParticleBeamHandler.BeamParticle>)currentParticles.object, thickness1, thickness2);
         }

         if (((Ray)current.object).targetHit != null && this.level.tickManager().isGameTick()) {
            this.spawnCollisionEndParticles(currentRay);
         }

         if (!current.hasNext()) {
            if (this.level.tickManager().isGameTick()) {
               this.spawnEndParticles(currentRay);
            }

            while (currentParticles.hasNext()) {
               GameLinkedList<GameLinkedList<ParticleBeamHandler.BeamParticle>>.Element nextParticles = currentParticles.next();

               for (ParticleBeamHandler.BeamParticle p : (GameLinkedList<ParticleBeamHandler.BeamParticle>)nextParticles.object) {
                  p.option.remove();
               }

               currentParticles.remove();
               currentParticles = nextParticles;
            }

            return;
         }

         current = current.next();
         if (!currentParticles.hasNext()) {
            currentParticles = currentParticles.insertAfter(new GameLinkedList());
         } else {
            currentParticles = currentParticles.next();
         }
      }
   }

   protected void spawnCollisionEndParticles(Ray<LevelObjectHit> ray) {
      if (this.particleColor != null && this.endParticleSizeMax > 0) {
         for (int i = 0; i < 3; i++) {
            Color color = this.particleColor.get();
            this.level.lightManager.refreshParticleLightFloat((float)ray.getX2(), (float)ray.getY2(), color, 1.0F);
            ParticleOption option;
            if (this.trail.drawOnTop) {
               option = this.level
                  .entityManager
                  .addTopParticle(
                     (float)ray.getX2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     (float)ray.getY2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     this.pTypeSwitcher.next(),
                     this.trail.drawOnTopOrder + 1
                  );
            } else {
               option = this.level
                  .entityManager
                  .addParticle(
                     (float)ray.getX2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     (float)ray.getY2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     this.pTypeSwitcher.next()
                  );
            }

            option.movesConstant(GameRandom.globalRandom.floatGaussian() * 8.0F, GameRandom.globalRandom.floatGaussian() * 8.0F)
               .sizeFades(this.endParticleSizeMin, this.endParticleSizeMax)
               .lifeTime(750)
               .color(color)
               .height(this.height);
         }
      }
   }

   protected void spawnEndParticles(Ray<LevelObjectHit> ray) {
      if (this.particleColor != null && this.endParticleSizeMax > 0) {
         for (int i = 0; i < 2; i++) {
            ParticleOption option;
            if (this.trail.drawOnTop) {
               option = this.level
                  .entityManager
                  .addTopParticle(
                     (float)ray.getX2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     (float)ray.getY2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     this.pTypeSwitcher.next(),
                     this.trail.drawOnTopOrder + 1
                  );
            } else {
               option = this.level
                  .entityManager
                  .addParticle(
                     (float)ray.getX2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     (float)ray.getY2() + GameRandom.globalRandom.floatGaussian() * 4.0F,
                     this.pTypeSwitcher.next()
                  );
            }

            option.movesConstant(GameRandom.globalRandom.floatGaussian() * 15.0F, GameRandom.globalRandom.floatGaussian() * 15.0F)
               .sizeFades(this.endParticleSizeMin, this.endParticleSizeMax)
               .lifeTime(500)
               .color(this.particleColor.get())
               .height(this.height);
         }
      }
   }

   protected void refreshParticles(
      GameLinkedList<Ray<LevelObjectHit>>.Element rayElement, GameLinkedList<GameLinkedList<ParticleBeamHandler.BeamParticle>>.Element pElements, float delta
   ) {
      for (ParticleBeamHandler.BeamParticle particle : (GameLinkedList<ParticleBeamHandler.BeamParticle>)pElements.object) {
         particle.increaseDist(rayElement, delta);
      }

      Iterator<GameLinkedList<ParticleBeamHandler.BeamParticle>.Element> iterator = ((GameLinkedList<ParticleBeamHandler.BeamParticle>)pElements.object).elementIterator();

      while (iterator.hasNext()) {
         GameLinkedList<ParticleBeamHandler.BeamParticle>.Element element = iterator.next();
         ParticleBeamHandler.BeamParticle particle = (ParticleBeamHandler.BeamParticle)element.object;
         if (!element.isRemoved() && !particle.option.isRemoved() && !(Math.abs(particle.dist) > ((Ray)rayElement.object).dist)) {
            particle.updatePos(element, rayElement, pElements);
         } else {
            particle.option.remove();
            element.remove();
         }
      }
   }

   protected void spawnRayParticles(
      GameLinkedList<Ray<LevelObjectHit>>.Element rayElement, GameLinkedList<ParticleBeamHandler.BeamParticle> particles, int thickness1, int thickness2
   ) {
      if (this.particleColor != null) {
         Ray<LevelObjectHit> ray = (Ray<LevelObjectHit>)rayElement.object;
         double dist = ray.getP1().distance(ray.getP2());

         for (double i = 0.0; i < dist - this.distPerParticle; i += this.distPerParticle) {
            double dx = ray.getX2() - ray.getX1();
            double dy = ray.getY2() - ray.getY1();
            double mod = GameRandom.globalRandom.getDoubleBetween(i / dist, (i + this.distPerParticle) / dist);
            double thickness = (thickness1 - thickness2) * Math.abs(mod - 1.0);
            float x = (float)(ray.getX1() + dx * mod);
            float y = (float)(ray.getY1() + dy * mod);
            Color color = this.particleColor.get();
            this.level.lightManager.refreshParticleLightFloat(x, y, color, 1.0F);
            Double dir = GameMath.normalize(dx, dy);
            Double perp = GameMath.getPerpendicularDir(dir.x, dir.y);
            double perpRandom = GameRandom.globalRandom.getFloatBetween(-1.0F, 1.0F) * thickness / 2.0 * this.particleThicknessMod;
            float xRandom = (float)(perp.x * perpRandom);
            float yRandom = (float)(perp.y * perpRandom);
            ParticleOption option;
            if (this.trail.drawOnTop) {
               option = this.level.entityManager.addTopParticle(x + xRandom, y + yRandom, this.pTypeSwitcher.next(), this.trail.drawOnTopOrder + 1);
            } else {
               option = this.level.entityManager.addParticle(x + xRandom, y + yRandom, this.pTypeSwitcher.next());
            }

            option.movesConstant(GameRandom.globalRandom.floatGaussian() * 5.0F, GameRandom.globalRandom.floatGaussian() * 5.0F)
               .sizeFades(this.particleSizeMin, this.particleSizeMax)
               .lifeTime(250)
               .color(color)
               .height(this.height);
            float distSpeed = GameRandom.globalRandom.getOneOf(1.0F, -1.0F) * this.particleSpeed;
            particles.addLast(new ParticleBeamHandler.BeamParticle(mod, distSpeed, option, xRandom, yRandom));
         }
      }
   }

   public void dispose() {
      this.trail.remove();
   }

   private static class BeamParticle {
      public double dist;
      public float speed;
      public float lastX;
      public float lastY;
      public float movedX;
      public float movedY;
      public ParticleOption option;

      public BeamParticle(double dist, float distSpeed, ParticleOption option, float xRandom, float yRandom) {
         this.dist = dist;
         this.speed = distSpeed;
         this.option = option;
         this.movedX = xRandom;
         this.movedY = yRandom;
         Float pos = option.getPos();
         this.lastX = pos.x;
         this.lastY = pos.y;
      }

      public void increaseDist(GameLinkedList<Ray<LevelObjectHit>>.Element rayElement, float delta) {
         this.dist = this.dist + this.speed * delta / 250.0F / ((Ray)rayElement.object).dist;
      }

      private void updatePos(
         GameLinkedList<ParticleBeamHandler.BeamParticle>.Element thisElement,
         GameLinkedList<Ray<LevelObjectHit>>.Element rayElement,
         GameLinkedList<GameLinkedList<ParticleBeamHandler.BeamParticle>>.Element pElements
      ) {
         Ray<LevelObjectHit> ray = (Ray<LevelObjectHit>)rayElement.object;
         GameLinkedList<Ray<LevelObjectHit>>.Element nextRay = null;
         GameLinkedList<GameLinkedList<ParticleBeamHandler.BeamParticle>>.Element nextParticles = null;
         int sig = 0;
         if (this.dist < 0.0) {
            sig = -1;
            nextRay = rayElement.prevWrap();
            nextParticles = pElements.prevWrap();
         } else if (this.dist > 1.0) {
            sig = 1;
            nextRay = rayElement.nextWrap();
            nextParticles = pElements.nextWrap();
         }

         if (sig != 0) {
            if (nextRay != null) {
               this.dist -= sig;
               thisElement.remove();
               GameLinkedList<ParticleBeamHandler.BeamParticle>.Element newElement = ((GameLinkedList<ParticleBeamHandler.BeamParticle>)nextParticles.object).addLast(thisElement.object);
               this.updatePos(newElement, nextRay, nextParticles);
               return;
            }

            this.option.remove();
         }

         double dx = ray.getX2() - ray.getX1();
         double dy = ray.getY2() - ray.getY1();
         Float currentPos = this.option.getPos();
         this.movedX = this.movedX + (currentPos.x - this.lastX);
         this.movedY = this.movedY + (currentPos.y - this.lastY);
         this.lastX = (float)(ray.getX1() + dx * this.dist + this.movedX);
         this.lastY = (float)(ray.getY1() + dy * this.dist + this.movedY);
         this.option.changePos(this.lastX, this.lastY);
      }
   }
}
