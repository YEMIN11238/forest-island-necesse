package necesse.engine.commands.parameterHandlers.gnd;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemInt;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class GNDIntParameterHandler extends GNDItemParameterHandler<GNDItemInt> {
   public GNDIntParameterHandler() {
      super("int");
   }

   @Override
   protected List<AutoComplete> autocompleteSecondArg(CmdArgument argument) {
      return Collections.emptyList();
   }

   protected GNDItemInt parseSecondArg(String arg, CmdParameter parameter) {
      try {
         return new GNDItemInt(Integer.parseInt(arg));
      } catch (NumberFormatException e) {
         throw new IllegalArgumentException(arg + " for <" + parameter.name + "> is not an int");
      }
   }

   public GNDItemInt getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
