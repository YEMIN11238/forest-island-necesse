package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.commands.parameterHandlers.ServerClientParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class InvincibilityServerCommand extends ModularChatCommand {
   public InvincibilityServerCommand() {
      super(
         "invincibility",
         "Sets a players invincibility",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("player", new ServerClientParameterHandler(true, false), true),
         new CmdParameter("1/0", new BoolParameterHandler(null), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      ServerClient target = (ServerClient)args[0];
      Boolean value = (Boolean)args[1];
      if (target == null) {
         logs.add("Must specify <player>");
      } else {
         if (value == null) {
            value = !target.playerMob.hasInvincibility;
         }

         target.playerMob.hasInvincibility = value;
         if (value) {
            logs.add("You are now invincible!");
         } else {
            logs.add("You are now no longer invincible!");
         }

         if (target != serverClient) {
            if (value) {
               logs.add(target.getName() + " is now invincible!");
            } else {
               logs.add(target.getName() + " is now no longer invincible!");
            }
         }
      }
   }
}
