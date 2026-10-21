package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZone;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZoneRegistry;

public class SettlementWorkZoneChangedEvent extends SettlementContainerEvent {
   public final SettlementWorkZone zone;

   public SettlementWorkZoneChangedEvent(ServerSettlementData data, SettlementWorkZone zone) {
      super(data);
      this.zone = zone;
   }

   public SettlementWorkZoneChangedEvent(PacketReader reader) {
      super(reader);
      int zoneID = reader.getNextShortUnsigned();
      int uniqueID = reader.getNextInt();
      this.zone = SettlementWorkZoneRegistry.getNewZone(zoneID);
      this.zone.setUniqueID(uniqueID);
      this.zone.applyPacket(reader);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextShortUnsigned(this.zone.getID());
      writer.putNextInt(this.zone.getUniqueID());
      this.zone.writePacket(writer);
   }
}
