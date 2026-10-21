package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketRequestMobData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;

public class MobMovementCharge extends MobMovement {
   public int targetUniqueID;
   public float distanceOffset;
   public boolean stopWhenColliding;
   protected MobMovementLevelPos mover = new MobMovementLevelPos();
   protected Mob targetMob;
   private long requestTargetMobTime;

   public MobMovementCharge() {
   }

   public MobMovementCharge(Mob target, float distanceOffset, boolean stopWhenColliding) {
      this();
      this.targetMob = target;
      this.targetUniqueID = target.getUniqueID();
      this.distanceOffset = distanceOffset;
      this.stopWhenColliding = stopWhenColliding;
   }

   public MobMovementCharge(Mob target, float distanceOffset) {
      this(target, distanceOffset, false);
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextInt(this.targetUniqueID);
      writer.putNextFloat(this.distanceOffset);
      writer.putNextBoolean(this.stopWhenColliding);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.targetUniqueID = reader.getNextInt();
      this.distanceOffset = reader.getNextFloat();
      this.stopWhenColliding = reader.getNextBoolean();
   }

   @Override
   public boolean tick(Mob mob) {
      if (this.targetMob == null || this.targetMob.getUniqueID() != this.targetUniqueID) {
         this.targetMob = GameUtils.getLevelMob(this.targetUniqueID, mob.getLevel());
         if (this.targetMob == null && mob.isClient() && this.requestTargetMobTime < mob.getWorldEntity().getTime()) {
            mob.getLevel().getClient().network.sendPacket(new PacketRequestMobData(this.targetUniqueID));
            this.requestTargetMobTime = mob.getWorldEntity().getTime() + 1000L;
         }
      }

      if (this.targetMob != null) {
         Float dir = GameMath.normalize(this.targetMob.x - mob.x, this.targetMob.y - mob.y);
         this.mover.levelX = this.targetMob.x + dir.x * this.distanceOffset;
         this.mover.levelY = this.targetMob.y + dir.y * this.distanceOffset;
         return this.stopWhenColliding && this.targetMob.getCollision().intersects(mob.getCollision()) ? true : this.mover.tick(mob);
      } else {
         return false;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementCharge)) {
         return false;
      }

      MobMovementCharge other = (MobMovementCharge)obj;
      return this.targetUniqueID == other.targetUniqueID && this.distanceOffset == other.distanceOffset && this.stopWhenColliding == other.stopWhenColliding;
   }

   @Override
   public String toString() {
      String mobString = this.targetMob == null ? "" + this.targetUniqueID : this.targetMob.getStringID() + "@" + this.targetUniqueID;
      return "MobMovementCharge@" + Integer.toHexString(this.hashCode()) + "[" + mobString + ", " + GameMath.toDecimals(this.distanceOffset, 2) + "]";
   }
}
