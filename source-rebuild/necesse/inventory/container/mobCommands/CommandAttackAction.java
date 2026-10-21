package necesse.inventory.container.mobCommands;

import java.util.ArrayList;
import java.util.Collection;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketRemoveMob;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;

public class CommandAttackAction extends CommandCustomAction {
   public CommandAttackAction(MobCommandsHandler handler) {
      super(handler, false);
   }

   public void runAndSend(Collection<Integer> mobUniqueIDs, Mob target) {
      PacketWriter writer = this.setupPacket(mobUniqueIDs);
      writer.putNextInt(target.getUniqueID());
      this.runAndSendAction(writer.getPacket());
   }

   @Override
   public void executePacket(PacketReader reader, ServerClient client, ArrayList<CommandMob> mobs) {
      int targetUniqueID = reader.getNextInt();
      Mob targetMob = GameUtils.getLevelMob(targetUniqueID, client.getLevel());
      if (targetMob != null) {
         for (CommandMob mob : mobs) {
            mob.commandAttack(client, targetMob);
         }
      } else {
         client.sendPacket(new PacketRemoveMob(targetUniqueID));
      }
   }
}
