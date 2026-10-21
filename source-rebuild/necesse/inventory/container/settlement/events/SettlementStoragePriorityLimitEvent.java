package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementStoragePriorityLimitEvent extends SettlementContainerEvent {
   public final int tileX;
   public final int tileY;
   public final boolean isPriority;
   public final int priority;
   public final ItemCategoriesFilter.ItemLimitMode limitMode;
   public final int limit;

   public SettlementStoragePriorityLimitEvent(ServerSettlementData data, int tileX, int tileY, int priority) {
      super(data);
      this.tileX = tileX;
      this.tileY = tileY;
      this.isPriority = true;
      this.priority = priority;
      this.limitMode = null;
      this.limit = 0;
   }

   public SettlementStoragePriorityLimitEvent(ServerSettlementData data, int tileX, int tileY, ItemCategoriesFilter.ItemLimitMode mode, int limit) {
      super(data);
      this.tileX = tileX;
      this.tileY = tileY;
      this.isPriority = false;
      this.limitMode = mode;
      this.limit = limit;
      this.priority = 0;
   }

   public SettlementStoragePriorityLimitEvent(PacketReader reader) {
      super(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.isPriority = reader.getNextBoolean();
      if (this.isPriority) {
         this.priority = reader.getNextInt();
         this.limitMode = null;
         this.limit = 0;
      } else {
         this.priority = 0;
         this.limitMode = reader.getNextEnum(ItemCategoriesFilter.ItemLimitMode.class);
         this.limit = reader.getNextInt();
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextBoolean(this.isPriority);
      if (this.isPriority) {
         writer.putNextInt(this.priority);
      } else {
         writer.putNextEnum(this.limitMode);
         writer.putNextInt(this.limit);
      }
   }
}
