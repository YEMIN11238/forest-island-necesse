package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.inventory.container.settlement.events.SettlementSettlerCommandDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class RequestSettlerCommandDataAction extends RequestSettlerDataAbstractAction {
   public RequestSettlerCommandDataAction(SettlementDependantContainer container) {
      super(container);
   }

   @Override
   public void executePacket(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      new SettlementSettlerCommandDataEvent(data, mobUniqueIDs).applyAndSendToClient(client);
   }
}
