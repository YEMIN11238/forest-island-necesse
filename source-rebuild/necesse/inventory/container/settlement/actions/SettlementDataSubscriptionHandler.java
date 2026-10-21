package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.customAction.EventSubscribeEmptyCustomAction;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementContainerEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class SettlementDataSubscriptionHandler {
   public final SettlementContainer container;
   protected final EventSubscribeEmptyCustomAction subscribeAction;
   protected final RequestSettlerDataAbstractAction requestDataAction;
   protected int clientSubscriptionID = -1;

   public SettlementDataSubscriptionHandler(final SettlementContainer container, final Class<? extends SettlementContainerEvent>... eventClasses) {
      this.container = container;
      this.subscribeAction = container.registerAction(new EventSubscribeEmptyCustomAction() {
         @Override
         public void onSubscribed(BooleanSupplier isActive) {
            for (Class<? extends SettlementContainerEvent> eventClass : eventClasses) {
               container.subscribeEvent(eventClass, e -> e.settlementUniqueID == container.getSettlementUniqueID(), isActive);
            }

            if (container.client.isServer()) {
               ServerSettlementData serverData = container.getServerData();
               if (serverData != null) {
                  SettlementDataSubscriptionHandler.this.sendInitialServerEvents(serverData, container.client.getServerClient());
               }
            }
         }
      });
      this.requestDataAction = container.registerAction(new RequestSettlerDataAbstractAction(container) {
         @Override
         public void executePacket(ServerSettlementData data, ServerClient client, Collection<Integer> mobUniqueIDs) {
            SettlementDataSubscriptionHandler.this.sendRequestedDataEvents(data, client, mobUniqueIDs);
         }
      });
   }

   public abstract void sendInitialServerEvents(ServerSettlementData var1, ServerClient var2);

   public abstract void sendRequestedDataEvents(ServerSettlementData var1, ServerClient var2, Collection<Integer> var3);

   public void ensureSubscribed() {
      if (!this.container.client.isClient()) {
         throw new IllegalStateException("Cannot run subscribe from server");
      }

      if (this.clientSubscriptionID == -1) {
         this.clientSubscriptionID = this.subscribeAction.subscribe();
      }
   }

   public void ensureUnsubscribed() {
      if (!this.container.client.isClient()) {
         throw new IllegalStateException("Cannot run unsubscribe from server");
      }

      if (this.clientSubscriptionID != -1) {
         this.subscribeAction.unsubscribe(this.clientSubscriptionID);
         this.clientSubscriptionID = -1;
      }
   }

   public void requestData(Collection<Integer> mobUniqueIDs) {
      if (!this.container.client.isClient()) {
         throw new IllegalStateException("Cannot run request data from server");
      }

      this.requestDataAction.runAndSend(mobUniqueIDs);
   }
}
