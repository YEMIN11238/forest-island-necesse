package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.events.ContainerEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class SettlementContainerEvent extends ContainerEvent {
   public final int settlementUniqueID;

   public SettlementContainerEvent(ServerSettlementData data) {
      this.settlementUniqueID = data.uniqueID;
   }

   public SettlementContainerEvent(PacketReader reader) {
      super(reader);
      this.settlementUniqueID = reader.getNextInt();
   }

   @Override
   public final void write(PacketWriter writer) {
      writer.putNextInt(this.settlementUniqueID);
      this.settlementWrite(writer);
   }

   public abstract void settlementWrite(PacketWriter var1);
}
