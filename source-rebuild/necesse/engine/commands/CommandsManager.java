package necesse.engine.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.clientCommands.BoolClientCommand;
import necesse.engine.commands.clientCommands.DebugClientClientCommand;
import necesse.engine.commands.clientCommands.DebugWorldPresetsClientClientCommand;
import necesse.engine.commands.clientCommands.DeleteMapDataClientClientCommand;
import necesse.engine.commands.clientCommands.HelpClientCommand;
import necesse.engine.commands.clientCommands.MaxFPSClientCommand;
import necesse.engine.commands.clientCommands.ObjectHitboxesClientCommand;
import necesse.engine.commands.clientCommands.PanningCameraClientCommand;
import necesse.engine.commands.clientCommands.PlayCreditsClientCommand;
import necesse.engine.commands.clientCommands.ReloadClientCommand;
import necesse.engine.commands.clientCommands.StopCreditsClientCommand;
import necesse.engine.commands.clientCommands.ToggleHeatDistortionClientCommand;
import necesse.engine.commands.clientCommands.VoidClientCommand;
import necesse.engine.commands.clientCommands.WalkDirectionClientCommand;
import necesse.engine.commands.clientCommands.ZoomClientCommand;
import necesse.engine.commands.parameterHandlers.ParameterHandler;
import necesse.engine.commands.serverCommands.AllowCheatsServerCommand;
import necesse.engine.commands.serverCommands.ArmorSetServerCommand;
import necesse.engine.commands.serverCommands.BanServerCommand;
import necesse.engine.commands.serverCommands.BansServerCommand;
import necesse.engine.commands.serverCommands.BuffServerCommand;
import necesse.engine.commands.serverCommands.CalculateRecipeValueClientCommand;
import necesse.engine.commands.serverCommands.ChangeNameServerCommand;
import necesse.engine.commands.serverCommands.ClearAreaServerCommand;
import necesse.engine.commands.serverCommands.ClearBuffServerCommand;
import necesse.engine.commands.serverCommands.ClearConnectionServerCommand;
import necesse.engine.commands.serverCommands.ClearDropsServerCommand;
import necesse.engine.commands.serverCommands.ClearEventsServerCommand;
import necesse.engine.commands.serverCommands.ClearMobsServerCommand;
import necesse.engine.commands.serverCommands.ClearTeamServerCommand;
import necesse.engine.commands.serverCommands.ClearallServerCommand;
import necesse.engine.commands.serverCommands.CompleteIncursionServerCommand;
import necesse.engine.commands.serverCommands.CopyInventoryServerCommand;
import necesse.engine.commands.serverCommands.CopyItemCommand;
import necesse.engine.commands.serverCommands.CreateTeamServerCommand;
import necesse.engine.commands.serverCommands.CreativeModeServerCommand;
import necesse.engine.commands.serverCommands.DeathPenaltyServerCommand;
import necesse.engine.commands.serverCommands.DebugLoadingPerformanceCommand;
import necesse.engine.commands.serverCommands.DeleteLevelServerCommand;
import necesse.engine.commands.serverCommands.DeletePlayerServerCommand;
import necesse.engine.commands.serverCommands.DieServerCommand;
import necesse.engine.commands.serverCommands.DifficultyServerCommand;
import necesse.engine.commands.serverCommands.EnchantServerCommand;
import necesse.engine.commands.serverCommands.EndRaidCommand;
import necesse.engine.commands.serverCommands.GetTeamServerCommand;
import necesse.engine.commands.serverCommands.GiveServerCommand;
import necesse.engine.commands.serverCommands.HealServerCommand;
import necesse.engine.commands.serverCommands.HealthServerCommand;
import necesse.engine.commands.serverCommands.HelpServerCommand;
import necesse.engine.commands.serverCommands.HungerServerCommand;
import necesse.engine.commands.serverCommands.InvincibilityServerCommand;
import necesse.engine.commands.serverCommands.InviteTeamServerCommand;
import necesse.engine.commands.serverCommands.ItemGNDCommand;
import necesse.engine.commands.serverCommands.KickServerCommand;
import necesse.engine.commands.serverCommands.LeaveTeamServerCommand;
import necesse.engine.commands.serverCommands.LevelsServerCommand;
import necesse.engine.commands.serverCommands.MOTDServerCommand;
import necesse.engine.commands.serverCommands.ManaServerCommand;
import necesse.engine.commands.serverCommands.MaxHealthServerCommand;
import necesse.engine.commands.serverCommands.MaxLatencyServerCommand;
import necesse.engine.commands.serverCommands.MaxManaServerCommand;
import necesse.engine.commands.serverCommands.MeServerCommand;
import necesse.engine.commands.serverCommands.MowServerCommand;
import necesse.engine.commands.serverCommands.MyPermissionsServerCommand;
import necesse.engine.commands.serverCommands.NetworkServerCommand;
import necesse.engine.commands.serverCommands.OpenHelpFormClientCommand;
import necesse.engine.commands.serverCommands.PasswordServerCommand;
import necesse.engine.commands.serverCommands.PauseWhenEmptyServerCommand;
import necesse.engine.commands.serverCommands.PerformanceServerCommand;
import necesse.engine.commands.serverCommands.PermissionsServerCommand;
import necesse.engine.commands.serverCommands.PlayerNamesServerCommand;
import necesse.engine.commands.serverCommands.PlayersServerCommand;
import necesse.engine.commands.serverCommands.PlaytimeServerCommand;
import necesse.engine.commands.serverCommands.PrintServerCommand;
import necesse.engine.commands.serverCommands.RaidsServerCommand;
import necesse.engine.commands.serverCommands.RainServerCommand;
import necesse.engine.commands.serverCommands.RegenServerCommand;
import necesse.engine.commands.serverCommands.ResetClientRomanceCommand;
import necesse.engine.commands.serverCommands.ResetHealthUpgradesServerCommand;
import necesse.engine.commands.serverCommands.ResilienceServerCommand;
import necesse.engine.commands.serverCommands.SaveServerCommand;
import necesse.engine.commands.serverCommands.SayServerCommand;
import necesse.engine.commands.serverCommands.SetDimensionServerCommand;
import necesse.engine.commands.serverCommands.SetIslandServerCommand;
import necesse.engine.commands.serverCommands.SetLanguageServerCommand;
import necesse.engine.commands.serverCommands.SetLevelServerCommand;
import necesse.engine.commands.serverCommands.SetPositionServerCommand;
import necesse.engine.commands.serverCommands.SetRaidDifficultyCommand;
import necesse.engine.commands.serverCommands.SetRomanceLevelForClosestSettlerCommand;
import necesse.engine.commands.serverCommands.SetTeamOwnerServerCommand;
import necesse.engine.commands.serverCommands.SetTeamServerCommand;
import necesse.engine.commands.serverCommands.SettingsServerCommand;
import necesse.engine.commands.serverCommands.SettlementsServerCommand;
import necesse.engine.commands.serverCommands.SpawnSettlerServerCommand;
import necesse.engine.commands.serverCommands.SpawnVisitorServerCommand;
import necesse.engine.commands.serverCommands.StartRaidAttackCommand;
import necesse.engine.commands.serverCommands.StartRaidCommand;
import necesse.engine.commands.serverCommands.StaticDamageServerCommand;
import necesse.engine.commands.serverCommands.StopHelpServerCommand;
import necesse.engine.commands.serverCommands.StopServerCommand;
import necesse.engine.commands.serverCommands.TeleportServerCommand;
import necesse.engine.commands.serverCommands.TimeServerCommand;
import necesse.engine.commands.serverCommands.UnbanServerCommand;
import necesse.engine.commands.serverCommands.UpgradeAllItemsServerCommand;
import necesse.engine.commands.serverCommands.UpgradeItemServerCommand;
import necesse.engine.commands.serverCommands.WhisperHelpServerCommand;
import necesse.engine.commands.serverCommands.WhisperServerCommand;
import necesse.engine.commands.serverCommands.WorldSeedServerCommand;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketCmdAutocomplete;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.platforms.PlatformManager;
import necesse.gfx.GameColor;

