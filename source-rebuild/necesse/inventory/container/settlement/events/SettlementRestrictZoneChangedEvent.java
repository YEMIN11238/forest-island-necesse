package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.util.ZoningChange;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementRestrictZoneChangedEvent extends SettlementContainerEvent {
   public final int restrictZoneUniqueID;
   public final ZoningChange change;

   public SettlementRestrictZoneChangedEvent(ServerSettlementData data, int restrictZoneUniqueID, ZoningChange change) {
      super(data);
      this.restrictZoneUniqueID = restrictZoneUniqueID;
      this.change = change;
   }

   public SettlementRestrictZoneChangedEvent(PacketReader reader) {
      super(reader);
      this.restrictZoneUniqueID = reader.getNextInt();
      this.change = ZoningChange.fromPacket(reader);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.restrictZoneUniqueID);
      this.change.write(writer);
   }
}
