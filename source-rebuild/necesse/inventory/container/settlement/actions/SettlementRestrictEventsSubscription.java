package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementNewSettlerRestrictZoneChangedEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZoneChangedEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZoneRecolorEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZoneRenameEvent;
import necesse.inventory.container.settlement.events.SettlementRestrictZonesFullEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerRestrictZoneDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementRestrictEventsSubscription extends SettlementDataSubscriptionHandler {
   public SettlementRestrictEventsSubscription(SettlementContainer container) {
      super(
         container,
         SettlementRestrictZonesFullEvent.class,
         SettlementNewSettlerRestrictZoneChangedEvent.class,
         SettlementRestrictZoneChangedEvent.class,
         SettlementRestrictZoneRenameEvent.class,
         SettlementRestrictZoneRecolorEvent.class,
         SettlementSettlerRestrictZoneDataEvent.class
      );
   }

   @Override
   public void sendInitialServerEvents(ServerSettlementData data, ServerClient client) {
      new SettlementSettlerRestrictZoneDataEvent(data, null).applyAndSendToClient(client);
      new SettlementRestrictZonesFullEvent(data).applyAndSendToClient(client);
      new SettlementNewSettlerRestrictZoneChangedEvent(data).applyAndSendToClient(client);
   }

   @Override
   public void sendRequestedDataEvents(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      new SettlementSettlerRestrictZoneDataEvent(data, mobUniqueIDs).applyAndSendToClient(client);
   }
}
