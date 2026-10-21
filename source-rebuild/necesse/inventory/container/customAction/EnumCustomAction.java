package necesse.inventory.container.customAction;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public abstract class EnumCustomAction<T extends Enum<T>> extends ContainerCustomAction {
   protected final Class<T> enumClass;

   public EnumCustomAction(Class<T> enumClass) {
      this.enumClass = enumClass;
   }

   public void runAndSend(T value) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextShortUnsigned(value.ordinal());
      this.runAndSendAction(content);
   }

   @Override
   public void executePacket(PacketReader reader) {
      int ordinal = reader.getNextShortUnsigned();
      T[] constants = this.enumClass.getEnumConstants();
      if (ordinal < constants.length) {
         this.run(constants[ordinal]);
      } else {
         throw new IllegalStateException("Could not find enum with ordinal " + ordinal + " from " + this.enumClass.getSimpleName());
      }
   }

   protected abstract void run(T var1);
}