public class CommandsManager {
   private static boolean registryOpen = true;
   private static boolean registeredCore = false;
   private static final List<ChatCommand> serverCommands = new ArrayList<>();
   private static final List<ChatCommand> clientCommands = new ArrayList<>();
   public final Server server;
   public final Client client;
   private String lastCheatCommand;

   public static void registerCoreCommands() {
      if (!registryOpen) {
         throw new IllegalStateException("Command registration is closed");
      }

      if (registeredCore) {
         throw new IllegalStateException("Cannot register core commands twice");
      }

      registeredCore = true;
      registerServerCommand(new StopHelpServerCommand());
      registerServerCommand(new StopServerCommand("quit"));
      registerServerCommand(new StopServerCommand("stop"));
      registerServerCommand(new StopServerCommand("exit"));
      registerServerCommand(new HelpServerCommand());
      registerServerCommand(new PlaytimeServerCommand());
      registerServerCommand(new MeServerCommand());
      registerServerCommand(new WhisperHelpServerCommand());
      registerServerCommand(new WhisperServerCommand("whisper"));
      registerServerCommand(new WhisperServerCommand("w"));
      registerServerCommand(new WhisperServerCommand("pm"));
      registerServerCommand(new NetworkServerCommand());
      registerServerCommand(new PlayersServerCommand());
      registerServerCommand(new PlayerNamesServerCommand());
      registerServerCommand(new LevelsServerCommand());
      registerServerCommand(new SettlementsServerCommand());
      registerServerCommand(new SaveServerCommand());
      registerServerCommand(new MowServerCommand());
      registerServerCommand(new TimeServerCommand());
      registerServerCommand(new ClearallServerCommand());
      registerServerCommand(new ClearMobsServerCommand());
      registerServerCommand(new ClearDropsServerCommand());
      registerServerCommand(new ClearEventsServerCommand());
      registerServerCommand(new TeleportServerCommand("tp"));
      registerServerCommand(new KickServerCommand());
      registerServerCommand(new SayServerCommand());
      registerServerCommand(new PrintServerCommand());
      registerServerCommand(new GiveServerCommand());
      registerServerCommand(new BuffServerCommand());
      registerServerCommand(new ClearBuffServerCommand());
      registerServerCommand(new SetIslandServerCommand());
      registerServerCommand(new SetDimensionServerCommand());
      registerServerCommand(new SetPositionServerCommand());
      registerServerCommand(new SetLevelServerCommand());
      registerServerCommand(new HealthServerCommand("hp"));
      registerServerCommand(new ResilienceServerCommand("resilience"));
      registerServerCommand(new MaxHealthServerCommand());
      registerServerCommand(new ManaServerCommand());
      registerServerCommand(new MaxManaServerCommand());
      registerServerCommand(new HungerServerCommand());
      registerServerCommand(new DeletePlayerServerCommand());
      registerServerCommand(new SettingsServerCommand());
      registerServerCommand(new DifficultyServerCommand());
      registerServerCommand(new DeathPenaltyServerCommand());
      registerServerCommand(new RaidsServerCommand());
      registerServerCommand(new PauseWhenEmptyServerCommand());
      registerServerCommand(new MaxLatencyServerCommand());
      registerServerCommand(new PasswordServerCommand());
      registerServerCommand(new PermissionsServerCommand());
      registerServerCommand(new MyPermissionsServerCommand());
      registerServerCommand(new BanServerCommand());
      registerServerCommand(new UnbanServerCommand());
      registerServerCommand(new BansServerCommand());
      registerServerCommand(new RegenServerCommand());
      registerServerCommand(new DeleteLevelServerCommand());
      registerServerCommand(new RainServerCommand());
      registerServerCommand(new EnchantServerCommand());
      registerServerCommand(new AllowCheatsServerCommand());
      registerServerCommand(new SetLanguageServerCommand());
      registerServerCommand(new DieServerCommand());
      registerServerCommand(new ItemGNDCommand());
      registerServerCommand(new CopyItemCommand());
      registerServerCommand(new HealServerCommand());
      registerServerCommand(new CopyInventoryServerCommand());
      registerServerCommand(new PerformanceServerCommand());
      registerServerCommand(new GetTeamServerCommand());
      registerServerCommand(new ClearTeamServerCommand());
      registerServerCommand(new SetTeamServerCommand());
      registerServerCommand(new SetTeamOwnerServerCommand());
      registerServerCommand(new CreateTeamServerCommand());
      registerServerCommand(new LeaveTeamServerCommand());
      registerServerCommand(new InviteTeamServerCommand());
      registerServerCommand(new MOTDServerCommand());
      registerServerCommand(new ChangeNameServerCommand());
      registerServerCommand(new StartRaidCommand());
      registerServerCommand(new StartRaidAttackCommand());
      registerServerCommand(new EndRaidCommand());
      registerServerCommand(new SetRaidDifficultyCommand());
      registerServerCommand(new InvincibilityServerCommand());
      registerServerCommand(new SpawnSettlerServerCommand());
      registerServerCommand(new SpawnVisitorServerCommand());
      registerServerCommand(new UpgradeItemServerCommand());
      registerServerCommand(new UpgradeAllItemsServerCommand());
      registerServerCommand(new CreativeModeServerCommand());
      registerServerCommand(new WorldSeedServerCommand());
      registerServerCommand(new ResetClientRomanceCommand());
      registerServerCommand(new SetRomanceLevelForClosestSettlerCommand());
      registerServerCommand(new ArmorSetServerCommand());
      registerClientCommand(new HelpClientCommand());
      registerClientCommand(new ReloadClientCommand());
      registerClientCommand(new PanningCameraClientCommand());
      registerClientCommand(new ZoomClientCommand());
      registerClientCommand(new ObjectHitboxesClientCommand());
      registerClientCommand(new ToggleHeatDistortionClientCommand());
      registerClientCommand(new VoidClientCommand("reloadlang", "Reloads language files", PermissionLevel.USER, (c, l) -> {
         Localization.reloadLanguageFiles();
         c.chat.addMessage("Reloaded language files");
      }));
      registerClientCommand(new PlayCreditsClientCommand());
      registerClientCommand(new StopCreditsClientCommand());
      if (GlobalData.isDevMode()) {
         registerClientCommand(BoolClientCommand.create("alwaysrain", "Toggles show rain", PermissionLevel.OWNER, Settings.class, "alwaysRain", null));
         registerClientCommand(BoolClientCommand.create("alwayslight", "Toggles always light", PermissionLevel.OWNER, Settings.class, "alwaysLight", null));
         registerClientCommand(new MaxFPSClientCommand());
         registerServerCommand(new ClearConnectionServerCommand());
         registerServerCommand(new StaticDamageServerCommand());
         registerServerCommand(new DebugLoadingPerformanceCommand("serverloadingperformance"));
         registerClientCommand(new DebugLoadingPerformanceCommand("clientloadingperformance"));
         registerClientCommand(new DeleteMapDataClientClientCommand());
         registerServerCommand(new DebugWorldPresetsClientClientCommand());
         registerServerCommand(new ClearAreaServerCommand());
         registerServerCommand(new ResetHealthUpgradesServerCommand());
         registerServerCommand(new CompleteIncursionServerCommand());
         registerClientCommand(new CalculateRecipeValueClientCommand());
         registerClientCommand(new WalkDirectionClientCommand());
         registerClientCommand(new OpenHelpFormClientCommand());
      }

      registerClientCommand(
         BoolClientCommand.create("hidechat", "Toggles showing chat when not typing", PermissionLevel.USER, Settings.class, "hideChat", null)
      );
      registerClientCommand(new DebugClientClientCommand());
      PlatformManager.getPlatform().registerPlatformCommands();
   }

