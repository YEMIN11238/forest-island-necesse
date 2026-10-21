package necesse.engine.commands.serverCommands.setupCommand;

import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.parameterHandlers.ParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class WorldSetupParameterHandler extends ParameterHandler<WorldSetupEntry> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      return autocompleteFromArray((String[])DemoServerCommand.setups.keySet().toArray(new String[0]), null, null, argument);
   }

   public WorldSetupEntry parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      WorldSetup setup = DemoServerCommand.setups.get(arg);
      if (setup != null) {
         return new WorldSetupEntry(arg, setup);
      } else {
         throw new IllegalArgumentException("Could not find setup with name \"" + arg + "\" for <" + parameter.name + ">");
      }
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public WorldSetupEntry getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
