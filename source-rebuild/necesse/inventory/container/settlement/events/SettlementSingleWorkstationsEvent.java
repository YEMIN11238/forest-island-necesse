package necesse.inventory.container.settlement.events;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSingleWorkstationsEvent extends SettlementContainerEvent {
   public final int tileX;
   public final int tileY;
   public final boolean exists;

   public SettlementSingleWorkstationsEvent(ServerSettlementData data, int tileX, int tileY) {
      super(data);
      this.tileX = tileX;
      this.tileY = tileY;
      this.exists = data.storageManager.getWorkstation(tileX, tileY) != null;
   }

   public SettlementSingleWorkstationsEvent(PacketReader reader) {
      super(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.exists = reader.getNextBoolean();
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextBoolean(this.exists);
   }
}
