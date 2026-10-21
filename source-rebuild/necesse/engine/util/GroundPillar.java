package necesse.engine.util;

import java.awt.Point;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.level.maps.Level;

public abstract class GroundPillar extends Point {
   public GroundPillar.Behaviour behaviour = new GroundPillar.DistanceTimedBehaviour(1000, 0, 500, 30.0, 20.0, 100.0);
   public final double spawnDistance;
   public final long spawnTime;

   public GroundPillar(int x, int y, double spawnDistance, long spawnTime) {
      super(x, y);
      this.spawnDistance = spawnDistance;
      this.spawnTime = spawnTime;
   }

   public double getHeight(long currentTime, double currentDistance) {
      return this.behaviour.getHeight(currentTime - this.spawnTime, currentDistance - this.spawnDistance);
   }

   public boolean shouldRemove(long currentTime, double currentDistance) {
      return this.behaviour.shouldRemove(currentTime - this.spawnTime, currentDistance - this.spawnDistance);
   }

   public abstract DrawOptions getDrawOptions(Level var1, long var2, double var4, GameCamera var6, PlayerMob var7);

   public abstract static class Behaviour {
      public abstract double getHeight(long var1, double var3);

      public abstract boolean shouldRemove(long var1, double var3);
   }

   public static class DistanceBehaviour extends GroundPillar.Behaviour {
      public double maxDistance;
      public double distanceAppear;
      public double distanceFalloff;

      public DistanceBehaviour(double maxDistance, double distanceAppear, double distanceFalloff) {
         this.maxDistance = maxDistance;
         this.distanceAppear = distanceAppear;
         this.distanceFalloff = distanceFalloff;
      }

      @Override
      public double getHeight(long deltaTime, double deltaDistance) {
         if (deltaDistance < this.distanceAppear) {
            return deltaDistance / this.distanceAppear;
         } else {
            return deltaDistance > this.distanceAppear + this.maxDistance
               ? Math.abs((deltaDistance - (this.distanceAppear + this.maxDistance)) / this.distanceFalloff - 1.0)
               : 1.0;
         }
      }

      @Override
      public boolean shouldRemove(long deltaTime, double deltaDistance) {
         if (deltaDistance < this.distanceAppear) {
            return false;
         } else {
            return deltaDistance < this.distanceAppear + this.distanceFalloff
               ? false
               : deltaDistance > this.distanceAppear + this.maxDistance + this.distanceFalloff;
         }
      }
   }

   public static class DistanceTimedBehaviour extends GroundPillar.Behaviour {
      public int timeout;
      public int timeAppear;
      public int timeDisappear;
      public double maxDistance;
      public double distanceAppear;
      public double distanceFalloff;

      public DistanceTimedBehaviour(int timeout, int timeAppear, int timeDisappear, double maxDistance, double distanceAppear, double distanceFalloff) {
         this.timeout = timeout;
         this.timeAppear = timeAppear;
         this.timeDisappear = timeDisappear;
         this.maxDistance = maxDistance;
         this.distanceAppear = distanceAppear;
         this.distanceFalloff = distanceFalloff;
      }

      @Override
      public double getHeight(long deltaTime, double deltaDistance) {
         if (!(deltaDistance < this.distanceAppear) && deltaTime >= this.timeAppear) {
            double height = 1.0;
            if (deltaDistance > this.distanceAppear + this.maxDistance) {
               height = Math.min(height, Math.abs((deltaDistance - (this.distanceAppear + this.maxDistance)) / this.distanceFalloff - 1.0));
            }

            if (deltaTime > this.timeout + this.timeAppear) {
               if (deltaDistance < this.distanceAppear + this.distanceFalloff) {
                  return Math.abs((deltaDistance - this.distanceAppear) / this.distanceFalloff - 1.0);
               }

               long extraTime = deltaTime - (this.timeout + this.timeAppear);
               height = Math.min(height, Math.abs((double)extraTime / this.timeDisappear - 1.0));
            }

            return height;
         } else {
            return Math.min(deltaDistance / this.distanceAppear, (double)deltaTime / this.timeAppear);
         }
      }

      @Override
      public boolean shouldRemove(long deltaTime, double deltaDistance) {
         if (deltaDistance < this.distanceAppear) {
            return false;
         } else if (deltaDistance < this.distanceAppear + this.distanceFalloff) {
            return false;
         } else {
            return deltaTime > this.timeout + this.timeDisappear + this.timeAppear
               ? true
               : deltaDistance > this.distanceAppear + this.maxDistance + this.distanceFalloff;
         }
      }
   }

   public static class TimedBehaviour extends GroundPillar.Behaviour {
      public int timeout;
      public int timeAppear;
      public int timeDisappear;

      public TimedBehaviour(int timeout, int timeAppear, int timeDisappear) {
         this.timeout = timeout;
         this.timeAppear = timeAppear;
         this.timeDisappear = timeDisappear;
      }

      @Override
      public double getHeight(long deltaTime, double deltaDistance) {
         if (deltaTime < this.timeAppear) {
            return (double)deltaTime / this.timeAppear;
         } else if (deltaTime > this.timeout + this.timeAppear) {
            long extraTime = deltaTime - (this.timeout + this.timeAppear);
            return Math.abs((double)extraTime / this.timeDisappear - 1.0);
         } else {
            return 1.0;
         }
      }

      @Override
      public boolean shouldRemove(long deltaTime, double deltaDistance) {
         return deltaTime > this.timeout + this.timeDisappear + this.timeAppear;
      }
   }
}
