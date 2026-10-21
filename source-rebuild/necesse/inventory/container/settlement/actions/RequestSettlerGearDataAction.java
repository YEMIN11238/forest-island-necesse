package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.inventory.container.settlement.events.SettlementSettlerGearDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class RequestSettlerGearDataAction extends RequestSettlerDataAbstractAction {
   public RequestSettlerGearDataAction(SettlementDependantContainer container) {
      super(container);
   }

   @Override
   public void executePacket(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      new SettlementSettlerGearDataEvent(data, mobUniqueIDs).applyAndSendToClient(client);
   }
}
