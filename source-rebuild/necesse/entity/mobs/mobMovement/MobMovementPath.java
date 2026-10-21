package necesse.entity.mobs.mobMovement;

import java.util.function.BiFunction;
import java.util.function.Function;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;

public abstract class MobMovementPath extends MobMovement {
   public MobMovementPath.Mode mode;
   public long startTime;
   public int totalTimeItShouldTake;
   private int calculateTimeIterations = -1;

   public MobMovementPath() {
   }

   public MobMovementPath(MobMovementPath.Mode mode, long startTime, int totalTimeItShouldTake) {
      this();
      this.mode = mode;
      this.startTime = startTime;
      this.totalTimeItShouldTake = totalTimeItShouldTake;
   }

   public MobMovementPath(MobMovementPath.Mode mode, Mob mob, int iterations, int extraTime) {
      this(mode, mob.getTime(), extraTime);
      if (iterations < 0) {
         throw new IllegalArgumentException("Cannot calculate time taken with 0 or less iterations");
      }

      this.calculateTimeIterations = iterations;
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextEnum(this.mode);
      writer.putNextLong(this.startTime);
      writer.putNextInt(this.totalTimeItShouldTake);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.mode = reader.getNextEnum(MobMovementPath.Mode.class);
      this.startTime = reader.getNextLong();
      this.totalTimeItShouldTake = reader.getNextInt();
      this.tick(mob);
   }

   public abstract java.awt.geom.Point2D.Float getCenterPos();

   public abstract java.awt.geom.Point2D.Float getPosOffset(Mob var1, float var2);

   public java.awt.geom.Point2D.Float getCurrentPos(Mob mob) {
      if (this.calculateTimeIterations > 0) {
         this.totalTimeItShouldTake = calculateTotalTimeItShouldTake(
               mob.getSpeed(), this.calculateTimeIterations, percentProgress -> this.getPosOffset(mob, percentProgress)
            )
            + this.totalTimeItShouldTake;
         this.calculateTimeIterations = -1;
      }

      java.awt.geom.Point2D.Float centerPos = this.getCenterPos();
      if (centerPos != null) {
         long timeSinceStart = mob.getTime() - this.startTime;
         float progress = this.mode.progressFunction.apply(timeSinceStart, this.totalTimeItShouldTake);
         java.awt.geom.Point2D.Float posOffset = this.getPosOffset(mob, progress);
         return new java.awt.geom.Point2D.Float(centerPos.x + posOffset.x, centerPos.y + posOffset.y);
      } else {
         return null;
      }
   }

   @Override
   public boolean tick(Mob mob) {
      java.awt.geom.Point2D.Float pos = this.getCurrentPos(mob);
      if (pos != null) {
         this.moveTo(mob, pos.x, pos.y, mob.moveAccuracy);
      }

      return mob.getTime() >= this.startTime + this.totalTimeItShouldTake;
   }

   public static float calculateTotalDistance(int iterations, Function<Float, java.awt.geom.Point2D.Float> positionFunction) {
      java.awt.geom.Point2D.Float lastPos = positionFunction.apply(0.0F);
      double totalDistance = 0.0;

      for (int i = 1; i <= iterations; i++) {
         float currentProgress = (float)i / iterations;
         java.awt.geom.Point2D.Float nextPosition = positionFunction.apply(currentProgress);
         double distanceToNextPosition = lastPos.distance(nextPosition);
         totalDistance += distanceToNextPosition;
         lastPos = nextPosition;
      }

      return (float)totalDistance;
   }

   public static int calculateTotalTimeItShouldTake(float mobSpeed, int iterations, Function<Float, java.awt.geom.Point2D.Float> positionFunction) {
      return (int)Entity.getTravelTimeMillis(mobSpeed, calculateTotalDistance(iterations, positionFunction));
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementPath)) {
         return false;
      }

      MobMovementPath other = (MobMovementPath)obj;
      return this.mode == other.mode && this.startTime == other.startTime && this.totalTimeItShouldTake == other.totalTimeItShouldTake;
   }

   public enum Mode {
      UNLIMITED((timeSinceStart, totalTimeItShouldTake) -> (float)timeSinceStart.longValue() / totalTimeItShouldTake.intValue()),
      LIMITED((timeSinceStart, totalTimeItShouldTake) -> GameMath.limit((float)timeSinceStart.longValue() / totalTimeItShouldTake.intValue(), 0.0F, 1.0F)),
      LOOP((timeSinceStart, totalTimeItShouldTake) -> (float)GameMath.floorMod((double)timeSinceStart.longValue() / totalTimeItShouldTake.intValue(), 1.0)),
      BOUNCE_LOOP((timeSinceStart, totalTimeItShouldTake) -> {
         float progress = (float)GameMath.floorMod((double)timeSinceStart.longValue() / totalTimeItShouldTake.intValue(), 2.0);
         if (progress > 1.0F) {
            progress = 1.0F - (progress - 1.0F);
         }

         return progress;
      });

      public final BiFunction<Long, Integer, Float> progressFunction;

      Mode(BiFunction<Long, Integer, Float> progressFunction) {
         this.progressFunction = progressFunction;
      }
   }
}
