package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class UnsignedShortNetworkField extends NetworkField<Integer> {
   public UnsignedShortNetworkField(int startValue) {
      super(startValue);
   }

   public void writePacket(Integer value, PacketWriter writer) {
      writer.putNextShortUnsigned(value);
   }

   public Integer readPacket(PacketReader reader) {
      return reader.getNextShortUnsigned();
   }
}
