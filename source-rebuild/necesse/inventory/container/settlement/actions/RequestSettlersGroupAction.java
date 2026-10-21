package necesse.inventory.container.settlement.actions;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupDeletedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupFullEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementSettlerGroup;

public class RequestSettlersGroupAction extends SettlementAccessRequiredContainerCustomAction {
   public RequestSettlersGroupAction(SettlementContainer container) {
      super(container);
   }

   public void runAndSend(int groupUniqueID) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(groupUniqueID);
      this.runAndSendAction(content);
   }

   @Override
   public void executePacket(PacketReader reader, ServerSettlementData data, ServerClient client) {
      int groupUniqueID = reader.getNextInt();
      SettlementSettlerGroup group = data.groups.getGroup(groupUniqueID);
      if (group != null) {
         new SettlementSettlerGroupFullEvent(data, group).applyAndSendToClient(client);
      } else {
         new SettlementSettlerGroupDeletedEvent(data, groupUniqueID).applyAndSendToClient(client);
      }
   }
}
