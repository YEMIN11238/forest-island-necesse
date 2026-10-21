package necesse.inventory.container.settlement.events;

import java.util.HashMap;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZone;
import necesse.level.maps.levelData.settlementData.zones.SettlementWorkZoneRegistry;

public class SettlementWorkZonesEvent extends SettlementContainerEvent {
   public final HashMap<Integer, SettlementWorkZone> zones;

   public SettlementWorkZonesEvent(ServerSettlementData data) {
      super(data);
      this.zones = new HashMap<>(data.getWorkZones().getZones());
   }

   public SettlementWorkZonesEvent(PacketReader reader) {
      super(reader);
      int size = reader.getNextShortUnsigned();
      this.zones = new HashMap<>();

      for (int i = 0; i < size; i++) {
         int id = reader.getNextShortUnsigned();
         int uniqueID = reader.getNextInt();
         SettlementWorkZone zone = SettlementWorkZoneRegistry.getNewZone(id);
         zone.setUniqueID(uniqueID);
         zone.applyPacket(reader);
         this.zones.put(uniqueID, zone);
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextShortUnsigned(this.zones.size());

      for (SettlementWorkZone zone : this.zones.values()) {
         writer.putNextShortUnsigned(zone.getID());
         writer.putNextInt(zone.getUniqueID());
         zone.writePacket(writer);
      }
   }
}
