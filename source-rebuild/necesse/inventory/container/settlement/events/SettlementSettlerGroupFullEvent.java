package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.events.ContainerEvent;
import necesse.inventory.container.settlement.SettlementContainerSettlerGroup;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementSettlerGroup;

public class SettlementSettlerGroupFullEvent extends ContainerEvent {
   public final int settlementUniqueID;
   public final SettlementContainerSettlerGroup group;

   public SettlementSettlerGroupFullEvent(ServerSettlementData data, SettlementSettlerGroup group) {
      this.settlementUniqueID = data.uniqueID;
      this.group = new SettlementContainerSettlerGroup(group);
   }

   public SettlementSettlerGroupFullEvent(PacketReader reader) {
      super(reader);
      this.settlementUniqueID = reader.getNextInt();
      this.group = new SettlementContainerSettlerGroup(reader);
   }

   @Override
   public void write(PacketWriter writer) {
      writer.putNextInt(this.settlementUniqueID);
      this.group.write(writer);
   }
}
