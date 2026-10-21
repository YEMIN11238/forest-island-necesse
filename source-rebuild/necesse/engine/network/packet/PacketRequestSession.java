package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;

public class PacketRequestSession extends Packet {
   public PacketRequestSession(byte[] data) {
      super(data);
   }

   public PacketRequestSession() {
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      client.network.sendPacket(new PacketUpdateSession(client.sessionID));
   }
}
