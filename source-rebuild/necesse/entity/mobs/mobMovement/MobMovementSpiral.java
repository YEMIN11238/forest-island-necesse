package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;

public abstract class MobMovementSpiral extends MobMovement {
   public long startTime;
   public float outerRadius;
   public int semiCircles;
   public float radiusDecreasePerSemiCircle;
   public float speed;
   public float startAngle;
   public boolean clockwise;
   public int currentSemiCircle;
   public long currentTimePassed;

   public static int getSemiCircles(float outerRadius, float radiusDecreasePerSemiCircle, float minInnerRadius) {
      float totalRadiusDecrease = outerRadius - minInnerRadius;
      return (int)(totalRadiusDecrease / radiusDecreasePerSemiCircle);
   }

   public MobMovementSpiral() {
   }

   public MobMovementSpiral(Mob mob, float outerRadius, int semiCircles, float radiusDecreasePerSemiCircle, float speed, float startAngle, boolean clockwise) {
      this();
      this.startTime = mob.getWorldEntity().getTime();
      this.outerRadius = outerRadius;
      this.semiCircles = semiCircles;
      this.radiusDecreasePerSemiCircle = radiusDecreasePerSemiCircle;
      this.speed = speed;
      this.startAngle = startAngle;
      this.clockwise = clockwise;
   }

   public MobMovementSpiral(
      Mob mob, float centerX, float centerY, float outerRadius, int semiCircles, float radiusDecreasePerSemiCircle, float speed, boolean clockwise
   ) {
      this(mob, outerRadius, semiCircles, radiusDecreasePerSemiCircle, speed, 0.0F, clockwise);
      Float dir = GameMath.normalize(centerX - mob.x, centerY - mob.y);
      this.startAngle = GameMath.fixAngle(GameMath.getAngle(dir) - 90.0F);
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextLong(this.startTime);
      writer.putNextFloat(this.outerRadius);
      writer.putNextInt(this.semiCircles);
      writer.putNextFloat(this.radiusDecreasePerSemiCircle);
      writer.putNextFloat(this.speed);
      writer.putNextFloat(this.startAngle);
      writer.putNextBoolean(this.clockwise);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.startTime = reader.getNextLong();
      this.outerRadius = reader.getNextFloat();
      this.semiCircles = reader.getNextInt();
      this.radiusDecreasePerSemiCircle = reader.getNextFloat();
      this.speed = reader.getNextFloat();
      this.startAngle = reader.getNextFloat();
      this.clockwise = reader.getNextBoolean();
      this.tick(mob);
   }

   public abstract Float getCenterPos();

   public float getCurrentRadius() {
      return Math.abs(this.outerRadius - this.currentSemiCircle * this.radiusDecreasePerSemiCircle);
   }

   public Float getCurrentPos(Mob mob) {
      Float centerPos = this.getCenterPos();
      if (centerPos == null) {
         return null;
      }

      while (true) {
         float currentRadius = this.getCurrentRadius();
         int timeForCurrentSemiCircle = this.getTimeForSemiCircle(currentRadius);
         long timeSinceStart = mob.getWorldEntity().getTime() - this.startTime;
         long timeAlongCurrentSemiCircle = timeSinceStart - this.currentTimePassed;
         if (timeAlongCurrentSemiCircle <= timeForCurrentSemiCircle) {
            float percentAlongSemiCircle = (float)timeAlongCurrentSemiCircle / timeForCurrentSemiCircle;
            float currentAngle = percentAlongSemiCircle * 180.0F;
            if (this.currentSemiCircle % 2 != 0) {
               currentAngle += 180.0F;
            }

            if (!this.clockwise) {
               currentAngle = -currentAngle;
            }

            currentAngle = this.startAngle + currentAngle;
            double cos = GameMath.cos(currentAngle);
            double sin = GameMath.sin(currentAngle);
            return new Float(centerPos.x + (float)(sin * currentRadius), centerPos.y + (float)(-(cos * currentRadius)));
         }

         this.currentTimePassed += timeForCurrentSemiCircle;
         this.currentSemiCircle++;
      }
   }

   @Override
   public boolean tick(Mob mob) {
      Float pos = this.getCurrentPos(mob);
      if (pos != null) {
         this.moveTo(mob, pos.x, pos.y, mob.moveAccuracy);
      }

      return this.currentSemiCircle > this.semiCircles;
   }

   public static int getTimeForSemiCircle(float radius, float speed) {
      float circumference = (float)(radius * Math.PI);
      float distanceMovedPerSecond = 1000.0F * speed / 250.0F;
      float secondsToMoveCircumference = circumference / distanceMovedPerSecond;
      return (int)(secondsToMoveCircumference * 1000.0F);
   }

   public int getTimeForSemiCircle(float radius) {
      return getTimeForSemiCircle(radius, this.speed);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementSpiral)) {
         return false;
      }

      MobMovementSpiral other = (MobMovementSpiral)obj;
      return this.startTime == other.startTime
         && this.outerRadius == other.outerRadius
         && this.semiCircles == other.semiCircles
         && this.radiusDecreasePerSemiCircle == other.radiusDecreasePerSemiCircle
         && this.speed == other.speed
         && this.startAngle == other.startAngle
         && this.clockwise == other.clockwise;
   }
}
