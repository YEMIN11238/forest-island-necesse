package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class ByteNetworkField extends NetworkField<Byte> {
   public ByteNetworkField(byte startValue) {
      super(startValue);
   }

   public void writePacket(Byte value, PacketWriter writer) {
      writer.putNextByte(value);
   }

   public Byte readPacket(PacketReader reader) {
      return reader.getNextByte();
   }
}
