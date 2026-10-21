package necesse.engine.commands.clientCommands;

import necesse.engine.Settings;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class MaxFPSClientCommand extends ModularChatCommand {
   public MaxFPSClientCommand() {
      super("maxfps", "Sets max fps", PermissionLevel.USER, false, new CmdParameter("fps", new IntParameterHandler(), false));
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      int fps = (Integer)args[0];
      Settings.maxFPS = fps;
      Settings.saveClientSettings();
      logs.add("Set max fps to " + fps);
   }
}
