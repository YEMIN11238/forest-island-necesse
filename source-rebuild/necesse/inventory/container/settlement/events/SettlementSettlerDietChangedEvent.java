package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerDietChangedEvent extends SettlementContainerEvent {
   public final int mobUniqueID;
   public final ItemCategoriesFilterChange change;

   public SettlementSettlerDietChangedEvent(ServerSettlementData data, int mobUniqueID, ItemCategoriesFilterChange change) {
      super(data);
      this.mobUniqueID = mobUniqueID;
      this.change = change;
   }

   public SettlementSettlerDietChangedEvent(PacketReader reader) {
      super(reader);
      this.mobUniqueID = reader.getNextInt();
      this.change = ItemCategoriesFilterChange.fromPacket(reader);
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.mobUniqueID);
      this.change.write(writer);
   }
}