   public static <T extends ChatCommand> T registerServerCommand(T command) {
      if (!registryOpen) {
         throw new IllegalStateException("Command registration is closed");
      }

      serverCommands.add(command);
      return command;
   }

   public static <T extends ChatCommand> T registerClientCommand(T command) {
      if (!registryOpen) {
         throw new IllegalStateException("Command registration is closed");
      }

      clientCommands.add(command);
      return command;
   }

   public static void closeRegistry() {
      registryOpen = false;
   }

   public CommandsManager(Server server) {
      this.server = server;
      this.client = null;
      this.lastCheatCommand = null;
   }

   public CommandsManager(Client client) {
      this.client = client;
      this.server = null;
   }

   public List<ChatCommand> getServerCommands() {
      return serverCommands;
   }

   public List<ChatCommand> getClientCommands() {
      return clientCommands;
   }

   public List<ChatCommand> getCommands() {
      List<ChatCommand> out = new ArrayList<>(this.getServerCommands());
      if (this.client != null) {
         out.addAll(this.getClientCommands());
      }

      return out;
   }

   public boolean runServerCommand(ParsedCommand command, ServerClient serverClient) {
      ArrayList<String> args = new ArrayList<>(Arrays.asList(command.args));
      CommandLog firstLogs = null;

      for (ChatCommand cmd : serverCommands) {
         if (!cmd.onlyForHelp() && cmd.name.equalsIgnoreCase(command.commandName)) {
            CommandLog logs = new CommandLog(this.client, serverClient);
            if (firstLogs == null) {
               firstLogs = logs;
            }

            if (cmd.havePermissions(this.client, this.server, serverClient)) {
               String error = null;
               if (cmd.isCheat() && !this.server.world.settings.cheatsAllowedOrHidden()) {
                  if (getPermissionLevel(this.client, serverClient).getLevel() < PermissionLevel.OWNER.getLevel()) {
                     error = GameColor.RED.getColorCode() + "Cheats are not allowed in this world.";
                  } else {
                     error = GameColor.CYAN.getColorCode() + "Running this command will disable achievements. Run it again to accept this.";
                     String cmdJoin = command.commandName + " " + String.join(" ", args);
                     if (cmdJoin.equals(this.lastCheatCommand)) {
                        error = null;
                        this.lastCheatCommand = null;
                        this.server.world.settings.enableCheats();
                     } else {
                        this.lastCheatCommand = cmdJoin;
                     }
                  }
               }

               if (error != null) {
                  logs.add(error);
               } else {
                  try {
                     if (cmd.run(this.client, this.server, serverClient, args, logs)) {
                        logs.printLog();
                        return true;
                     }
                  } catch (Exception e) {
                     logs.add(GameColor.RED.getColorCode() + "Error executing command " + command.commandName);
                     e.printStackTrace();
                  }
               }
            } else {
               logs.add(GameColor.RED.getColorCode() + "You do not have permissions for that command.");
            }
         }
      }

      if (firstLogs != null) {
         firstLogs.printLog();
         return true;
      } else {
         this.lastCheatCommand = null;
         CommandLog.print(null, serverClient, "Could not find command: \"" + command.commandName + "\", use command \"help\".");
         return false;
      }
   }

