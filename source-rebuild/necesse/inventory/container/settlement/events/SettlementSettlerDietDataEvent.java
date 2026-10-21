package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerDietData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerDietDataEvent extends SettlementSettlerDataAbstractEvent<SettlerDietData> {
   public SettlementSettlerDietDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerDietDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerDietData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      return settler != null ? new SettlerDietData(settler) : null;
   }

   public void writeSettlerData(SettlerDietData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerDietData readSettlerData(PacketReader reader) {
      return new SettlerDietData(reader);
   }
}
