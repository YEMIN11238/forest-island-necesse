package necesse.inventory.container.settlement;

import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.level.maps.levelData.settlementData.SettlementSettlerGroup;

public class SettlementContainerSettlerGroup {
   public final int groupUniqueID;
   public GameMessage name;
   public HashSet<Integer> mobUniqueIDs;

   public SettlementContainerSettlerGroup(SettlementSettlerGroup group) {
      this.groupUniqueID = group.groupUniqueID;
      this.name = group.getName();
      this.mobUniqueIDs = new HashSet<>(group.getSize());

      for (int mobUniqueID : group.getMobUniqueIDs()) {
         this.mobUniqueIDs.add(mobUniqueID);
      }
   }

   public SettlementContainerSettlerGroup(PacketReader reader) {
      this.groupUniqueID = reader.getNextInt();
      this.name = GameMessage.fromPacket(reader);
      int size = reader.getNextShortUnsigned();
      this.mobUniqueIDs = new HashSet<>(size);

      for (int i = 0; i < size; i++) {
         this.mobUniqueIDs.add(reader.getNextInt());
      }
   }

   public void write(PacketWriter writer) {
      writer.putNextInt(this.groupUniqueID);
      this.name.writePacket(writer);
      writer.putNextShortUnsigned(this.mobUniqueIDs.size());

      for (int mobUniqueID : this.mobUniqueIDs) {
         writer.putNextInt(mobUniqueID);
      }
   }
}
