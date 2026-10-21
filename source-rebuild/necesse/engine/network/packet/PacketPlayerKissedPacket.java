package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;

public class PacketPlayerKissedPacket extends Packet {
   public final int fromSlot;
   public final int toSlot;

   public PacketPlayerKissedPacket(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.fromSlot = reader.getNextByteUnsigned();
      this.toSlot = reader.getNextByteUnsigned();
   }

   public PacketPlayerKissedPacket(int fromSlot, int toSlot) {
      this.fromSlot = fromSlot;
      this.toSlot = toSlot;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextByteUnsigned(fromSlot);
      writer.putNextByteUnsigned(toSlot);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         ClientClient target = client.getClient(this.fromSlot);
         if (target != null) {
            if (target.playerMob != null && target.playerMob.getLevel() != null) {
               target.playerMob.applyKissed(this.toSlot, false);
            }
         } else {
            client.network.sendPacket(new PacketRequestPlayerData(this.fromSlot));
         }
      }
   }
}
