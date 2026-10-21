package necesse.inventory.container.mobCommands;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.container.settlement.events.SettlementSettlerGroupDeletedEvent;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementSettlerGroup;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class CommandGroupAddOrRemoveAction extends CommandCustomAction {
   public CommandGroupAddOrRemoveAction(MobCommandsHandler handler) {
      super(handler, true);
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs, int groupUniqueID, boolean addToGroup) {
      PacketWriter writer = this.setupPacket(mobUniqueIDs);
      writer.putNextInt(groupUniqueID);
      writer.putNextBoolean(addToGroup);
      this.runAndSendAction(writer.getPacket());
   }

   @Override
   public void executePacket(PacketReader reader, ServerClient client, ArrayList<CommandMob> mobs) {
      int groupUniqueID = reader.getNextInt();
      boolean addToGroup = reader.getNextBoolean();
      ServerSettlementData serverData = this.handler.settlementContainer.getServerData();
      SettlementSettlerGroup group = serverData.groups.getGroup(groupUniqueID);
      if (group == null) {
         new SettlementSettlerGroupDeletedEvent(serverData, groupUniqueID).applyAndSendToClient(client);
      } else {
         ArrayList<Integer> mobUniqueIDs = new ArrayList<>(mobs.size());

         for (CommandMob commandMob : mobs) {
            mobUniqueIDs.add(commandMob.getMob().getUniqueID());
         }

         if (addToGroup) {
            group.addMobUniqueIDs(mobUniqueIDs, true);
         } else {
            group.removeMobUniqueIDs(mobUniqueIDs, true);
         }
      }
   }
}