   public boolean runClientCommand(ParsedCommand command) {
      if (this.client == null) {
         throw new IllegalStateException("Cannot run client commands on server");
      }

      ArrayList<String> args = new ArrayList<>(Arrays.asList(command.args));
      CommandLog firstLogs = null;

      for (ChatCommand cmd : clientCommands) {
         if (!cmd.onlyForHelp() && cmd.name.equalsIgnoreCase(command.commandName)) {
            CommandLog logs = new CommandLog(this.client, null);
            if (firstLogs == null) {
               firstLogs = logs;
            }

            if (cmd.havePermissions(this.client, this.server, null)) {
               try {
                  if (cmd.run(this.client, this.server, null, args, logs)) {
                     logs.printLog();
                     return true;
                  }
               } catch (Exception e) {
                  logs.add(GameColor.RED.getColorCode() + "Error executing command " + command.commandName);
                  e.printStackTrace();
               }
            } else {
               logs.add(GameColor.RED.getColorCode() + "You do not have permissions for that command.");
            }
         }
      }

      if (firstLogs != null) {
         firstLogs.printLog();
         return true;
      } else {
         return false;
      }
   }

   public List<AutoComplete> autocomplete(ParsedCommand command, ServerClient serverClient) {
      if (command.args.length == 0) {
         return ParameterHandler.autocompleteFromList(
            this.getCommands(),
            cmdx -> !cmdx.onlyForHelp() && cmdx.havePermissions(this.client, this.server, serverClient),
            cmdx -> cmdx.name,
            new CmdArgument(null, command.commandName, 1)
         );
      }

      for (ChatCommand cmd : clientCommands) {
         if (!cmd.onlyForHelp() && cmd.havePermissions(this.client, this.server, serverClient) && cmd.name.equalsIgnoreCase(command.commandName)) {
            List<AutoComplete> out = this.clearDuplicates(cmd.autocomplete(this.client, this.server, serverClient, command.args));
            if (!out.isEmpty()) {
               return out;
            }
         }
      }

      for (ChatCommand cmd : serverCommands) {
         if (!cmd.onlyForHelp() && cmd.havePermissions(this.client, this.server, serverClient) && cmd.name.equalsIgnoreCase(command.commandName)) {
            if (this.client != null && cmd.autocompleteOnServer()) {
               this.client.network.sendPacket(new PacketCmdAutocomplete(command.fullCommand));
               return Collections.emptyList();
            }

            List<AutoComplete> out = this.clearDuplicates(cmd.autocomplete(this.client, this.server, serverClient, command.args));
            if (!out.isEmpty()) {
               return out;
            }
         }
      }

      return Collections.emptyList();
   }

