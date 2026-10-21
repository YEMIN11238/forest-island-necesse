package necesse.entity.mobs.ability;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;

public abstract class EmptyMobAbility extends MobAbility {
   public void runAndSend() {
      this.runAndSendAbility(new Packet());
   }

   @Override
   public void executePacket(PacketReader reader) {
      this.run();
   }

   protected abstract void run();
}
