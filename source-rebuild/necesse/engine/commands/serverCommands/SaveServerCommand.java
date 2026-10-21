package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.network.server.ServerSaveHandler;

public class SaveServerCommand extends ModularChatCommand {
   public SaveServerCommand() {
      super("save", "Saves all data", PermissionLevel.MODERATOR, false, new CmdParameter("recordPerformance", new BoolParameterHandler(false), true));
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, final CommandLog logs) {
      boolean recordPerformance = (Boolean)args[0];
      boolean startedSave = server.startFullSave(false, recordPerformance, new ServerSaveHandler.SaveInterruptedOrFinishedEventHandler() {
         @Override
         public void onSaveInterrupted(ServerSaveHandler newSaveHandler) {
            logs.add("Save interrupted by a new save request");
         }

         @Override
         public void onSaveFinished() {
            logs.add("Completed world save");
         }
      });
      if (startedSave) {
         logs.add("Starting save...");
      } else {
         logs.add("A save is already in progress");
      }
   }
}
