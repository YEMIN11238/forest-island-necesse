package necesse.entity.objectEntity;

import java.awt.Point;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ObjectUserMob;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.level.gameObject.ObjectUsersObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;

public class BedObjectEntity extends ObjectEntity implements OEUsers {
   public final OEUsers.Users users = this.constructUsersObject(2000L);

   public BedObjectEntity(Level level, int x, int y) {
      super(level, "bed", x, y);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      this.users.writeUsersSpawnPacket(writer);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      this.users.readUsersSpawnPacket(reader, this);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.users.serverTick(this);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.users.clientTick(this);
   }

   @Override
   public GameMessage getCanUseError(Mob mob) {
      return this.isInUse() ? new LocalMessage("misc", "bedinuse") : mob.getRiderDismountError(null);
   }

   @Override
   public OEUsers.Users getUsersObject() {
      return this.users;
   }

   @Override
   public void updateUserPosition(Mob mob) {
      mob.setPos(this.tileX * 32 + 16, this.tileY * 32 + 16, true);
   }

   @Override
   public void updateUserToExitPos(Mob mob, float dirX, float dirY) {
      Level mobLevel = mob.getLevel();
      LevelIdentifier mobLevelIdentifier = mobLevel == null ? null : mobLevel.getIdentifier();
      if (mobLevelIdentifier != null && this.getLevel().getIdentifier().equals(mobLevelIdentifier)) {
         LevelObject levelObject = this.getLevelObject();
         Point exitPos = OEUsers.findExitPos(
            levelObject,
            mob,
            dirX,
            dirY,
            comparator -> comparator.thenComparing(
               (p1, p2) -> levelObject.rotation != 0 && levelObject.rotation != 2
                  ? Math.abs(GameMath.getTileCoordinate(p1.x) - this.tileX) - Math.abs(GameMath.getTileCoordinate(p2.x) - this.tileX)
                  : Math.abs(GameMath.getTileCoordinate(p1.y) - this.tileY) - Math.abs(GameMath.getTileCoordinate(p2.y) - this.tileY)
            )
         );
         if (exitPos != null) {
            mob.setPos(exitPos.x, exitPos.y, true);
            mob.sendMovementPacket(true);
         }
      }
   }

   @Override
   public void onUsageChanged(Mob mob, boolean using) {
      if (mob instanceof ObjectUserMob) {
         LevelObject levelObject = this.getLevelObject();
         if (using) {
            if (!this.removed()) {
               ((ObjectUserMob)mob).startUsingObject(levelObject, (ObjectUsersObject)levelObject.object, this);
            }
         } else {
            ((ObjectUserMob)mob).clearUsingObject(this.getLevel(), this.tileX, this.tileY);
            mob.buffManager.removeBuff(BuffRegistry.SLEEPING, mob.isServer());
         }
      }
   }

   @Override
   public void onIsInUseChanged(boolean isInUse) {
   }

   @Override
   public void remove() {
      super.remove();
      this.users.onRemoved(this);
   }
}
