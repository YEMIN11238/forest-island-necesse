package necesse.engine.commands.parameterHandlers.gnd;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemByte;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class GNDByteParameterHandler extends GNDItemParameterHandler<GNDItemByte> {
   public GNDByteParameterHandler() {
      super("byte");
   }

   @Override
   protected List<AutoComplete> autocompleteSecondArg(CmdArgument argument) {
      return Collections.emptyList();
   }

   protected GNDItemByte parseSecondArg(String arg, CmdParameter parameter) {
      try {
         return new GNDItemByte(Byte.parseByte(arg));
      } catch (NumberFormatException e) {
         throw new IllegalArgumentException(arg + " for <" + parameter.name + "> is not a byte");
      }
   }

   public GNDItemByte getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
