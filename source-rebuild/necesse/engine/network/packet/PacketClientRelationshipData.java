package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.server.ServerClient;

public class PacketClientRelationshipData extends Packet {
   public final int slot;
   public final Packet content;

   public PacketClientRelationshipData(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.slot = reader.getNextByteUnsigned();
      this.content = reader.getNextContentPacket();
   }

   public PacketClientRelationshipData(ServerClient client) {
      this.slot = client.slot;
      this.content = new Packet();
      client.relationshipData.writePacket(new PacketWriter(this.content));
      PacketWriter writer = new PacketWriter(this);
      writer.putNextByteUnsigned(this.slot);
      writer.putNextContentPacket(this.content);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         ClientClient target = client.getClient(this.slot);
         if (target != null) {
            target.relationshipData.readPacket(new PacketReader(this.content));
         } else {
            client.network.sendPacket(new PacketRequestPlayerData(this.slot));
         }
      }
   }
}
