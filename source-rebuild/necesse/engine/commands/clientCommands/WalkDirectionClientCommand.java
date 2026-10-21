package necesse.engine.commands.clientCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.WalkDirectionAINode;

public class WalkDirectionClientCommand extends ModularChatCommand {
   public WalkDirectionClientCommand() {
      super(
         "walkdirection",
         "Start walking in a direction",
         PermissionLevel.USER,
         false,
         new CmdParameter("dirX", new IntParameterHandler(), false),
         new CmdParameter("dirY", new IntParameterHandler(), false),
         new CmdParameter("tileSearchDistance", new IntParameterHandler(20), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      int dirX = (Integer)args[0];
      int dirY = (Integer)args[1];
      int tileSearchDistance = (Integer)args[2];
      if (dirX == 0 && dirY == 0) {
         logs.add("Direction cannot be (0,0)");
      } else {
         PlayerMob player = client.getPlayer();
         player.ai = new BehaviourTreeAI<>(
            player, new WalkDirectionAINode<PlayerMob>(dirX, dirY, tileSearchDistance).setKeepCenterTile(player.getTileX(), player.getTileY())
         );
         logs.add("Started walking in direction (" + dirX + ", " + dirY + ")");
      }
   }
}
