package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PacketRequestQuests extends Packet {
   public PacketRequestQuests(byte[] data) {
      super(data);
   }

   public PacketRequestQuests() {
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      server.network.sendPacket(new PacketQuests(client.getQuests()), client);
   }
}
