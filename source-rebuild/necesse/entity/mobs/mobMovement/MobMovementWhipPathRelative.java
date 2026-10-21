package necesse.entity.mobs.mobMovement;

import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketRequestMobData;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;

public class MobMovementWhipPathRelative extends MobMovementWhipPath {
   public int targetUniqueID;
   public float relativeX;
   public float relativeY;
   protected Mob targetMob;
   private long requestTargetMobTime;

   public MobMovementWhipPathRelative() {
   }

   public MobMovementWhipPathRelative(
      MobMovementPath.Mode mode, long startTime, int totalTimeItShouldTake, Mob targetMob, int relativeX, int relativeY, float dirX, float dirY, float distance
   ) {
      super(mode, startTime, totalTimeItShouldTake, dirX, dirY, distance);
      this.targetMob = targetMob;
      this.targetUniqueID = targetMob.getUniqueID();
      this.relativeX = relativeX;
      this.relativeY = relativeY;
   }

   public MobMovementWhipPathRelative(
      MobMovementPath.Mode mode, Mob mob, int iterations, int extraTime, Mob targetMob, int relativeX, int relativeY, float dirX, float dirY, float distance
   ) {
      super(mode, mob, iterations, extraTime, dirX, dirY, distance);
      this.targetMob = targetMob;
      this.targetUniqueID = targetMob.getUniqueID();
      this.relativeX = relativeX;
      this.relativeY = relativeY;
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextInt(this.targetUniqueID);
      writer.putNextFloat(this.relativeX);
      writer.putNextFloat(this.relativeY);
      super.setupPacket(mob, writer);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.targetUniqueID = reader.getNextInt();
      this.relativeX = reader.getNextFloat();
      this.relativeY = reader.getNextFloat();
      super.applyPacket(mob, reader);
   }

   @Override
   public Float getCenterPos() {
      return this.targetMob == null ? null : new Float(this.targetMob.x + this.relativeX, this.targetMob.y + this.relativeY);
   }

   @Override
   public boolean tick(Mob mob) {
      if (this.targetMob == null || this.targetMob.getUniqueID() != this.targetUniqueID) {
         this.targetMob = GameUtils.getLevelMob(this.targetUniqueID, mob.getLevel());
         if (this.targetMob == null && mob.isClient() && this.requestTargetMobTime < mob.getTime()) {
            mob.getClient().network.sendPacket(new PacketRequestMobData(this.targetUniqueID));
            this.requestTargetMobTime = mob.getTime() + 1000L;
         }
      }

      return super.tick(mob);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementWhipPathRelative)) {
         return false;
      }

      MobMovementWhipPathRelative other = (MobMovementWhipPathRelative)obj;
      return this.targetUniqueID == other.targetUniqueID && this.relativeX == other.relativeX && this.relativeY == other.relativeY && super.equals(obj);
   }
}
