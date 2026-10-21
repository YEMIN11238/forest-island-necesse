package necesse.inventory.container.settlement.events;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementWorkstation;

public class SettlementWorkstationsEvent extends SettlementContainerEvent {
   public final ArrayList<Point> workstations;

   public SettlementWorkstationsEvent(ServerSettlementData data) {
      super(data);
      Collection<SettlementWorkstation> stations = data.storageManager.getWorkstations();
      this.workstations = new ArrayList<>(stations.size());

      for (SettlementWorkstation station : stations) {
         this.workstations.add(new Point(station.tileX, station.tileY));
      }
   }

   public SettlementWorkstationsEvent(PacketReader reader) {
      super(reader);
      int size = reader.getNextShortUnsigned();
      this.workstations = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         int tileX = reader.getNextInt();
         int tileY = reader.getNextInt();
         this.workstations.add(new Point(tileX, tileY));
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextShortUnsigned(this.workstations.size());

      for (Point point : this.workstations) {
         writer.putNextInt(point.x);
         writer.putNextInt(point.y);
      }
   }
}
