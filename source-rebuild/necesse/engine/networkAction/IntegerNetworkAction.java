package necesse.engine.networkAction;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class IntegerNetworkAction<R> extends NetworkAction<R> {
   public void runAndSend(int value) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(value);
      this.runAndSendAction(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      this.run(reader.getNextInt());
   }

   protected abstract void run(int var1);
}