   protected List<AutoComplete> clearDuplicates(List<AutoComplete> out) {
      ListIterator<AutoComplete> li = out.listIterator();

      while (li.hasNext()) {
         AutoComplete next = li.next();
         boolean remove = false;

         for (AutoComplete other : out) {
            if (next != other && next.equals(other)) {
               remove = true;
               break;
            }
         }

         if (remove) {
            li.remove();
         }
      }

      return out;
   }

   public String getCurrentUsage(ParsedCommand command, ServerClient serverClient) {
      if (command.args.length == 0) {
         return null;
      }

      for (ChatCommand cmd : clientCommands) {
         if (!cmd.onlyForHelp() && cmd.havePermissions(this.client, this.server, serverClient) && cmd.name.equalsIgnoreCase(command.commandName)) {
            return cmd.getCurrentUsage(this.client, this.server, serverClient, command.args);
         }
      }

      for (ChatCommand cmd : serverCommands) {
         if (!cmd.onlyForHelp() && cmd.havePermissions(this.client, this.server, serverClient) && cmd.name.equalsIgnoreCase(command.commandName)) {
            return cmd.getCurrentUsage(this.client, this.server, serverClient, command.args);
         }
      }

      return null;
   }

   public static PermissionLevel getPermissionLevel(Client client, ServerClient serverClient) {
      if (client != null) {
         return client.getPermissionLevel();
      } else {
         return serverClient != null ? serverClient.getPermissionLevel() : PermissionLevel.SERVER;
      }
   }
}
