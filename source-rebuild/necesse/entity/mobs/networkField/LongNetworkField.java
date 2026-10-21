package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class LongNetworkField extends NetworkField<Long> {
   public LongNetworkField(long startValue) {
      super(startValue);
   }

   public void writePacket(Long value, PacketWriter writer) {
      writer.putNextLong(value);
   }

   public Long readPacket(PacketReader reader) {
      return reader.getNextLong();
   }
}
