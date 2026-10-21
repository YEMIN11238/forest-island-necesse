package necesse.entity.particle.fireworks;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;
import java.util.function.Function;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.Ray;
import necesse.engine.util.RayLinkedList;
import necesse.entity.particle.ParticleOption;

public class FireworksPath {
   private static final float phi = (float)(Math.PI * (3.0 - Math.sqrt(5.0)));
   public static Polygon star = new Polygon();
   public static Polygon heart = new Polygon();
   public ParticleOption.FloatGetter delta = (lifeTime, timeAlive, lifePercent) -> (float)Math.pow(lifePercent, 0.3F);
   public float dx;
   public float dy;
   public float dh;

   public static FireworksRocketParticle.ParticleGetter<FireworksPath> sphere(float radius) {
      return (particle, progress, random) -> {
         float z = 1.0F - progress * 2.0F;
         float fRadius = (float)Math.sqrt(1.0F - z * z);
         float theta = phi * particle;
         float x = (float)(Math.cos(theta) * fRadius);
         float y = (float)(Math.sin(theta) * fRadius);
         float heightPart = 0.25F;
         return new FireworksPath(x * radius, y * radius * (1.0F - heightPart), -y * radius * heightPart);
      };
   }

   public static FireworksRocketParticle.ParticleGetter<FireworksPath> disc(float radius) {
      return (particle, progress, random) -> {
         float x = GameMath.cos(progress * 360.0F);
         float y = GameMath.sin(progress * 360.0F) * 0.3F;
         float dist = particle % 2 == 0 ? 1.15F : 0.85F;
         float heightPart = 0.25F;
         return new FireworksPath(x * dist * radius, y * dist * radius * (1.0F - heightPart), -y * dist * radius * heightPart);
      };
   }

   public static FireworksRocketParticle.ParticleGetter<FireworksPath> splash(float angle, float size) {
      java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
      return (particle, progress, random) -> {
         float x = dir.x / 2.0F + GameMath.cos(progress * 360.0F);
         float y = (dir.y / 2.0F + GameMath.sin(progress * 360.0F)) * 0.8F;
         float dist = random.getFloatBetween(0.0F, size);
         float heightPart = 0.25F;
         return new FireworksPath(x * dist, y * dist * (1.0F - heightPart), -y * dist * heightPart);
      };
   }

   public static FireworksRocketParticle.ParticleGetter<FireworksPath> shape(Shape shape, float size) {
      return shape(shape, size, random -> 1.0F);
   }

   public static FireworksRocketParticle.ParticleGetter<FireworksPath> shape(Shape shape, float size, Function<GameRandom, Float> distMod) {
      int points = 0;
      double midX = 0.0;
      double midY = 0.0;
      RayLinkedList<Boolean> list = new RayLinkedList<>();
      double[] lastPoint = null;
      double[] coords = new double[6];

      for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
         int SG = it.currentSegment(coords);
         if (SG == 4) {
            lastPoint = null;
         } else if (SG == 0) {
            lastPoint = new double[]{coords[0], coords[1]};
         } else if (SG == 1) {
            if (lastPoint != null) {
               midX += lastPoint[0] + coords[0];
               midY += lastPoint[1] + coords[1];
               points += 2;
               Ray<Boolean> ray = new Ray<>(lastPoint[0], lastPoint[1], coords[0], coords[1], false, null);
               list.addLast(ray);
               list.totalDist = list.totalDist + ray.dist;
            }

            lastPoint = new double[]{coords[0], coords[1]};
         }
      }

      Rectangle2D bounds = shape.getBounds2D();
      double finalSize = size / new Double(bounds.getWidth(), bounds.getHeight()).distance(0.0, 0.0) * 2.5;
      double finalMidX = midX / points;
      double finalMidY = midY / points;
      return (particle, progress, random) -> {
         double currentDist = list.totalDist * progress;
         double px = 0.0;
         double py = 0.0;

         for (Ray<Boolean> rayx : list) {
            if (currentDist <= rayx.dist) {
               Double rayDir = GameMath.normalize(rayx.x2 - rayx.x1, rayx.y2 - rayx.y1);
               Double rayPoint = new Double(rayx.x1 + rayDir.x * currentDist, rayx.y1 + rayDir.y * currentDist);
               Double dir = new Double(rayPoint.x - finalMidX, rayPoint.y - finalMidY);
               Float mod = distMod.apply(random);
               px = dir.x * mod.floatValue() * finalSize;
               py = dir.y * mod.floatValue() * finalSize;
               break;
            }

            currentDist -= rayx.dist;
         }

         return new FireworksPath((float)px, (float)py, 0.0F);
      };
   }

   public FireworksPath(float dx, float dy, float dh) {
      this.dx = dx;
      this.dy = dy;
      this.dh = dh;
   }

   static {
      star.addPoint(0, 0);
      star.addPoint(1, 2);
      star.addPoint(3, 2);
      star.addPoint(1, 3);
      star.addPoint(2, 5);
      star.addPoint(0, 4);
      star.addPoint(-2, 5);
      star.addPoint(-1, 3);
      star.addPoint(-3, 2);
      star.addPoint(-1, 2);
      star.addPoint(0, 0);
      heart.addPoint(0, 0);
      heart.addPoint(1, -1);
      heart.addPoint(2, -1);
      heart.addPoint(3, 0);
      heart.addPoint(3, 1);
      heart.addPoint(0, 4);
      heart.addPoint(-3, 1);
      heart.addPoint(-3, 0);
      heart.addPoint(-2, -1);
      heart.addPoint(-1, -1);
      heart.addPoint(0, 0);
   }
}
