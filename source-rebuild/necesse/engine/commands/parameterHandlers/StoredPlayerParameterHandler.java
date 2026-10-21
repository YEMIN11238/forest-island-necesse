package necesse.engine.commands.parameterHandlers;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.SavedServerClientData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.world.WorldFile;

public class StoredPlayerParameterHandler extends ParameterHandler<StoredPlayerParameterHandler.StoredPlayer> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      if (server != null) {
         List<AutoComplete> out = autocompleteFromCollection(server.getSavedClients(), v -> true, v -> v.name, argument);
         out.addAll(autocompleteFromCollection(server.getSavedClients(), v -> true, v -> String.valueOf(v.authentication), argument));
         return out;
      } else {
         return Collections.emptyList();
      }
   }

   public StoredPlayerParameterHandler.StoredPlayer parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      try {
         long argAuth = Long.parseLong(arg);
         SavedServerClientData savedClient = server.getSavedClientData(argAuth);
         if (savedClient != null) {
            return new StoredPlayerParameterHandler.StoredPlayer(server, savedClient.name, argAuth);
         }
      } catch (NumberFormatException var9) {
      }

      for (SavedServerClientData savedClient : server.getSavedClients()) {
         String name = savedClient.name;
         if (name.equalsIgnoreCase(arg) || String.valueOf(savedClient.authentication).equals(arg)) {
            return new StoredPlayerParameterHandler.StoredPlayer(server, name, savedClient.authentication);
         }
      }

      throw new IllegalArgumentException("Could not find player with name/auth \"" + arg + "\"");
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public StoredPlayerParameterHandler.StoredPlayer getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }

   public static class StoredPlayer {
      public long authentication;
      public String name;
      public WorldFile file;
      public WorldFile mapFile;

      public StoredPlayer(Server server, String name, long authentication) {
         this.name = name;
         this.authentication = authentication;
         this.file = server.world.fileSystem.getPlayerFile(authentication);
         this.mapFile = server.world.fileSystem.getMapPlayerFile(authentication);
      }
   }
}
