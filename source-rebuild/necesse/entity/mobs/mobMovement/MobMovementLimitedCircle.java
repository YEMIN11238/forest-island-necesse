package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.entity.Entity;
import necesse.entity.mobs.Mob;

public abstract class MobMovementLimitedCircle extends MobMovement {
   public long startTime;
   public int startTimeOffset;
   public float startRadius;
   public float endRadiusOffset;
   public float startAngle;
   public float endAngleOffset;

   public MobMovementLimitedCircle() {
   }

   public MobMovementLimitedCircle(long startTime, int startTimeOffset, float startRadius, float endRadiusOffset, float startAngle, float endAngleOffset) {
      this();
      this.startTime = startTime;
      this.startTimeOffset = startTimeOffset;
      this.startRadius = startRadius;
      this.endRadiusOffset = endRadiusOffset;
      this.startAngle = startAngle;
      this.endAngleOffset = endAngleOffset;
   }

   public MobMovementLimitedCircle(Mob mob, int extraTime, float startRadius, float endRadiusOffset, float startAngle, float endAngleOffset) {
      this(mob.getTime(), 0, startRadius, endRadiusOffset, startAngle, endAngleOffset);
      float averageRadius = startRadius + endRadiusOffset / 2.0F;
      this.startTimeOffset = convertMobSpeedAndRotationToTime(averageRadius, mob.getSpeed(), endAngleOffset) + extraTime;
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextLong(this.startTime);
      writer.putNextInt(this.startTimeOffset);
      writer.putNextFloat(this.startRadius);
      writer.putNextFloat(this.endRadiusOffset);
      writer.putNextFloat(this.startAngle);
      writer.putNextFloat(this.endAngleOffset);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.startTime = reader.getNextLong();
      this.startTimeOffset = reader.getNextInt();
      this.startRadius = reader.getNextFloat();
      this.endRadiusOffset = reader.getNextFloat();
      this.startAngle = reader.getNextFloat();
      this.endAngleOffset = reader.getNextFloat();
      this.tick(mob);
   }

   public abstract Float getCenterPos();

   public Float getCurrentPos(Mob mob) {
      Float centerPos = this.getCenterPos();
      if (centerPos != null) {
         long currentTime = GameMath.limit(mob.getTime(), this.startTime, this.startTime + this.startTimeOffset);
         float timeProgressPercent = this.startTimeOffset <= 0 ? 1.0F : (float)(currentTime - this.startTime) / this.startTimeOffset;
         float currentAngle = this.startAngle + this.endAngleOffset * timeProgressPercent;
         float currentRadius = this.startRadius + this.endRadiusOffset * timeProgressPercent;
         Float dir = GameMath.getAngleDir(currentAngle);
         return new Float(centerPos.x + dir.x * currentRadius, centerPos.y + dir.y * currentRadius);
      } else {
         return null;
      }
   }

   @Override
   public boolean tick(Mob mob) {
      Float pos = this.getCurrentPos(mob);
      if (pos != null) {
         this.moveTo(mob, pos.x, pos.y, mob.moveAccuracy);
      }

      return mob.getTime() >= this.startTime + this.startTimeOffset;
   }

   public static int convertMobSpeedAndRotationToTime(float radius, float mobSpeed, float angleOffset) {
      float circumference = (float)(radius * 2.0F * Math.PI) * (Math.abs(angleOffset) / 360.0F);
      return (int)Entity.getTravelTimeMillis(mobSpeed, circumference);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementLimitedCircle)) {
         return false;
      }

      MobMovementLimitedCircle other = (MobMovementLimitedCircle)obj;
      return this.startTime == other.startTime
         && this.startTimeOffset == other.startTimeOffset
         && this.startRadius == other.startRadius
         && this.endRadiusOffset == other.endRadiusOffset
         && this.startAngle == other.startAngle
         && this.endAngleOffset == other.endAngleOffset;
   }
}
