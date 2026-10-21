package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.inventory.container.settlement.data.SettlerWorkPrioritiesData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class SettlementSettlerWorkPrioritiesDataEvent extends SettlementSettlerDataAbstractEvent<SettlerWorkPrioritiesData> {
   public SettlementSettlerWorkPrioritiesDataEvent(ServerSettlementData data, Iterable<Integer> mobUniqueIDs) {
      super(data, mobUniqueIDs);
   }

   public SettlementSettlerWorkPrioritiesDataEvent(PacketReader reader) {
      super(reader);
   }

   public SettlerWorkPrioritiesData getSettlerData(ServerSettlementData data, int mobUniqueID) {
      LevelSettler settler = data.getSettler(mobUniqueID);
      if (settler != null) {
         SettlerMob settlerMob = settler.getMob();
         if (settlerMob instanceof EntityJobWorker) {
            EntityJobWorker worker = (EntityJobWorker)settlerMob;
            return new SettlerWorkPrioritiesData(settler, worker);
         }
      }

      return null;
   }

   public void writeSettlerData(SettlerWorkPrioritiesData settlerData, PacketWriter writer) {
      settlerData.writeContentPacket(writer);
   }

   public SettlerWorkPrioritiesData readSettlerData(PacketReader reader) {
      return new SettlerWorkPrioritiesData(reader);
   }
}
