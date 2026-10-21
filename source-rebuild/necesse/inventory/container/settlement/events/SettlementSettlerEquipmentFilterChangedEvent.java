package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerEquipmentFilterChangedEvent extends SettlementContainerEvent {
   public final int mobUniqueID;
   public final boolean preferArmorSets;
   public final ItemCategoriesFilterChange change;

   public SettlementSettlerEquipmentFilterChangedEvent(ServerSettlementData data, int mobUniqueID, boolean preferArmorSets, ItemCategoriesFilterChange change) {
      super(data);
      this.mobUniqueID = mobUniqueID;
      this.change = change;
      this.preferArmorSets = preferArmorSets;
   }

   public SettlementSettlerEquipmentFilterChangedEvent(PacketReader reader) {
      super(reader);
      this.mobUniqueID = reader.getNextInt();
      this.preferArmorSets = reader.getNextBoolean();
      if (reader.getNextBoolean()) {
         this.change = ItemCategoriesFilterChange.fromPacket(reader);
      } else {
         this.change = null;
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.mobUniqueID);
      writer.putNextBoolean(this.preferArmorSets);
      writer.putNextBoolean(this.change != null);
      if (this.change != null) {
         this.change.write(writer);
      }
   }
}
