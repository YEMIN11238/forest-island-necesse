package necesse.inventory.container.settlement.events;

import java.util.ArrayList;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.container.events.ContainerEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerGroupChangedEvent extends ContainerEvent {
   public final int settlementUniqueID;
   public final int groupUniqueID;
   public final boolean addedToGroup;
   public final ArrayList<Integer> mobUniqueIDs;

   public SettlementSettlerGroupChangedEvent(ServerSettlementData data, int groupUniqueID, boolean addedToGroup, ArrayList<Integer> mobUniqueIDs) {
      this.settlementUniqueID = data.uniqueID;
      this.groupUniqueID = groupUniqueID;
      this.addedToGroup = addedToGroup;
      this.mobUniqueIDs = mobUniqueIDs;
   }

   public SettlementSettlerGroupChangedEvent(PacketReader reader) {
      super(reader);
      this.settlementUniqueID = reader.getNextInt();
      this.groupUniqueID = reader.getNextInt();
      this.addedToGroup = reader.getNextBoolean();
      int size = reader.getNextShortUnsigned();
      this.mobUniqueIDs = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         this.mobUniqueIDs.add(reader.getNextInt());
      }
   }

   @Override
   public void write(PacketWriter writer) {
      writer.putNextInt(this.settlementUniqueID);
      writer.putNextInt(this.groupUniqueID);
      writer.putNextBoolean(this.addedToGroup);
      writer.putNextShortUnsigned(this.mobUniqueIDs.size());

      for (int mobUniqueID : this.mobUniqueIDs) {
         writer.putNextInt(mobUniqueID);
      }
   }
}
