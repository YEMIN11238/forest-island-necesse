package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class ShortNetworkField extends NetworkField<Short> {
   public ShortNetworkField(short startValue) {
      super(startValue);
   }

   public void writePacket(Short value, PacketWriter writer) {
      writer.putNextShort(value);
   }

   public Short readPacket(PacketReader reader) {
      return reader.getNextShort();
   }
}
