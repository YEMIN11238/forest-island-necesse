package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class IntNetworkField extends NetworkField<Integer> {
   public IntNetworkField(int startValue) {
      super(startValue);
   }

   public void writePacket(Integer value, PacketWriter writer) {
      writer.putNextInt(value);
   }

   public Integer readPacket(PacketReader reader) {
      return reader.getNextInt();
   }
}
