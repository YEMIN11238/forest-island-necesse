package necesse.level.gameObject;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ObjectUserMob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.inventory.PlayerInventorySlot;
import necesse.level.maps.Level;

public interface ObjectUsersObject {
   default OEUsers getOEUsersObject(Level level, int tileX, int tileY) {
      ObjectEntity oe = level.entityManager.getObjectEntity(tileX, tileY);
      return oe instanceof OEUsers ? (OEUsers)oe : null;
   }

   default Stream<ObjectUserMob> streamObjectUsers(Level level, int tileX, int tileY) {
      OEUsers oeUsers = this.getOEUsersObject(level, tileX, tileY);
      return oeUsers != null ? oeUsers.streamUsers(level).filter(m -> m instanceof ObjectUserMob).map(m -> (ObjectUserMob)m) : Stream.empty();
   }

   default List<ObjectUserMob> getObjectUsers(Level level, int tileX, int tileY) {
      return this.streamObjectUsers(level, tileX, tileY).collect(Collectors.toList());
   }

   default boolean accelerationCancelsUser(Level level, int tileX, int tileY, Mob mob) {
      return true;
   }

   default boolean canUserAttack(Level level, int tileX, int tileY, PlayerInventorySlot slot, PlayerMob player) {
      return true;
   }

   default boolean canUserInteract(Level level, int tileX, int tileY, PlayerInventorySlot slot, PlayerMob player) {
      return true;
   }

   default int getForcedUserDir(Level level, int tileX, int tileY) {
      return -1;
   }

   default void tickUser(Level level, int tileX, int tileY, Mob mob) {
   }

   void stopUsing(Level var1, int var2, int var3, Mob var4, boolean var5, float var6, float var7);

   void updateUserToExitPos(Level var1, int var2, int var3, Mob var4, float var5, float var6);

   boolean drawsUser(Level var1, int var2, int var3, Mob var4);

   boolean preventsUserPushed(Level var1, int var2, int var3, Mob var4);

   boolean preventsUserLevelInteract(Level var1, int var2, int var3, Mob var4);

   boolean userCanBeTargetedFromAdjacentTiles(Level var1, int var2, int var3, Mob var4);

   Rectangle getUserCollisionBox(Level var1, int var2, int var3, Mob var4, Rectangle var5);

   Rectangle getUserHitBox(Level var1, int var2, int var3, Mob var4, Rectangle var5);

   Rectangle getUserSelectBox(Level var1, int var2, int var3, Mob var4);

   Point getUserAppearancePos(Level var1, int var2, int var3, Mob var4);

   default boolean isValidUser(Level level, int tileX, int tileY, Mob mob) {
      return true;
   }
}
