package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerLooksData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerLooksDataEvent extends SettlementSettlerDataAbstractEvent<SettlerLooksData> {
   public SettlementSettlerLooksDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerLooksDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerLooksData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      if (settler != null) {
         SettlerMob settlerMob = settler.getMob();
         if (settlerMob != null) {
            return settlerMob.createLooksData();
         }
      }

      return null;
   }

   public void writeSettlerData(SettlerLooksData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerLooksData readSettlerData(PacketReader reader) {
      return new SettlerLooksData(reader);
   }
}
