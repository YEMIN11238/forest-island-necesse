package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.itemFilter.ItemCategoriesFilterChange;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementNewSettlerEquipmentFilterChangedEvent extends SettlementContainerEvent {
   public final boolean selfManageEquipment;
   public final boolean preferArmorSets;
   public final ItemCategoriesFilterChange change;

   public SettlementNewSettlerEquipmentFilterChangedEvent(ServerSettlementData data) {
      super(data);
      this.selfManageEquipment = data.newSettlerSelfManageEquipment;
      this.preferArmorSets = data.newSettlerEquipmentPreferArmorSets;
      this.change = ItemCategoriesFilterChange.fullChange(data.getNewSettlerEquipmentFilter());
   }

   public SettlementNewSettlerEquipmentFilterChangedEvent(
      ServerSettlementData data, boolean selfManageEquipment, boolean preferArmorSets, ItemCategoriesFilterChange change
   ) {
      super(data);
      this.selfManageEquipment = selfManageEquipment;
      this.preferArmorSets = preferArmorSets;
      this.change = change;
   }

   public SettlementNewSettlerEquipmentFilterChangedEvent(PacketReader reader) {
      super(reader);
      this.selfManageEquipment = reader.getNextBoolean();
      this.preferArmorSets = reader.getNextBoolean();
      if (reader.getNextBoolean()) {
         this.change = ItemCategoriesFilterChange.fromPacket(reader);
      } else {
         this.change = null;
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextBoolean(this.selfManageEquipment);
      writer.putNextBoolean(this.preferArmorSets);
      writer.putNextBoolean(this.change != null);
      if (this.change != null) {
         this.change.write(writer);
      }
   }
}
