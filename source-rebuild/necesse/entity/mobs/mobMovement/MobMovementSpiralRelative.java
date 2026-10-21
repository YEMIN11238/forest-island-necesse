package necesse.entity.mobs.mobMovement;

import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketRequestMobData;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;

public class MobMovementSpiralRelative extends MobMovementSpiral {
   public int targetUniqueID;
   protected Mob targetMob;
   private long requestTargetMobTime;

   public MobMovementSpiralRelative() {
   }

   public MobMovementSpiralRelative(
      Mob mob, Mob targetMob, float outerRadius, int semiCircles, float radiusDecreasePerSemiCircle, float speed, float startAngle, boolean clockwise
   ) {
      super(mob, outerRadius, semiCircles, radiusDecreasePerSemiCircle, speed, startAngle, clockwise);
      this.targetMob = targetMob;
      this.targetUniqueID = targetMob.getUniqueID();
   }

   public MobMovementSpiralRelative(
      Mob mob, Mob targetMob, float outerRadius, int semiCircles, float radiusDecreasePerSemiCircle, float speed, boolean clockwise
   ) {
      super(mob, targetMob.x, targetMob.y, outerRadius, semiCircles, radiusDecreasePerSemiCircle, speed, clockwise);
      this.targetMob = targetMob;
      this.targetUniqueID = targetMob.getUniqueID();
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextInt(this.targetUniqueID);
      super.setupPacket(mob, writer);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.targetUniqueID = reader.getNextInt();
      super.applyPacket(mob, reader);
   }

   @Override
   public Float getCenterPos() {
      if (this.targetMob == null) {
         return null;
      }

      Rectangle selectBox = this.targetMob.getSelectBox();
      return new Float(selectBox.x + selectBox.width / 2.0F, selectBox.y + selectBox.height / 2.0F);
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

      return super.tick(mob);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementSpiralRelative)) {
         return false;
      }

      MobMovementSpiralRelative other = (MobMovementSpiralRelative)obj;
      return this.targetUniqueID == other.targetUniqueID && super.equals(obj);
   }
}
