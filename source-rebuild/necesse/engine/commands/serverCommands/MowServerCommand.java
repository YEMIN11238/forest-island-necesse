package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.FloatParameterHandler;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.level.maps.Level;

public class MowServerCommand extends ModularChatCommand {
   public MowServerCommand() {
      super(
         "mow",
         "Mows ground of grass in range with percent chance",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("range", new IntParameterHandler()),
         new CmdParameter("chance", new FloatParameterHandler(100.0F), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      if (serverClient == null) {
         logs.add("Cannot run mow from server console.");
      } else {
         int range = (Integer)args[0];
         float chance = (Float)args[1];
         float actualChance = chance / 100.0F;
         Level level = server.world.getLevel(serverClient);
         int startX = level.limitTileXToBounds(serverClient.playerMob.getTileX() - range);
         int startY = level.limitTileXToBounds(serverClient.playerMob.getTileY() - range);
         int endX = level.limitTileXToBounds(serverClient.playerMob.getTileX() + range);
         int endY = level.limitTileXToBounds(serverClient.playerMob.getTileY() + range);
         int grassID = ObjectRegistry.getObjectID("grass");

         for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
               if (level.getObjectID(x, y) == grassID && GameRandom.globalRandom.getChance(actualChance)) {
                  level.sendObjectChangePacket(server, x, y, 0);
               }
            }
         }

         logs.add("Mowed grass in a range of " + range + " around you.");
      }
   }
}
