package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.settlement.data.SettlerActivityData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerActivityDataEvent extends SettlementSettlerDataAbstractEvent<SettlerActivityData> {
   public SettlementSettlerActivityDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerActivityDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerActivityData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      if (settler != null) {
         SettlerMob settlerMob = settler.getMob();
         if (settlerMob != null) {
            return new SettlerActivityData(settlerMob.getCurrentActivity(), settlerMob.hasCommandOrders());
         }
      }

      return null;
   }

   public void writeSettlerData(SettlerActivityData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerActivityData readSettlerData(PacketReader reader) {
      return new SettlerActivityData(reader);
   }
}
