package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.events.ContainerEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerGroupDeletedEvent extends ContainerEvent {
   public final int settlementUniqueID;
   public final int groupUniqueID;

   public SettlementSettlerGroupDeletedEvent(ServerSettlementData data, int groupUniqueID) {
      this.settlementUniqueID = data.uniqueID;
      this.groupUniqueID = groupUniqueID;
   }

   public SettlementSettlerGroupDeletedEvent(PacketReader reader) {
      super(reader);
      this.settlementUniqueID = reader.getNextInt();
      this.groupUniqueID = reader.getNextInt();
   }

   @Override
   public void write(PacketWriter writer) {
      writer.putNextInt(this.settlementUniqueID);
      writer.putNextInt(this.groupUniqueID);
   }
}
