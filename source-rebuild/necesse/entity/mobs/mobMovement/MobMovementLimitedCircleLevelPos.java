package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.Mob;

public class MobMovementLimitedCircleLevelPos extends MobMovementLimitedCircle {
   public float centerX;
   public float centerY;

   public MobMovementLimitedCircleLevelPos() {
   }

   public MobMovementLimitedCircleLevelPos(
      float centerX, float centerY, long startTime, int startTimeOffset, float startRadius, float endRadiusOffset, float startAngle, float endAngleOffset
   ) {
      super(startTime, startTimeOffset, startRadius, endRadiusOffset, startAngle, endAngleOffset);
      this.centerX = centerX;
      this.centerY = centerY;
   }

   public MobMovementLimitedCircleLevelPos(
      Mob mob, int extraTime, float centerX, float centerY, float startRadius, float endRadiusOffset, float startAngle, float endAngleOffset
   ) {
      super(mob, extraTime, startRadius, endRadiusOffset, startAngle, endAngleOffset);
      this.centerX = centerX;
      this.centerY = centerY;
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextFloat(this.centerX);
      writer.putNextFloat(this.centerY);
      super.setupPacket(mob, writer);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.centerX = reader.getNextFloat();
      this.centerY = reader.getNextFloat();
      super.applyPacket(mob, reader);
   }

   @Override
   public Float getCenterPos() {
      return new Float(this.centerX, this.centerY);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementLimitedCircleLevelPos)) {
         return false;
      }

      MobMovementLimitedCircleLevelPos other = (MobMovementLimitedCircleLevelPos)obj;
      return this.centerX == other.centerX && this.centerY == other.centerY && super.equals(obj);
   }
}
