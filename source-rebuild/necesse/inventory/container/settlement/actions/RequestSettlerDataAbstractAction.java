package necesse.inventory.container.settlement.actions;

import java.util.Collection;
import java.util.HashSet;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.SettlementDependantContainer;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class RequestSettlerDataAbstractAction extends SettlementAccessRequiredContainerCustomAction {
   public RequestSettlerDataAbstractAction(SettlementDependantContainer container) {
      super(container);
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      if (mobUniqueIDs == null) {
         writer.putNextBoolean(true);
      } else {
         writer.putNextBoolean(false);
         writer.putNextShortUnsigned(mobUniqueIDs.size());

         for (int mobUniqueID : mobUniqueIDs) {
            writer.putNextInt(mobUniqueID);
         }
      }

      this.runAndSendAction(packet);
   }

   @Override
   public final void executePacket(PacketReader reader, ServerSettlementData data, ServerClient client) {
      if (reader.getNextBoolean()) {
         HashSet<Integer> mobUniqueIDs = new HashSet<>(data.countTotalSettlers());

         for (LevelSettler settler : data.getSettlers()) {
            mobUniqueIDs.add(settler.mobUniqueID);
         }

         this.executePacket(data, client, mobUniqueIDs);
      } else {
         int size = reader.getNextShortUnsigned();
         HashSet<Integer> mobUniqueIDs = new HashSet<>();

         for (int i = 0; i < size; i++) {
            mobUniqueIDs.add(reader.getNextInt());
         }

         this.executePacket(data, client, mobUniqueIDs);
      }
   }

   public abstract void executePacket(ServerSettlementData var1, ServerClient var2, Collection<Integer> var3);
}
