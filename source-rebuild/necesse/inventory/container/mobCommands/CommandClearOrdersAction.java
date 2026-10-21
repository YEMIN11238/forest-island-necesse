package necesse.inventory.container.mobCommands;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class CommandClearOrdersAction extends CommandCustomAction {
   public CommandClearOrdersAction(MobCommandsHandler handler) {
      super(handler, true);
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs) {
      PacketWriter writer = this.setupPacket(mobUniqueIDs);
      this.runAndSendAction(writer.getPacket());
   }

   @Override
   public void executePacket(PacketReader reader, ServerClient client, ArrayList<CommandMob> mobs) {
      for (CommandMob mob : mobs) {
         mob.clearCommandsOrders(client);
      }
   }
}
