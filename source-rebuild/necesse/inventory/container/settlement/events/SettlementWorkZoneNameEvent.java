package necesse.inventory.container.settlement.events;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZone;

public class SettlementWorkZoneNameEvent extends SettlementContainerEvent {
   public final int zoneUniqueID;
   public final GameMessage name;

   public SettlementWorkZoneNameEvent(ServerSettlementData data, SettlementWorkZone zone) {
      super(data);
      this.zoneUniqueID = zone.getUniqueID();
      this.name = zone.getName();
   }

   public SettlementWorkZoneNameEvent(PacketReader reader) {
      super(reader);
      this.zoneUniqueID = reader.getNextInt();
      this.name = GameMessage.fromPacket(reader);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.zoneUniqueID);
      this.name.writePacket(writer);
   }
}
