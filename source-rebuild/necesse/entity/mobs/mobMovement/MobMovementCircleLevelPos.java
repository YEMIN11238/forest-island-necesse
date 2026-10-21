package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.Mob;

public class MobMovementCircleLevelPos extends MobMovementCircle {
   public float centerX;
   public float centerY;

   public MobMovementCircleLevelPos() {
   }

   public MobMovementCircleLevelPos(Mob mob, float centerX, float centerY, int range, float speed, float angleOffset, boolean reversed) {
      super(mob, range, speed, angleOffset, reversed);
      this.centerX = centerX;
      this.centerY = centerY;
   }

   public MobMovementCircleLevelPos(Mob mob, float centerX, float centerY, int range, float speed, float offsetCenterX, float offsetCenterY, boolean reversed) {
      super(mob, offsetCenterX, offsetCenterY, range, speed, reversed);
      this.centerX = centerX;
      this.centerY = centerY;
   }

   public MobMovementCircleLevelPos(Mob mob, float centerX, float centerY, int range, float speed, boolean reversed) {
      super(mob, centerX, centerY, range, speed, reversed);
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
      if (!(obj instanceof MobMovementCircleLevelPos)) {
         return false;
      }

      MobMovementCircleLevelPos other = (MobMovementCircleLevelPos)obj;
      return this.centerX == other.centerX && this.centerY == other.centerY && super.equals(obj);
   }
}
