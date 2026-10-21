package necesse.inventory.container.mobCommands;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class CommandSetHideOnLowHealthAction extends CommandCustomAction {
   public CommandSetHideOnLowHealthAction(MobCommandsHandler handler) {
      super(handler, true);
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs, boolean shouldHideInside) {
      PacketWriter writer = this.setupPacket(mobUniqueIDs);
      writer.putNextBoolean(shouldHideInside);
      this.runAndSendAction(writer.getPacket());
   }

   @Override
   public void executePacket(PacketReader reader, ServerClient client, ArrayList<CommandMob> mobs) {
      boolean shouldHideInside = reader.getNextBoolean();

      for (CommandMob mob : mobs) {
         mob.setHideOnLowHealth(shouldHideInside);
      }
   }
}
