package necesse.inventory.container.settlement.events;

import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class SettlementSettlerDataAbstractEvent<T> extends SettlementContainerEvent {
   public final HashMap<Integer, T> setterData;

   public SettlementSettlerDataAbstractEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data);
      this.setterData = new HashMap<>();
      if (mobUniqueIDs == null) {
         for (LevelSettler settler : data.getSettlers()) {
            this.setterData.put(settler.mobUniqueID, this.getSettlerData(data, settler.mobUniqueID));
         }
      } else {
         for (int mobUniqueID : mobUniqueIDs) {
            this.setterData.put(mobUniqueID, this.getSettlerData(data, mobUniqueID));
         }
      }
   }

   public SettlementSettlerDataAbstractEvent(PacketReader reader) {
      super(reader);
      int size = reader.getNextShortUnsigned();
      this.setterData = new HashMap<>(size);

      for (int i = 0; i < size; i++) {
         int mobUniqueID = reader.getNextInt();
         boolean hasData = reader.getNextBoolean();
         if (hasData) {
            T data = this.readSettlerData(reader);
            this.setterData.put(mobUniqueID, data);
         } else {
            this.setterData.put(mobUniqueID, null);
         }
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextShortUnsigned(this.setterData.size());

      for (Entry<Integer, T> entry : this.setterData.entrySet()) {
         writer.putNextInt(entry.getKey());
         T value = entry.getValue();
         if (value != null) {
            writer.putNextBoolean(true);
            this.writeSettlerData(value, writer);
         } else {
            writer.putNextBoolean(false);
         }
      }
   }

   public abstract T getSettlerData(ServerSettlementData var1, int var2);

   public abstract void writeSettlerData(T var1, PacketWriter var2);

   public abstract T readSettlerData(PacketReader var1);
}
