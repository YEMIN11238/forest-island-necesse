package necesse.engine.commands.serverCommands;

import java.util.ArrayList;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.commands.parameterHandlers.ServerClientParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;

public class ResetClientRomanceCommand extends ModularChatCommand {
   public ResetClientRomanceCommand() {
      super(
         "resetcharacterromance",
         "Resets all relationships/dating progress for the given client",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("global", new BoolParameterHandler(), true),
         new CmdParameter("player", new ServerClientParameterHandler(true, false), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      ArrayList<String> settlerNames = new ArrayList<>();
      boolean global = (Boolean)args[0];
      ServerClient targetClient = (ServerClient)args[1];
      if (!global && serverClient != null) {
         settlerNames.addAll(this.removeClientFromSettlerRomanceManagers(targetClient, server.world.getLevel(serverClient)));
      } else {
         for (Level level : server.world.levelManager.getLoadedLevels()) {
            settlerNames.addAll(this.removeClientFromSettlerRomanceManagers(targetClient, level));
         }
      }

      for (String settlerName : settlerNames) {
         logs.add("Removed " + targetClient.getName() + " from " + settlerName + "'s romance manager.");
      }
   }

   private ArrayList<String> removeClientFromSettlerRomanceManagers(ServerClient client, Level level) {
      ArrayList<String> settlerNames = new ArrayList<>();

      for (Mob mob : level.entityManager.mobs) {
         if (mob.isHuman) {
            HumanMob settler = (HumanMob)mob;
            settler.getRomancePersonality().debugClearProgress(client);
         }
      }

      return settlerNames;
   }
}
