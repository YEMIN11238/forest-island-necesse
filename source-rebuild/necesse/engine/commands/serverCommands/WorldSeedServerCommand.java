package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class WorldSeedServerCommand extends ModularChatCommand {
   public WorldSeedServerCommand() {
      super("worldseed", "Prints the world seed", PermissionLevel.OWNER, false);
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      logs.add("World seed: " + server.world.worldEntity.worldSeed);
   }
}
