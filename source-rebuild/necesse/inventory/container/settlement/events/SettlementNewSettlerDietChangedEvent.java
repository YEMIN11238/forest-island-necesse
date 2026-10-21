package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementNewSettlerDietChangedEvent extends SettlementContainerEvent {
   public final ItemCategoriesFilterChange change;

   public SettlementNewSettlerDietChangedEvent(ServerSettlementData data) {
      super(data);
      this.change = ItemCategoriesFilterChange.fullChange(data.getNewSettlerDiet());
   }

   public SettlementNewSettlerDietChangedEvent(ServerSettlementData data, ItemCategoriesFilterChange change) {
      super(data);
      this.change = change;
   }

   public SettlementNewSettlerDietChangedEvent(PacketReader reader) {
      super(reader);
      this.change = ItemCategoriesFilterChange.fromPacket(reader);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      this.change.write(writer);
   }
}
