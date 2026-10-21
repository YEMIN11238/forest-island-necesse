package necesse.inventory.container.settlement.events;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.events.ContainerEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementSettlerGroup;

public class SettlementSettlerGroupRenameEvent extends ContainerEvent {
   public final int settlementUniqueID;
   public final int groupUniqueID;
   public final GameMessage name;

   public SettlementSettlerGroupRenameEvent(ServerSettlementData data, SettlementSettlerGroup group) {
      this.settlementUniqueID = data.uniqueID;
      this.groupUniqueID = group.groupUniqueID;
      this.name = group.getName();
   }

   public SettlementSettlerGroupRenameEvent(PacketReader reader) {
      super(reader);
      this.settlementUniqueID = reader.getNextInt();
      this.groupUniqueID = reader.getNextInt();
      this.name = GameMessage.fromPacket(reader);
   }

   @Override
   public void write(PacketWriter writer) {
      writer.putNextInt(this.settlementUniqueID);
      writer.putNextInt(this.groupUniqueID);
      this.name.writePacket(writer);
   }
}
