package necesse.inventory.container.settlement.events;

import java.util.HashMap;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlementRestrictZoneData;
import necesse.level.maps.levelData.settlementData.RestrictZone;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementRestrictZonesFullEvent extends SettlementContainerEvent {
   public final HashMap<Integer, SettlementRestrictZoneData> zones;
   public final int newSettlerRestrictZoneUniqueID;

   public SettlementRestrictZonesFullEvent(ServerSettlementData data) {
      super(data);
      this.zones = new HashMap<>();

      for (RestrictZone restrictZone : data.getRestrictZones()) {
         this.zones.put(restrictZone.uniqueID, new SettlementRestrictZoneData(restrictZone));
      }

      this.newSettlerRestrictZoneUniqueID = data.getNewSettlerRestrictZoneUniqueID();
   }

   public SettlementRestrictZonesFullEvent(PacketReader reader) {
      super(reader);
      int zonesSize = reader.getNextShortUnsigned();
      this.zones = new HashMap<>(zonesSize);

      for (int i = 0; i < zonesSize; i++) {
         SettlementRestrictZoneData zone = new SettlementRestrictZoneData(reader);
         this.zones.put(zone.uniqueID, zone);
      }

      this.newSettlerRestrictZoneUniqueID = reader.getNextInt();
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextShortUnsigned(this.zones.size());

      for (SettlementRestrictZoneData zone : this.zones.values()) {
         zone.writeContentPacket(writer);
      }

      writer.putNextInt(this.newSettlerRestrictZoneUniqueID);
   }
}
