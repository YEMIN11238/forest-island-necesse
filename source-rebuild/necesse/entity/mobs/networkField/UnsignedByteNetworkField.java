package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class UnsignedByteNetworkField extends NetworkField<Integer> {
   public UnsignedByteNetworkField(int startValue) {
      super(startValue);
   }

   public void writePacket(Integer value, PacketWriter writer) {
      writer.putNextByteUnsigned(value);
   }

   public Integer readPacket(PacketReader reader) {
      return reader.getNextByteUnsigned();
   }
}
