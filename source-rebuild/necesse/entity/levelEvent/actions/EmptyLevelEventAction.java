package necesse.entity.levelEvent.actions;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;

public abstract class EmptyLevelEventAction extends LevelEventAction {
   public void runAndSend() {
      this.runAndSendAction(new Packet());
   }

   @Override
   public void executePacket(PacketReader reader) {
      this.run();
   }

   protected abstract void run();
}
