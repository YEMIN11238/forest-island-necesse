package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.entity.DamagedObjectEntity;

public class PacketDamagedTileRemoved extends Packet {
   public final int tileX;
   public final int tileY;

   public PacketDamagedTileRemoved(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
   }

   public PacketDamagedTileRemoved(int tileX, int tileY) {
      this.tileX = tileX;
      this.tileY = tileY;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(tileX);
      writer.putNextInt(tileY);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      DamagedObjectEntity damagedTile = client.getLevel().entityManager.damagedObjects.get(this.tileX, this.tileY, false);
      if (damagedTile != null) {
         damagedTile.remove();
      }
   }
}
