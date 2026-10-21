package necesse.entity.mobs.ability;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class EnumListMobAbility<T extends Enum<T>> extends MobAbility {
   private final Class<T> clazz;

   public EnumListMobAbility(Class<T> clazz) {
      this.clazz = clazz;
   }

   public void runAndSend(List<T> list) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextShortUnsigned(list.size());

      for (T value : list) {
         writer.putNextEnum(value);
      }

      this.runAndSendAbility(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      int size = reader.getNextShortUnsigned();
      ArrayList<T> list = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         list.add(reader.getNextEnum(this.clazz));
      }

      this.run(list);
   }

   protected abstract void run(List<T> var1);
}
