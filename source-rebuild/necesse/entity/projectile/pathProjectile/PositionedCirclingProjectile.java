package necesse.entity.projectile.pathProjectile;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class PositionedCirclingProjectile extends CirclingProjectile {
   protected float centerX;
   protected float centerY;

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.centerX);
      writer.putNextFloat(this.centerY);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.centerX = reader.getNextFloat();
      this.centerY = reader.getNextFloat();
   }

   @Override
   public Float getCenterPos() {
      return new Float(this.centerX, this.centerY);
   }
}
