package necesse.engine.commands.clientCommands;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PlayCreditsClientCommand extends ModularChatCommand {
   public PlayCreditsClientCommand() {
      super("playcredits", "Plays the credits", PermissionLevel.USER, false);
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      client.startCreditsDraw();
   }
}
