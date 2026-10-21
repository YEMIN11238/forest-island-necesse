package necesse.inventory.container.settlement.events;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.RestrictZone;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementRestrictZoneRenameEvent extends SettlementContainerEvent {
   public final int restrictZoneUniqueID;
   public final GameMessage name;

   public SettlementRestrictZoneRenameEvent(ServerSettlementData data, RestrictZone zone) {
      super(data);
      this.restrictZoneUniqueID = zone.uniqueID;
      this.name = zone.name;
   }

   public SettlementRestrictZoneRenameEvent(PacketReader reader) {
      super(reader);
      this.restrictZoneUniqueID = reader.getNextInt();
      this.name = GameMessage.fromPacket(reader);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.restrictZoneUniqueID);
      this.name.writePacket(writer);
   }
}
