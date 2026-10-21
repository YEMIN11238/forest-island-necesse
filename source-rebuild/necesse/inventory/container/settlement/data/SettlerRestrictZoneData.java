package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.LevelSettler;

public class SettlerRestrictZoneData {
   public int restrictZoneUniqueID;

   public SettlerRestrictZoneData(LevelSettler settler) {
      this.restrictZoneUniqueID = settler.getRestrictZoneUniqueID();
   }

   public SettlerRestrictZoneData(PacketReader reader) {
      this.restrictZoneUniqueID = reader.getNextInt();
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextInt(this.restrictZoneUniqueID);
   }
}
