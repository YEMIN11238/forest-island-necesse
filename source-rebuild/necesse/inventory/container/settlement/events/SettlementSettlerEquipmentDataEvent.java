package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerEquipmentData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerEquipmentDataEvent extends SettlementSettlerDataAbstractEvent<SettlerEquipmentData> {
   public SettlementSettlerEquipmentDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerEquipmentDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerEquipmentData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      return settler != null ? new SettlerEquipmentData(settler) : null;
   }

   public void writeSettlerData(SettlerEquipmentData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerEquipmentData readSettlerData(PacketReader reader) {
      return new SettlerEquipmentData(reader);
   }
}
