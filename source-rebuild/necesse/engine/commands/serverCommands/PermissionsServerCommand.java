package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.PermissionLevelParameterHandler;
import necesse.engine.commands.parameterHandlers.PresetStringParameterHandler;
import necesse.engine.commands.parameterHandlers.ServerClientParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class PermissionsServerCommand extends ModularChatCommand {
   public PermissionsServerCommand() {
      super(
         "permissions",
         "Sets a players permissions",
         PermissionLevel.OWNER,
         false,
         new CmdParameter("list/set/get", new PresetStringParameterHandler("list", "set", "get")),
         new CmdParameter(
            "authentication/name",
            new ServerClientParameterHandler(false, true),
            true,
            new CmdParameter("permissions", new PermissionLevelParameterHandler(), true)
         )
      );
   }

   @Override
   public void runModular(Client var1, Server var2, ServerClient var3, Object[] var4, String[] var5, CommandLog var6) {
      String var7 = (String)var4[0];
      switch (var7) {
         case "list":
            var6.add("Permission levels:");
            StringBuilder var10 = new StringBuilder();
            PermissionLevel[] var11 = PermissionLevel.values();

            for (int var15 = 0; var15 < var11.length; var15++) {
               var10.append(var11[var15].name.translate());
               if (var15 < var11.length - 1) {
                  var10.append(", ");
               }
            }

            var6.add(var10.toString());
            break;
         case "set":
            ServerClient var14 = (ServerClient)var4[1];
            PermissionLevel var13 = (PermissionLevel)var4[2];
            if (var14 == null) {
               var6.add("Missing authentication/name");
               return;
            }

            if (var13 == null) {
               var6.add("Missing permissions");
               return;
            }

            var14.setPermissionLevel(var13, true);
            var6.add("Changed " + var14.getName() + " permissions to " + var13.name.translate());
            break;
         case "get":
            ServerClient var12 = (ServerClient)var4[1];
            if (var12 == null) {
               var6.add("Missing authentication/name");
               return;
            }

            var6.add(var12.getName() + " has " + var12.getPermissionLevel().name.translate() + " permissions");
      }
   }
}
