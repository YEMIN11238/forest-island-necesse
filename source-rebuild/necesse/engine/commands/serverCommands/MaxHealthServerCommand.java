package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.commands.parameterHandlers.ServerClientParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketPlayerGeneral;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class MaxHealthServerCommand extends ModularChatCommand {
   public MaxHealthServerCommand() {
      super(
         "maxhp",
         "Sets the max health of player",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("player", new ServerClientParameterHandler(true, false), true),
         new CmdParameter("health", new IntParameterHandler()),
         new CmdParameter("ignoreHealthUpgrades", new BoolParameterHandler(false), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      ServerClient player = (ServerClient)args[0];
      int amount = (Integer)args[1];
      boolean ignoreHealthUpgrades = (Boolean)args[2];
      if (player == null) {
         logs.add("Must specify <player>");
      } else {
         if (ignoreHealthUpgrades) {
            player.playerMob.setMaxHealth(amount);
         } else {
            player.playerMob.healthUpgradeManager.resetHealthUpgrades();
            player.playerMob.healthUpgradeManager.migrateHealthUpgrades(player.playerMob, amount);
         }

         int currentIncrease = player.playerMob.healthUpgradeManager.getCurrentMaxHealthIncrease();
         server.network.sendToAllClients(new PacketPlayerGeneral(player));
         logs.add(
            "Set "
               + player.getName()
               + " max health to "
               + (player.playerMob.getMaxHealthFlat() + currentIncrease)
               + " total ("
               + player.playerMob.getMaxHealthFlat()
               + " base + "
               + currentIncrease
               + " upgrades)"
         );
      }
   }
}
