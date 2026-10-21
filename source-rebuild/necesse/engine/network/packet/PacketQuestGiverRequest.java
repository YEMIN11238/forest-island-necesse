package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.QuestGiver;

public class PacketQuestGiverRequest extends Packet {
   public final int mobUniqueID;

   public PacketQuestGiverRequest(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.mobUniqueID = reader.getNextInt();
   }

   public PacketQuestGiverRequest(int mobUniqueID) {
      this.mobUniqueID = mobUniqueID;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(mobUniqueID);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      Mob mob = server.world.getLevel(client).entityManager.mobs.get(this.mobUniqueID, false);
      if (mob != null) {
         if (mob instanceof QuestGiver) {
            client.sendPacket(new PacketQuestGiverUpdate((QuestGiver)mob, client));
         }
      } else {
         server.network.sendPacket(new PacketRemoveMob(this.mobUniqueID), client);
      }
   }
}
