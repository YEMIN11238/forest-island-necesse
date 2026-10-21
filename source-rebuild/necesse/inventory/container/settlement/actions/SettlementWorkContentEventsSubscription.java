package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementSingleStorageEvent;
import necesse.inventory.container.settlement.events.SettlementSingleWorkstationsEvent;
import necesse.inventory.container.settlement.events.SettlementStorageEvent;
import necesse.inventory.container.settlement.events.SettlementWorkZoneChangedEvent;
import necesse.inventory.container.settlement.events.SettlementWorkZoneNameEvent;
import necesse.inventory.container.settlement.events.SettlementWorkZoneRemovedEvent;
import necesse.inventory.container.settlement.events.SettlementWorkZonesEvent;
import necesse.inventory.container.settlement.events.SettlementWorkstationsEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementWorkContentEventsSubscription extends SettlementDataSubscriptionHandler {
   public SettlementWorkContentEventsSubscription(SettlementContainer container) {
      super(
         container,
         SettlementStorageEvent.class,
         SettlementSingleStorageEvent.class,
         SettlementWorkstationsEvent.class,
         SettlementSingleWorkstationsEvent.class,
         SettlementWorkZoneRemovedEvent.class,
         SettlementWorkZoneChangedEvent.class,
         SettlementWorkZoneNameEvent.class
      );
   }

   @Override
   public void sendInitialServerEvents(ServerSettlementData data, ServerClient client) {
      new SettlementStorageEvent(data).applyAndSendToClient(client);
      new SettlementWorkstationsEvent(data).applyAndSendToClient(client);
      new SettlementWorkZonesEvent(data).applyAndSendToClient(client);
   }

   @Override
   public void sendRequestedDataEvents(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      throw new UnsupportedOperationException("Work content subscription does not support requesting data for specific mobs");
   }
}
