package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.HelpFormParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.gfx.forms.presets.HelpForms;

public class OpenHelpFormClientCommand extends ModularChatCommand {
   public OpenHelpFormClientCommand() {
      super("openhelpform", "Opens a predefined help form", PermissionLevel.USER, false, new CmdParameter("form", new HelpFormParameterHandler()));
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      String helpFormStringID = (String)args[0];
      HelpForms.openHelpForm(helpFormStringID);
   }
}
