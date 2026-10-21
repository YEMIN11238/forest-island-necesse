package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.gfx.fairType.TypeParsers;

public class PlayersServerCommand extends ModularChatCommand {
   public PlayersServerCommand() {
      super("players", "Lists players currently online", PermissionLevel.MODERATOR, false);
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      int online = 0;

      for (int i = 0; i < server.getSlots(); i++) {
         if (server.getClient(i) != null) {
            online++;
         }
      }

      logs.add("Players online: " + online + "/" + server.getSlots());

      for (int i = 0; i < server.getSlots(); i++) {
         if (server.getClient(i) != null) {
            String hostAddress = server.getClient(i).networkInfo == null ? "LOCAL" : server.getClient(i).networkInfo.getDisplayName();
            String nameString = serverClient == null ? server.getClient(i).getName() : TypeParsers.getTeleportParseString(server.getClient(i));
            String levelIdentifierString = serverClient == null
               ? server.getClient(i).getLevelIdentifier().toString()
               : TypeParsers.getTeleportParseString(server.getClient(i).getLevelIdentifier());
            logs.add(
               "Slot "
                  + (i + 1)
                  + ": "
                  + server.getClient(i).authentication
                  + " \""
                  + nameString
                  + "\", latency: "
                  + server.getClient(i).latency
                  + ", level: "
                  + levelIdentifierString
                  + ",conn: "
                  + hostAddress
            );
         }
      }
   }
}
