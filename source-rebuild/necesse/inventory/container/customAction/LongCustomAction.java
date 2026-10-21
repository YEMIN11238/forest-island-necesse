package necesse.inventory.container.customAction;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class LongCustomAction extends ContainerCustomAction {
   public void runAndSend(long value) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextLong(value);
      this.runAndSendAction(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      this.run(reader.getNextLong());
   }

   protected abstract void run(long var1);
}
