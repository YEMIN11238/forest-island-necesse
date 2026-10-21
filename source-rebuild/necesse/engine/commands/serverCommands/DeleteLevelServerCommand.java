package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.LevelIdentifierParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.level.maps.Level;

public class DeleteLevelServerCommand extends ModularChatCommand {
   public DeleteLevelServerCommand() {
      super(
         "deletelevel",
         "Deletes a level if possible and teleports players back to their fallback level",
         PermissionLevel.OWNER,
         true,
         new CmdParameter("level", new LevelIdentifierParameterHandler(null), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      LevelIdentifier levelIdentifier = (LevelIdentifier)args[0];
      if (levelIdentifier == null) {
         if (serverClient == null) {
            logs.add("Please specify level");
            return;
         }

         levelIdentifier = serverClient.getLevelIdentifier();
      }

      Level level;
      if (server.world.levelManager.isLoaded(levelIdentifier)) {
         level = server.world.getLevel(levelIdentifier);
      } else if (server.world.fileSystem.levelFileExists(levelIdentifier)) {
         level = server.world.getLevel(levelIdentifier);
      } else {
         level = null;
      }

      if (level == null) {
         logs.add("Cannot delete level " + levelIdentifier + " because it does not exist.");
      } else if (level.fallbackIdentifier == null) {
         logs.add("Cannot delete level " + levelIdentifier + " it has no fallback level.");
      } else {
         server.world.levelManager.deleteLevel(levelIdentifier, null);
         SettlementsWorldData.getSettlementsData(server).deleteSettlementsAt(levelIdentifier);
         logs.add("Deleted level " + levelIdentifier + " and teleported all players back to their fallbacks");
      }
   }
}
