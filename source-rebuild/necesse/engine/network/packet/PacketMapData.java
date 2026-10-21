package necesse.engine.network.packet;

import java.io.IOException;
import java.util.zip.DataFormatException;
import necesse.engine.GameLog;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;

public class PacketMapData extends Packet {
   public final int fromPlayerSlot;
   private final Packet mapContentPacket;

   public PacketMapData(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.fromPlayerSlot = reader.getNextShortUnsigned();
      this.mapContentPacket = reader.getNextContentPacket();
   }

   public PacketMapData(int fromPlayerSlot, Packet mapContentPacket) {
      this.fromPlayerSlot = fromPlayerSlot;
      this.mapContentPacket = mapContentPacket;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextShortUnsigned(fromPlayerSlot);
      writer.putNextContentPacket(mapContentPacket);
   }

   public PacketReader getMapContentReader() {
      return new PacketReader(this.mapContentPacket);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         try {
            client.levelManager.readMapPacketData(this.getMapContentReader());
         } catch (DataFormatException | IOException e) {
            e.printStackTrace(GameLog.err);
         }
      }
   }
}
