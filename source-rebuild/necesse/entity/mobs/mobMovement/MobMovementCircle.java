package necesse.entity.mobs.mobMovement;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;

public abstract class MobMovementCircle extends MobMovement {
   public int range;
   public float speed;
   public float angleOffset;
   public boolean reversed;

   public MobMovementCircle() {
   }

   public MobMovementCircle(Mob mob, int range, float speed, float angleOffset, boolean reversed) {
      this();
      this.range = range;
      this.speed = speed;
      this.angleOffset = angleOffset;
      this.reversed = reversed;
   }

   public MobMovementCircle(Mob mob, float centerX, float centerY, int range, float speed, boolean reversed) {
      this(mob, range, speed, 0.0F, reversed);
      float startAngle = getTimeAngle(mob.getTime(), speed, reversed);
      Float dir = GameMath.normalize(centerX - mob.getX(), centerY - mob.getY());
      this.angleOffset = GameMath.fixAngle((float)Math.toDegrees(Math.atan2(dir.y, dir.x)) - 90.0F - startAngle);
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextInt(this.range);
      writer.putNextFloat(this.speed);
      writer.putNextFloat(this.angleOffset);
      writer.putNextBoolean(this.reversed);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.range = reader.getNextInt();
      this.speed = reader.getNextFloat();
      this.angleOffset = reader.getNextFloat();
      this.reversed = reader.getNextBoolean();
      this.tick(mob);
   }

   public abstract Float getCenterPos();

   public float getCurrentAngle(Mob mob) {
      return getTimeAngle(mob.getWorldEntity().getTime(), this.speed, this.reversed) + this.angleOffset;
   }

   public Float getCurrentPos(Mob mob) {
      Float centerPos = this.getCenterPos();
      if (centerPos != null) {
         Float p = getOffsetPositionFloat(mob, this.range, this.speed, this.angleOffset, this.reversed);
         return new Float(centerPos.x + p.x, centerPos.y + p.y);
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

      return false;
   }

   public static float convertToRotSpeed(int range, float mobSpeed) {
      float circumference = (float)(range * 2 * Math.PI);
      float secondsPerRotation = circumference / mobSpeed;
      return 10.0F / secondsPerRotation * 4.0F;
   }

   public static Point getOffsetPosition(Mob mob, int range, float speed, float angleOffset, boolean reversed) {
      Float p = getOffsetPositionFloat(mob, range, speed, angleOffset, reversed);
      return new Point((int)p.x, (int)p.y);
   }

   public static Float getOffsetPositionFloat(Mob mob, int range, float speed, float angleOffset, boolean reversed) {
      float angle = getTimeAngle(mob.getWorldEntity().getTime(), speed, reversed);
      angle += angleOffset;
      double cos = GameMath.cos(angle);
      double sin = GameMath.sin(angle);
      return new Float((float)(sin * range), (float)(-(cos * range)));
   }

   public static float getTimeAngle(long time, float speed, boolean reversed) {
      float angle = (float)(time / 1000.0 * speed * 36.0 % 360.0);
      return reversed ? -angle : angle;
   }

   public static float getTimeToAngle(float speed, float angle) {
      return angle / (speed * 36.0F) * 1000.0F;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementCircle)) {
         return false;
      }

      MobMovementCircle other = (MobMovementCircle)obj;
      return this.range == other.range && this.speed == other.speed && this.angleOffset == other.angleOffset && this.reversed == other.reversed;
   }
}
