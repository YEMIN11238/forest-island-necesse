package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerRestrictZoneData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerRestrictZoneDataEvent extends SettlementSettlerDataAbstractEvent<SettlerRestrictZoneData> {
   public SettlementSettlerRestrictZoneDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerRestrictZoneDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerRestrictZoneData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      return settler != null ? new SettlerRestrictZoneData(settler) : null;
   }

   public void writeSettlerData(SettlerRestrictZoneData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerRestrictZoneData readSettlerData(PacketReader reader) {
      return new SettlerRestrictZoneData(reader);
   }
}
