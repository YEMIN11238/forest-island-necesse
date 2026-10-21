package necesse.entity.projectile.pathProjectile;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;

public abstract class CirclingProjectile extends PathProjectile {
   protected float currentAngle;

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.currentAngle);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.currentAngle = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      Float pos = this.getAnglePosition();
      this.x = pos.x;
      this.y = pos.y;
   }

   @Override
   public Float getPosition(double dist) {
      this.currentAngle = (float)(this.currentAngle + dist / ((Math.PI * 2) * this.getRadius()) * 360.0);
      return this.getAnglePosition();
   }

   public Float getAnglePosition() {
      float x = this.rotatesClockwise() ? -GameMath.sin(this.currentAngle) : GameMath.sin(this.currentAngle);
      float y = GameMath.cos(this.currentAngle);
      Float center = this.getCenterPos();
      float radius = this.getRadius();
      return new Float(center.x + x * radius, center.y + y * radius);
   }

   public abstract Float getCenterPos();

   public abstract float getRadius();

   public boolean rotatesClockwise() {
      return true;
   }
}
