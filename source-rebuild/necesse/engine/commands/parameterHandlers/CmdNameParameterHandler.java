package necesse.engine.commands.parameterHandlers;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.ChatCommand;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class CmdNameParameterHandler extends ParameterHandler<ChatCommand> {
   private boolean ignoreOnlyHelpCommands;

   public CmdNameParameterHandler(boolean ignoreOnlyHelpCommands) {
      this.ignoreOnlyHelpCommands = ignoreOnlyHelpCommands;
   }

   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      if (client != null) {
         return autocompleteFromList(
            client.commandsManager.getCommands(), cmd -> !cmd.onlyForHelp() && cmd.havePermissions(client, server, serverClient), cmd -> cmd.name, argument
         );
      } else {
         return server != null
            ? autocompleteFromList(
               server.commandsManager.getServerCommands(),
               cmd -> !cmd.onlyForHelp() && cmd.havePermissions(client, server, serverClient),
               cmd -> cmd.name,
               argument
            )
            : Collections.emptyList();
      }
   }

   public ChatCommand parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      ChatCommand cmd = null;

      for (ChatCommand c : client != null ? client.commandsManager.getCommands() : server.commandsManager.getServerCommands()) {
         if ((!c.onlyForHelp() || !this.ignoreOnlyHelpCommands) && c.name.equalsIgnoreCase(arg)) {
            cmd = c;
            break;
         }
      }

      if (cmd != null && cmd.onlyForHelp() && this.ignoreOnlyHelpCommands) {
         cmd = null;
      }

      if (cmd == null) {
         throw new IllegalArgumentException("Could not find command \"" + arg + "\" for <" + parameter.name + ">");
      } else {
         return cmd;
      }
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public ChatCommand getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
