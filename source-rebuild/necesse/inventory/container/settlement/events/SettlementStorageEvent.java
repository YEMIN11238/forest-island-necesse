package necesse.inventory.container.settlement.events;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementInventory;

public class SettlementStorageEvent extends SettlementContainerEvent {
   public final ArrayList<Point> storage;

   public SettlementStorageEvent(ServerSettlementData data) {
      super(data);
      Collection<SettlementInventory> settlementStorage = data.storageManager.getStorage();
      this.storage = new ArrayList<>(settlementStorage.size());

      for (SettlementInventory inv : settlementStorage) {
         this.storage.add(new Point(inv.tileX, inv.tileY));
      }
   }

   public SettlementStorageEvent(PacketReader reader) {
      super(reader);
      int size = reader.getNextShortUnsigned();
      this.storage = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         int tileX = reader.getNextInt();
         int tileY = reader.getNextInt();
         this.storage.add(new Point(tileX, tileY));
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextShortUnsigned(this.storage.size());

      for (Point point : this.storage) {
         writer.putNextInt(point.x);
         writer.putNextInt(point.y);
      }
   }
}
