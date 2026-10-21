package necesse.inventory.container.settlement.events;

import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementStorageFullUpdateEvent extends SettlementContainerEvent {
   public final int tileX;
   public final int tileY;
   public final int priority;
   public final Packet filterContent;

   public SettlementStorageFullUpdateEvent(ServerSettlementData data, int tileX, int tileY, ItemCategoriesFilter filter, int priority) {
      super(data);
      this.tileX = tileX;
      this.tileY = tileY;
      this.priority = priority;
      this.filterContent = new Packet();
      filter.writePacket(new PacketWriter(this.filterContent));
   }

   public SettlementStorageFullUpdateEvent(PacketReader reader) {
      super(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.priority = reader.getNextInt();
      this.filterContent = reader.getNextContentPacket();
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextInt(this.priority);
      writer.putNextContentPacket(this.filterContent);
   }
}
