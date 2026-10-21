package necesse.inventory.container.settlement.actions;

import java.util.function.BooleanSupplier;
import necesse.inventory.container.customAction.EventSubscribeEmptyCustomAction;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.events.SettlementSettlerActivityDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerCommandDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerLooksDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerOverviewDataEvent;

public class SubscribeSettlerBasicsAction extends EventSubscribeEmptyCustomAction {
   public final SettlementContainer container;

   public SubscribeSettlerBasicsAction(SettlementContainer container) {
      this.container = container;
   }

   @Override
   public void onSubscribed(BooleanSupplier isActive) {
      this.container.subscribeEvent(SettlementSettlerBasicsEvent.class, e -> e.settlementUniqueID == this.container.getSettlementUniqueID(), isActive);
      this.container.subscribeEvent(SettlementSettlerLooksDataEvent.class, e -> e.settlementUniqueID == this.container.getSettlementUniqueID(), isActive);
      this.container.subscribeEvent(SettlementSettlerActivityDataEvent.class, e -> e.settlementUniqueID == this.container.getSettlementUniqueID(), isActive);
      this.container.subscribeEvent(SettlementSettlerCommandDataEvent.class, e -> e.settlementUniqueID == this.container.getSettlementUniqueID(), isActive);
      this.container.subscribeEvent(SettlementSettlerOverviewDataEvent.class, e -> e.settlementUniqueID == this.container.getSettlementUniqueID(), isActive);
   }
}
