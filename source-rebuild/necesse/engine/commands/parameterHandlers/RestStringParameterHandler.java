package necesse.engine.commands.parameterHandlers;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class RestStringParameterHandler extends ParameterHandler<String> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      return Collections.emptyList();
   }

   public String parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      return arg;
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return true;
   }

   public String getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return "";
   }

   @Override
   public int getArgsUsed() {
      return 10000;
   }
}
