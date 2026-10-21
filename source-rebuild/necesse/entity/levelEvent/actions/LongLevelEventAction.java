package necesse.entity.levelEvent.actions;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class LongLevelEventAction extends LevelEventAction {
   public void runAndSend(long value) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextLong(value);
      this.runAndSendAction(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      long value = reader.getNextLong();
      this.run(value);
   }

   protected abstract void run(long var1);
}
