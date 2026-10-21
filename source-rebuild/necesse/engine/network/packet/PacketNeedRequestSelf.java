package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;

public class PacketNeedRequestSelf extends Packet {
   public PacketNeedRequestSelf(byte[] data) {
      super(data);
   }

   public PacketNeedRequestSelf() {
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getSlot() != -1) {
         client.network.sendPacket(new PacketRequestPlayerData(client.getSlot()));
      }
   }
}
