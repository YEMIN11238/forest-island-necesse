package necesse.engine.commands.serverCommands;

import java.util.Collection;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.SavedServerClientData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PlayerNamesServerCommand extends ModularChatCommand {
   public PlayerNamesServerCommand() {
      super("playernames", "Lists all authentications and their names", PermissionLevel.MODERATOR, false);
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      Collection<SavedServerClientData> savedClients = server.getSavedClients();
      logs.add("Total players stored: " + savedClients.size());

      for (SavedServerClientData savedClient : savedClients) {
         logs.add(savedClient.authentication + " - " + savedClient.name);
      }
   }
}
