package necesse.entity.mobs.mobMovement;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.Mob;

public class MobMovementLevelPos extends MobMovement {
   public float levelX;
   public float levelY;
   private int stuck;
   private float oldX;
   private float oldY;
   public int customMoveAccuracy = -1;

   public MobMovementLevelPos() {
   }

   public MobMovementLevelPos(float levelX, float levelY) {
      this();
      this.levelX = levelX;
      this.levelY = levelY;
   }

   @Override
   public void setupPacket(Mob mob, PacketWriter writer) {
      writer.putNextFloat(this.levelX);
      writer.putNextFloat(this.levelY);
   }

   @Override
   public void applyPacket(Mob mob, PacketReader reader) {
      this.levelX = reader.getNextFloat();
      this.levelY = reader.getNextFloat();
      this.tick(mob);
   }

   @Override
   public boolean tick(Mob mob) {
      if (mob.getLevel().tickManager().isGameTick()) {
         if (mob.getDistance(this.oldX, this.oldY) < 2.0F) {
            this.stuck += 50;
         } else {
            this.stuck = 0;
         }
      }

      float errorValue = this.stuck > 1000 ? 2.0F : (this.customMoveAccuracy == -1 ? mob.moveAccuracy : this.customMoveAccuracy);
      boolean arrived = false;
      if (this.moveTo(mob, this.levelX, this.levelY, errorValue)) {
         this.stuck = 0;
         arrived = true;
      }

      if (mob.getLevel().tickManager().isGameTick()) {
         this.oldX = mob.x;
         this.oldY = mob.y;
      }

      return arrived;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof MobMovementLevelPos)) {
         return false;
      }

      MobMovementLevelPos other = (MobMovementLevelPos)obj;
      return this.levelX == other.levelX && this.levelY == other.levelY;
   }
}
