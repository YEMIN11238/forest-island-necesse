package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementNewSettlerDietChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerDietChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerDietDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementDietEventsSubscription extends SettlementDataSubscriptionHandler {
   public SettlementDietEventsSubscription(SettlementContainer container) {
      super(container, SettlementNewSettlerDietChangedEvent.class, SettlementSettlerDietChangedEvent.class);
   }

   @Override
   public void sendInitialServerEvents(ServerSettlementData data, ServerClient client) {
      new SettlementSettlerDietDataEvent(data, null).applyAndSendToClient(client);
      new SettlementNewSettlerDietChangedEvent(data).applyAndSendToClient(client);
   }

   @Override
   public void sendRequestedDataEvents(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      new SettlementSettlerDietDataEvent(data, mobUniqueIDs).applyAndSendToClient(client);
   }
}
