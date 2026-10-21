package necesse.inventory.container.travel;

import java.awt.Point;
import java.awt.geom.Line2D.Float;
import necesse.engine.GameLog;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.packet.PacketLevelEvent;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;
import necesse.engine.util.IntersectionPoint;
import necesse.entity.levelEvent.TeleportEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;

public class TravelStoneContainer extends TravelContainer {
   public TravelStoneContainer(NetworkClient client, int uniqueSeed, Packet contentPacket) {
      super(client, uniqueSeed, contentPacket);
   }

   @Override
   public void travelTo(IslandData destination, int dimension) {
      if (this.client.isClient()) {
         throw new IllegalStateException("Cannot be called client side, send a request travel packet instead");
      }

      ServerClient serverClient = this.client.getServerClient();
      if (!this.travelLevel.isIslandPosition()) {
         GameLog.warn.println(serverClient.getName() + " tried to use travel stone from non island level");
         this.close();
      } else {
         this.sendTeleportEvent(this.client.playerMob);
         int lastTileX = this.client.playerMob.getTileX();
         int lastTileY = this.client.playerMob.getTileY();
         int oldIslandX = this.travelLevel.getIslandX();
         int oldIslandY = this.travelLevel.getIslandY();
         int newIslandX = destination.islandX;
         int newIslandY = destination.islandY;
         serverClient.changeIsland(
            newIslandX,
            newIslandY,
            dimension,
            level -> {
               int newTileX = lastTileX;
               if (level.tileWidth > 0) {
                  if (oldIslandX < newIslandX) {
                     newTileX = 5;
                  } else if (oldIslandX > newIslandX) {
                     newTileX = level.tileWidth - 5;
                  } else {
                     newTileX = level.limitTileXToBounds(lastTileX, 0, 5);
                  }
               }

               int newTileY = lastTileY;
               if (level.tileHeight > 0) {
                  if (oldIslandY < newIslandY) {
                     newTileY = 5;
                  } else if (oldIslandY > newIslandY) {
                     newTileY = level.tileHeight - 5;
                  } else {
                     newTileY = level.limitTileYToBounds(lastTileY, 0, 5);
                  }
               }

               if (level.tileWidth > 0 && level.tileHeight > 0) {
                  Float line = new Float(
                     newTileX * 32 + 16, newTileY * 32 + 16, level.tileWidth / 2.0F * 32.0F + 16.0F, level.tileHeight / 2.0F * 32.0F + 16.0F
                  );
                  IntersectionPoint<LevelObjectHit> hit = GameUtils.castRayFirstHit(
                     line, 100.0, ray -> level.getCollisionPoint(level.getCollisions(ray, new CollisionFilter().allLandTiles()), ray, true)
                  );
                  if (hit != null && hit.target != null) {
                     return new Point(hit.target.tileX * 32 + 16, hit.target.tileY * 32 + 16);
                  }
               }

               return new Point(newTileX * 32 + 16, newTileY * 32 + 16);
            },
            true
         );
         serverClient.newStats.island_travels.increment(1);
         serverClient.closeContainer(false);
         this.client.playerMob.addBuff(new ActiveBuff("teleportsickness", this.client.playerMob, 10.0F, null), true);
         this.sendTeleportEvent(this.client.playerMob);
      }
   }

   protected void sendTeleportEvent(Mob mob) {
      TeleportEvent event = new TeleportEvent(mob.getX(), mob.getY() + 5, mob.getUniqueID());
      mob.getLevel().getServer().network.sendToClientsWithTile(new PacketLevelEvent(event), mob.getLevel(), mob.getTileX(), mob.getTileY());
   }
}
