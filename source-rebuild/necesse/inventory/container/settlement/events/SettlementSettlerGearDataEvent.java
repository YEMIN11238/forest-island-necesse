package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerGearData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerGearDataEvent extends SettlementSettlerDataAbstractEvent<SettlerGearData> {
   public SettlementSettlerGearDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerGearDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerGearData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      if (settler != null) {
         SettlerMob settlerMob = settler.getMob();
         if (settlerMob != null) {
            return settlerMob.createGearData();
         }
      }

      return null;
   }

   public void writeSettlerData(SettlerGearData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerGearData readSettlerData(PacketReader reader) {
      return new SettlerGearData(reader);
   }
}
