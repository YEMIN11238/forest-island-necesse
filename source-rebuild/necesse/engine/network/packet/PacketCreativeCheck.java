package necesse.engine.network.packet;

import necesse.engine.network.Packet;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public abstract class PacketCreativeCheck extends Packet {
   public PacketCreativeCheck(byte[] data) {
      super(data);
   }

   public PacketCreativeCheck() {
   }

   public static boolean checkCreativeAndSendUpdate(Server server, ServerClient client) {
      if (!client.checkHasRequestedSelf()) {
         return false;
      } else if (!server.world.settings.creativeMode) {
         client.sendPacket(new PacketSettings(server.world.settings));
         return false;
      } else {
         return true;
      }
   }
}
