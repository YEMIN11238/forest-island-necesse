package necesse.level.gameObject;

import java.awt.Point;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;

public interface RespawnObject {
   boolean canRespawn(Level var1, int var2, int var3, ServerClient var4);

   default boolean isCurrentSpawn(Level level, int tileX, int tileY, ServerClient client) {
      return client.spawnLevelIdentifier.equals(level.getIdentifier()) && client.spawnTile.x == tileX && client.spawnTile.y == tileY;
   }

   default boolean removeSpawn(Level level, int tileX, int tileY, ServerClient client, boolean sendChatMessage) {
      if (this.isCurrentSpawn(level, tileX, tileY, client)) {
         client.resetSpawnPoint(level.getServer());
         if (sendChatMessage) {
            client.sendChatMessage(new LocalMessage("misc", "spawnremoved"));
         }

         return true;
      } else {
         return false;
      }
   }

   default void setSpawn(Level level, int tileX, int tileY, ServerClient client, boolean sendChatMessage) {
      if (client.achievementsLoaded()) {
         client.achievements().SET_SPAWN.markCompleted(client);
      }

      client.spawnLevelIdentifier = level.getIdentifier();
      client.spawnTile = new Point(tileX, tileY);
      if (sendChatMessage) {
         client.sendChatMessage(new LocalMessage("misc", "spawnset"));
      }
   }

   Point getSpawnOffset(Level var1, int var2, int var3, ServerClient var4);

   static Point calculateSpawnOffset(Level level, int tileX, int tileY, ServerClient client) {
      if (level == null) {
         return null;
      } else {
         level.regionManager.ensureTilesAreLoaded(tileX - 2, tileY - 2, tileX + 2, tileY + 2);
         GameObject spawnObject = level.getObject(tileX, tileY);
         if (spawnObject instanceof RespawnObject) {
            return !((RespawnObject)spawnObject).canRespawn(level, tileX, tileY, client)
               ? null
               : ((RespawnObject)spawnObject).getSpawnOffset(level, tileX, tileY, client);
         } else {
            return null;
         }
      }
   }
}
