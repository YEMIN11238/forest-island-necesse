package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementNewSettlerRestrictZoneChangedEvent extends SettlementContainerEvent {
   public final int restrictZoneUniqueID;

   public SettlementNewSettlerRestrictZoneChangedEvent(ServerSettlementData data) {
      super(data);
      this.restrictZoneUniqueID = data.getNewSettlerRestrictZoneUniqueID();
   }

   public SettlementNewSettlerRestrictZoneChangedEvent(PacketReader reader) {
      super(reader);
      this.restrictZoneUniqueID = reader.getNextInt();
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.restrictZoneUniqueID);
   }
}
