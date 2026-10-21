package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.pathProjectile.SurvivorWhipProjectile;

public abstract class MobMovementWhipPath extends MobMovementPath {
   public float dirX;
   public float dirY;
   public float distance;

   public MobMovementWhipPath() {
   }

   public MobMovementWhipPath(MobMovementPath.Mode mode, long startTime, int totalTimeItShouldTake, float dirX, float dirY, float distance) {
      super(mode, startTime, totalTimeItShouldTake);
      Float normalize = GameMath.normalize(dirX, dirY);
      this.dirX = normalize.x;
      this.dirY = normalize.y;
      this.distance = distance;
   }

   public MobMovementWhipPath(MobMovementPath.Mode mode, Mob mob, int iterations, int extraTime, float dirX, float dirY, float distance) {
      super(mode, mob, iterations, extraTime);
      Float normalize = GameMath.normalize(dirX, dirY);
      this.dirX = normalize.x;
      this.dirY = normalize.y;
      this.distance = distance;
   }

   @Override
   public Float getPosOffset(Mob mob, float percentProgress) {
      Float currentPos = SurvivorWhipProjectile.getWhipPositionFloat(percentProgress);
      float forwardRange = GameMath.lerp(percentProgress, this.distance, Math.max(1.0F, this.distance - this.distance / 4.0F));
      float perpRange = GameMath.lerp(percentProgress, Math.max(1.0F, this.distance - this.distance / 5.0F), this.distance) / 1.5F;
      float currentX = currentPos.x * this.dirX * forwardRange;
      float currentY = currentPos.x * this.dirY * forwardRange;
      Float perpDir = GameMath.getPerpendicularDir(this.dirX, this.dirY);
      currentX += currentPos.y * perpDir.x * perpRange * (this.dirX < 0.0F ? 1 : -1);
      currentY += currentPos.y * perpDir.y * perpRange * (this.dirX < 0.0F ? 1 : -1);
      return new Float(currentX, currentY);
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      super.setupPacket(mob, writer);
      writer.putNextFloat(this.dirX * this.distance);
      writer.putNextFloat(this.dirY * this.distance);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      super.applyPacket(mob, reader);
      float dirX = reader.getNextFloat();
      float dirY = reader.getNextFloat();
      this.distance = (float)Math.sqrt(dirX * dirX + dirY * dirY);
      this.dirX = dirX / this.distance;
      this.dirY = dirY / this.distance;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementWhipPath)) {
         return false;
      }

      MobMovementWhipPath other = (MobMovementWhipPath)obj;
      return this.dirX == other.dirX && this.dirY == other.dirY && this.distance == other.distance && super.equals(other);
   }
}
