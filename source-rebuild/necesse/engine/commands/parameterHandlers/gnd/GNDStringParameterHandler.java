package necesse.engine.commands.parameterHandlers.gnd;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemString;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class GNDStringParameterHandler extends GNDItemParameterHandler<GNDItemString> {
   public GNDStringParameterHandler() {
      super("string");
   }

   @Override
   protected List<AutoComplete> autocompleteSecondArg(CmdArgument argument) {
      return Collections.emptyList();
   }

   protected GNDItemString parseSecondArg(String arg, CmdParameter parameter) {
      return new GNDItemString(arg);
   }

   public GNDItemString getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
