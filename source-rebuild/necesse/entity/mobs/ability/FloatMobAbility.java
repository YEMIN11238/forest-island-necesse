package necesse.entity.mobs.ability;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class FloatMobAbility extends MobAbility {
   public void runAndSend(float value) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextFloat(value);
      this.runAndSendAbility(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      float value = reader.getNextFloat();
      this.run(value);
   }

   protected abstract void run(float var1);
}
