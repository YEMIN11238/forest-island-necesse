package necesse.entity.mobs.networkField;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class FloatNetworkField extends NetworkField<Float> {
   public FloatNetworkField(float startValue) {
      super(startValue);
   }

   public void writePacket(Float value, PacketWriter writer) {
      writer.putNextFloat(value);
   }

   public Float readPacket(PacketReader reader) {
      return reader.getNextFloat();
   }
}
