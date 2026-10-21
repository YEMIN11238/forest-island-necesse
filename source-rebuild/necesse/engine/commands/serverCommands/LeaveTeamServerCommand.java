package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.team.PlayerTeam;

public class LeaveTeamServerCommand extends ModularChatCommand {
   public LeaveTeamServerCommand() {
      super("leaveteam", "Leaves your current team", PermissionLevel.USER, false);
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      if (serverClient == null) {
         logs.add("Command cannot be run from server.");
      } else {
         PlayerTeam currentTeam = serverClient.getPlayerTeam();
         if (currentTeam == null) {
            logs.add(new LocalMessage("ui", "teamnocurrent"));
         } else {
            PlayerTeam.removeMember(server, currentTeam, serverClient.authentication, false);
            logs.add(new LocalMessage("ui", "teamleaved", "name", currentTeam.getName()));
         }
      }
   }
}
