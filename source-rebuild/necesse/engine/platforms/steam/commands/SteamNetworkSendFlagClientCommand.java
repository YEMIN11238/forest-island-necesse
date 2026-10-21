package necesse.engine.platforms.steam.commands;

import com.codedisaster.steamworks.SteamNetworkingMessages.SendFlag;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.EnumParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.platforms.steam.network.networkInfo.SteamNetworkMessagesInfo;

public class SteamNetworkSendFlagClientCommand extends ModularChatCommand {
   public SteamNetworkSendFlagClientCommand() {
      super(
         "steamnetwork",
         "Sets steam network send flag",
         PermissionLevel.USER,
         false,
         new CmdParameter("flag", new EnumParameterHandler<>(SendFlag.values()), false)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      SteamNetworkMessagesInfo.sendFlag = (SendFlag)args[0];
      logs.add("Steam network send tag set to " + SteamNetworkMessagesInfo.sendFlag);
   }
}
