package necesse.entity.objectEntity;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketChangeObject;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.ComputedFunction;
import necesse.engine.util.LevelIdentifier;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class LadderDownObjectEntity extends PortalObjectEntity {
   private final int ladderDownID;
   private final int ladderUpID;

   public LadderDownObjectEntity(String type, Level level, int x, int y, LevelIdentifier destinationLevelIdentifier, int ladderDownID, int ladderUpID) {
      super(level, type, x, y, level.getIdentifier(), x, y);
      this.destinationIdentifier = destinationLevelIdentifier;
      this.ladderDownID = ladderDownID;
      this.ladderUpID = ladderUpID;
      this.saveDestination = false;
   }

   @Override
   public void use(Server server, ServerClient client) {
      ComputedFunction<Level, GameMessage> isBlockingExit = new ComputedFunction<>(
         levelx -> {
            levelx.regionManager.ensureTilesAreLoaded(this.destinationTileX, this.destinationTileY, this.destinationTileX, this.destinationTileY);
            return levelx.getObjectID(this.destinationTileX, this.destinationTileY) != this.ladderUpID
               ? levelx.preventsLadderPlacement(this.destinationTileX, this.destinationTileY)
               : null;
         }
      );
      if (server.world.levelManager.isLoaded(this.getDestinationIdentifier())) {
         Level level = server.world.getLevel(this.getDestinationIdentifier());
         GameMessage error = isBlockingExit.get(level);
         if (error != null) {
            client.sendChatMessage(error);
            return;
         }
      }

      this.teleportClientToAroundDestination(client, levelx -> {
         if (!isBlockingExit.isComputed()) {
            GameMessage errorx = isBlockingExit.get(levelx);
            if (errorx != null) {
               client.sendChatMessage(errorx);
               return false;
            }
         }

         levelx.regionManager.ensureTileIsLoaded(this.destinationTileX, this.destinationTileY);
         if (levelx.getObjectID(this.destinationTileX, this.destinationTileY) != this.ladderUpID) {
            clearAndPlaceLadder(server, levelx, this.destinationTileX, this.destinationTileY, this.ladderUpID, true);
         }

         client.newStats.ladders_used.increment(1);
         this.runClearMobs(levelx, this.destinationTileX, this.destinationTileY);
         return true;
      }, true);
   }

   public static void clearAndPlaceLadder(Server server, Level level, int tileX, int tileY, int ladderObjectID, boolean clearLiquid) {
      GameObject ladderObject = ObjectRegistry.getObject(ladderObjectID);

      for (int i = -1; i <= 1; i++) {
         int currentTileX = tileX + i;

         for (int j = -1; j <= 1; j++) {
            int currentTileY = tileY + j;
            level.regionManager.ensureTileIsLoaded(currentTileX, currentTileY);
            GameObject obj = level.getObject(currentTileX, currentTileY);
            boolean shouldClearObject = obj.isClearedOnLadderPlacement(level, currentTileX, currentTileY);
            if (i == 0 && j == 0) {
               if (!shouldClearObject) {
                  level.entityManager.destroyObjectOverride(0, currentTileX, currentTileY);
               }

               ladderObject.placeObject(level, currentTileX, currentTileY, 0, false);
               if (level.getTile(currentTileX, currentTileY).isLiquid) {
                  level.setTile(currentTileX, currentTileY, TileRegistry.dirtID);
               }

               server.network
                  .sendToClientsWithTile(new PacketChangeObject(level, 0, currentTileX, currentTileY, ladderObjectID), level, currentTileX, currentTileY);
            } else {
               if (shouldClearObject && obj.preventsLadderPlacement(level, currentTileX, currentTileY) == null) {
                  level.setObject(currentTileX, currentTileY, 0);
                  server.network.sendToClientsWithTile(new PacketChangeObject(level, 0, currentTileX, currentTileY, 0), level, currentTileX, currentTileY);
               }

               if (clearLiquid && level.getTile(currentTileX, currentTileY).isLiquid) {
                  level.sendTileChangePacket(server, currentTileX, currentTileY, TileRegistry.dirtID);
               }
            }
         }
      }
   }
}
