package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.team.PlayerTeam;

public class CreateTeamServerCommand extends ModularChatCommand {
   public CreateTeamServerCommand() {
      super("createteam", "Creates a new team for yourself", PermissionLevel.USER, false);
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      if (serverClient == null) {
         logs.add("Command cannot be run from server.");
      } else if (serverClient.getPlayerTeam() != null) {
         logs.add(new LocalMessage("ui", "teamcreateleave"));
      } else {
         PlayerTeam newTeam = server.world.getTeams().createNewTeam(serverClient);
         logs.add(new LocalMessage("ui", "teamcreated", "name", newTeam.getName()));
      }
   }
}
