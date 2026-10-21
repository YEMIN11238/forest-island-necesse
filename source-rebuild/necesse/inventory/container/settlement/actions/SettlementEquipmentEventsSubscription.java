package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementNewSettlerEquipmentFilterChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentFilterChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentSelfManageChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerGearDataEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementEquipmentEventsSubscription extends SettlementDataSubscriptionHandler {
   public SettlementEquipmentEventsSubscription(SettlementContainer container) {
      super(
         container,
         SettlementNewSettlerEquipmentFilterChangedEvent.class,
         SettlementSettlerEquipmentFilterChangedEvent.class,
         SettlementSettlerEquipmentSelfManageChangedEvent.class,
         SettlementSettlerGearDataEvent.class
      );
   }

   @Override
   public void sendInitialServerEvents(ServerSettlementData data, ServerClient client) {
      new SettlementSettlerEquipmentDataEvent(data, null).applyAndSendToClient(client);
      new SettlementNewSettlerEquipmentFilterChangedEvent(data).applyAndSendToClient(client);
   }

   @Override
   public void sendRequestedDataEvents(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
      new SettlementSettlerEquipmentDataEvent(data, mobUniqueIDs).applyAndSendToClient(client);
   }
}
