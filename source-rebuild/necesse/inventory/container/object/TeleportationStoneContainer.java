package necesse.inventory.container.object;

import java.awt.Point;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.TeleportToTeamContainer;
import necesse.level.maps.LevelObject;

public class TeleportationStoneContainer extends TeleportToTeamContainer {
   public final LevelObject levelObject;

   public TeleportationStoneContainer(NetworkClient client, int uniqueSeed, LevelObject levelObject, Packet contentPacket) {
      super(client, uniqueSeed, contentPacket);
      this.levelObject = levelObject;
   }

   @Override
   public Point getFromLevelPosition() {
      return new Point(this.levelObject.tileX * 32 + 16, this.levelObject.tileY * 32 + 16);
   }

   @Override
   public void performTeleport(ServerClient client, ServerClient target) {
      client.getLevel().entityManager.events.add(this.getTeleportEvent(client, target, 0, 10.0F));
      client.closeContainer(false);
   }

   @Override
   public boolean isValid(ServerClient client) {
      return !super.isValid(client) ? false : !this.levelObject.hasChanged() && this.levelObject.isInInteractRange(client.playerMob);
   }
}
