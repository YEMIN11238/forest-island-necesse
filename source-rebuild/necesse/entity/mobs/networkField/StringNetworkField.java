package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class StringNetworkField extends NetworkField<String> {
   public StringNetworkField(String startValue) {
      super(startValue);
   }

   public void writePacket(String value, PacketWriter writer) {
      writer.putNextString(value);
   }

   public String readPacket(PacketReader reader) {
      return reader.getNextString();
   }
}
