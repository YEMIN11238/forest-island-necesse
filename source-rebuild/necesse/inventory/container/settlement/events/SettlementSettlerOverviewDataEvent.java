package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerOverviewData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerOverviewDataEvent extends SettlementSettlerDataAbstractEvent<SettlerOverviewData> {
   public SettlementSettlerOverviewDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerOverviewDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerOverviewData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      return settler != null ? new SettlerOverviewData(settler) : null;
   }

   public void writeSettlerData(SettlerOverviewData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerOverviewData readSettlerData(PacketReader reader) {
      return new SettlerOverviewData(reader);
   }
}
