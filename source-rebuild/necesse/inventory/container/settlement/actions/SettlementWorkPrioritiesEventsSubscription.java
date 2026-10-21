package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementSettlerPrioritiesChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerWorkPrioritiesDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementWorkPrioritiesEventsSubscription extends SettlementDataSubscriptionHandler {
   public SettlementWorkPrioritiesEventsSubscription(SettlementContainer container) {
      super(container, SettlementSettlerPrioritiesChangedEvent.class, SettlementSettlerWorkPrioritiesDataEvent.class);
   }

   @Override
   public void sendInitialServerEvents(ServerSettlementData data, ServerClient client) {
      new SettlementSettlerWorkPrioritiesDataEvent(data, null).applyAndSendToClient(client);
   }

   @Override
   public void sendRequestedDataEvents(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      new SettlementSettlerWorkPrioritiesDataEvent(data, mobUniqueIDs).applyAndSendToClient(client);
   }
}
