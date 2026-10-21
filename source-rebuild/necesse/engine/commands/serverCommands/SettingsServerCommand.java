package necesse.engine.commands.serverCommands;

import java.util.ArrayList;
import necesse.engine.Settings;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.BoolParameterHandler;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.commands.parameterHandlers.ParameterHandler;
import necesse.engine.commands.parameterHandlers.PresetStringParameterHandler;
import necesse.engine.commands.parameterHandlers.RestStringParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;

public class SettingsServerCommand extends ModularChatCommand {
   public static ArrayList<SettingsServerCommand.ServerSettingCommand<?>> settings = new ArrayList<>();

   private static String[] presetParams() {
      return settings.stream().map(s -> s.commandString).toArray(String[]::new);
   }

   private static ParameterHandler<?>[] handlers() {
      return settings.stream().map(s -> s.handler).toArray(ParameterHandler[]::new);
   }

   public SettingsServerCommand() {
      super(
         "settings",
         "Change server world settings",
         PermissionLevel.ADMIN,
         false,
         new CmdParameter("list/setting", new PresetStringParameterHandler(GameUtils.concat(new String[]{"list"}, presetParams()))),
         new CmdParameter("arg", new RestStringParameterHandler(), true)
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      String setting = (String)args[0];
      String argStr = (String)args[1];
      argStr = argStr.trim();
      if (setting.equals("list")) {
         logs.add("Settings list:");
         logs.add(GameUtils.join(presetParams(), ", "));
      } else {
         for (SettingsServerCommand.ServerSettingCommand<?> command : settings) {
            if (setting.equalsIgnoreCase(command.commandString)) {
               if (argStr.isEmpty() && command.statusMessage != null) {
                  String statusMessage = command.statusMessage.get(server, serverClient);
                  if (statusMessage != null) {
                     logs.add(statusMessage);
                     break;
                  }
               }

               try {
                  Object arg = command.handler
                     .parse(client, server, serverClient, argStr, new CmdParameter(command.commandString, new RestStringParameterHandler()));
                  command.apply(logs, server, serverClient, arg);
               } catch (IllegalArgumentException ex) {
                  logs.add(ex.getMessage());
               }
               break;
            }
         }
      }
   }

   private static void saveWorldSettings(Server server) {
      server.world.settings.saveSettings();
      server.world.settings.sendSettingsPacket();
   }

   private static void saveClientAndServerSettings() {
      Settings.saveClientSettings();
      Settings.saveServerSettings();
   }

   static {
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("DisableMobSpawns", new BoolParameterHandler(), (logs, server, client, arg) -> {
         if (!server.world.settings.cheatsAllowedOrHidden() && arg) {
            logs.add("Allow cheats to change that setting");
         } else {
            server.world.settings.disableMobSpawns = arg;
            saveWorldSettings(server);
            logs.add("Disable mob spawns set to " + server.world.settings.disableMobSpawns);
         }
      }, (server, client) -> "Disable mob spawns currently set to " + server.world.settings.disableMobSpawns));
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("ForcedPvP", new BoolParameterHandler(), (logs, server, client, arg) -> {
         server.world.settings.forcedPvP = arg;
         saveWorldSettings(server);
         logs.add("Forced PvP set to " + server.world.settings.forcedPvP);
      }, (server, client) -> "Forced PvP currently set to " + server.world.settings.forcedPvP));
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("SurvivalMode", new BoolParameterHandler(), (logs, server, client, arg) -> {
         server.world.settings.survivalMode = arg;
         saveWorldSettings(server);
         logs.add("Survival mode set to " + server.world.settings.survivalMode);
      }, (server, client) -> "Survival mode currently set to " + server.world.settings.survivalMode));
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("AllowOutsideCharacters", new BoolParameterHandler(), (logs, server, client, arg) -> {
         server.world.settings.allowOutsideCharacters = arg;
         saveWorldSettings(server);
         logs.add("Allow outside characters set to: " + server.world.settings.allowOutsideCharacters);
      }, (server, client) -> "Allow outside characters currently set to " + server.world.settings.allowOutsideCharacters));
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("PlayerHunger", new BoolParameterHandler(), (logs, server, client, arg) -> {
         server.world.settings.playerHunger = arg;
         saveWorldSettings(server);
         logs.add("Player hunger set to " + server.world.settings.playerHunger);
      }, (server, client) -> "Player hunger currently set to " + server.world.settings.playerHunger));
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("CanSettlersDie", new BoolParameterHandler(), (logs, server, client, arg) -> {
         server.world.settings.canSettlersDie = arg;
         saveWorldSettings(server);
         logs.add("Settler permanently die set to " + server.world.settings.canSettlersDie);
      }, (server, client) -> "Settler permanently die set to " + server.world.settings.canSettlersDie));
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("UnloadLevelsCooldown", new IntParameterHandler(), (logs, server, client, arg) -> {
         Settings.unloadLevelsCooldown = Math.max(2, arg);
         saveClientAndServerSettings();
         logs.add("Unload levels cooldown set to " + Settings.unloadLevelsCooldown + " seconds");
      }, (server, client) -> "Unload levels cooldown currently set to " + Settings.unloadLevelsCooldown + " seconds"));
      settings.add(
         new SettingsServerCommand.ServerSettingCommand<>(
            "DroppedItemsLifeMinutes",
            new IntParameterHandler(),
            (logs, server, client, arg) -> {
               Settings.droppedItemsLifeMinutes = arg;
               server.world.settings.droppedItemsLifeMinutes = Settings.droppedItemsLifeMinutes;
               saveWorldSettings(server);
               saveClientAndServerSettings();
               logs.add("Dropped items lifetime set to " + (Settings.droppedItemsLifeMinutes <= 0 ? "Infinite" : Settings.droppedItemsLifeMinutes) + " minutes");
            },
            (server, client) -> "Dropped items lifetime currently set to "
               + (server.world.settings.droppedItemsLifeMinutes <= 0 ? "Infinite" : server.world.settings.droppedItemsLifeMinutes)
               + " minutes"
         )
      );
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("UnloadSettlements", new BoolParameterHandler(), (logs, server, client, arg) -> {
         Settings.unloadSettlements = arg;
         server.world.settings.unloadSettlements = Settings.unloadSettlements;
         saveWorldSettings(server);
         saveClientAndServerSettings();
         logs.add("Unload settlements set to " + Settings.unloadSettlements);
      }, (server, client) -> "Unload settlements currently set to " + server.world.settings.unloadSettlements));
      settings.add(
         new SettingsServerCommand.ServerSettingCommand<>(
            "MaxSettlementsPerPlayer",
            new IntParameterHandler(),
            (logs, server, client, arg) -> {
               Settings.maxSettlementsPerPlayer = arg;
               server.world.settings.maxSettlementsPerPlayer = Settings.maxSettlementsPerPlayer;
               saveWorldSettings(server);
               saveClientAndServerSettings();
               logs.add("Max settlements per player set to " + (Settings.maxSettlementsPerPlayer < 0 ? "Unlimited" : Settings.maxSettlementsPerPlayer));
            },
            (server, client) -> "Max settlements per player currently set to "
               + (server.world.settings.maxSettlementsPerPlayer < 0 ? "Unlimited" : server.world.settings.maxSettlementsPerPlayer)
         )
      );
      settings.add(
         new SettingsServerCommand.ServerSettingCommand<>(
            "MaxSettlersPerSettlement",
            new IntParameterHandler(),
            (logs, server, client, arg) -> {
               Settings.maxSettlersPerSettlement = arg;
               server.world.settings.maxSettlersPerSettlement = Settings.maxSettlersPerSettlement;
               saveWorldSettings(server);
               saveClientAndServerSettings();
               logs.add("Max settlers per settlement set to " + (Settings.maxSettlersPerSettlement < 0 ? "Unlimited" : Settings.maxSettlersPerSettlement));
            },
            (server, client) -> "Max settlers per settlement currently set to "
               + (server.world.settings.maxSettlersPerSettlement < 0 ? "Unlimited" : server.world.settings.maxSettlersPerSettlement)
         )
      );
      settings.add(new SettingsServerCommand.ServerSettingCommand<>("DisableMobAI", new BoolParameterHandler(), (logs, server, client, arg) -> {
         if (!server.world.settings.cheatsAllowedOrHidden() && arg && !server.world.settings.creativeMode) {
            logs.add("Allow cheats or set to creative to change that setting");
         } else {
            server.world.settings.disableMobAI = arg;
            saveWorldSettings(server);
            logs.add("Disable mob ai set to " + server.world.settings.disableMobAI);
         }
      }, (server, client) -> "Disable mob ai set to " + server.world.settings.disableMobAI));
   }

   @FunctionalInterface
   public interface ServerSettingApply<T> {
      void apply(CommandLog var1, Server var2, ServerClient var3, T var4);
   }

   public static class ServerSettingCommand<T> {
      public final String commandString;
      public final ParameterHandler<T> handler;
      public final SettingsServerCommand.ServerSettingApply<T> applier;
      public final SettingsServerCommand.ServerSettingStatus statusMessage;

      public ServerSettingCommand(
         String commandString,
         ParameterHandler<T> handler,
         SettingsServerCommand.ServerSettingApply<T> applier,
         SettingsServerCommand.ServerSettingStatus statusMessage
      ) {
         this.commandString = commandString;
         this.handler = handler;
         this.applier = applier;
         this.statusMessage = statusMessage;
      }

      public void apply(CommandLog logs, Server server, ServerClient client, Object arg) {
         this.applier.apply(logs, server, client, (T)arg);
      }
   }

   public interface ServerSettingStatus {
      String get(Server var1, ServerClient var2);
   }
}
