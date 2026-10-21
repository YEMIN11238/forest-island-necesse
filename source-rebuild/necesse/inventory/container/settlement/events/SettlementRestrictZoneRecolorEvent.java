package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.RestrictZone;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementRestrictZoneRecolorEvent extends SettlementContainerEvent {
   public final int restrictZoneUniqueID;
   public final int hue;

   public SettlementRestrictZoneRecolorEvent(ServerSettlementData data, RestrictZone zone) {
      super(data);
      this.restrictZoneUniqueID = zone.uniqueID;
      this.hue = zone.colorHue;
   }

   public SettlementRestrictZoneRecolorEvent(PacketReader reader) {
      super(reader);
      this.restrictZoneUniqueID = reader.getNextInt();
      this.hue = reader.getNextMaxValue(360);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.restrictZoneUniqueID);
      writer.putNextMaxValue(this.hue, 360);
   }
}
