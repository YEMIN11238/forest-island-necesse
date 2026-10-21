package necesse.engine.commands.parameterHandlers;

import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.SettlerRegistry;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class SettlerParameterHandler extends ParameterHandler<Settler> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      return autocompleteFromArray(SettlerRegistry.streamSettlers().toArray(Settler[]::new), i -> true, Settler::getStringID, argument);
   }

   public Settler parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      Settler out = SettlerRegistry.getSettler(arg);
      if (out == null) {
         throw new IllegalArgumentException("Could not find settler with stringID \"" + arg + "\" for <" + parameter.name + ">");
      } else {
         return out;
      }
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public Settler getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
