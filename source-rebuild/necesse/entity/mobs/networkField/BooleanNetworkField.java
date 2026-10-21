package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class BooleanNetworkField extends NetworkField<Boolean> {
   public BooleanNetworkField(boolean startValue) {
      super(startValue);
   }

   public void writePacket(Boolean value, PacketWriter writer) {
      writer.putNextBoolean(value);
   }

   public Boolean readPacket(PacketReader reader) {
      return reader.getNextBoolean();
   }
}
