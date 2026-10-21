package necesse.engine.commands.parameterHandlers.gnd;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.gameNetworkData.GNDItemBoolean;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;

public class GNDBooleanParameterHandler extends GNDItemParameterHandler<GNDItemBoolean> {
   public GNDBooleanParameterHandler() {
      super("bool");
   }

   @Override
   protected List<AutoComplete> autocompleteSecondArg(CmdArgument argument) {
      return Collections.emptyList();
   }

   protected GNDItemBoolean parseSecondArg(String arg, CmdParameter parameter) {
      for (String s : BoolParameterHandler.validTrue) {
         if (arg.equalsIgnoreCase(s)) {
            return new GNDItemBoolean(true);
         }
      }

      for (String s : BoolParameterHandler.validFalse) {
         if (arg.equalsIgnoreCase(s)) {
            return new GNDItemBoolean(false);
         }
      }

      throw new IllegalArgumentException(
         arg
            + " for <"
            + parameter.name
            + "> must be either "
            + GameUtils.join(GameUtils.concat(BoolParameterHandler.validTrue, BoolParameterHandler.validFalse), ", ", " or ")
      );
   }

   public GNDItemBoolean getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
