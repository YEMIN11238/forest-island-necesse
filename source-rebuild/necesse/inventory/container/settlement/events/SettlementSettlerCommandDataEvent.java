package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerCommandData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerCommandDataEvent extends SettlementSettlerDataAbstractEvent<SettlerCommandData> {
   public SettlementSettlerCommandDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerCommandDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerCommandData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      if (settler != null) {
         SettlerMob settlerMob = settler.getMob();
         if (settlerMob instanceof CommandMob) {
            return new SettlerCommandData((CommandMob)settlerMob);
         }
      }

      return null;
   }

   public void writeSettlerData(SettlerCommandData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerCommandData readSettlerData(PacketReader reader) {
      return new SettlerCommandData(reader);
   }
}
