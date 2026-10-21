package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementWorkZoneRemovedEvent extends SettlementContainerEvent {
   public final int zoneUniqueID;

   public SettlementWorkZoneRemovedEvent(ServerSettlementData data, int zoneUniqueID) {
      super(data);
      this.zoneUniqueID = zoneUniqueID;
   }

   public SettlementWorkZoneRemovedEvent(PacketReader reader) {
      super(reader);
      this.zoneUniqueID = reader.getNextInt();
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.zoneUniqueID);
   }
}
