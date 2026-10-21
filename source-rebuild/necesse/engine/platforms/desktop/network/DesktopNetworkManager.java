package necesse.engine.platforms.desktop.network;

import java.io.File;
import java.io.IOException;
import necesse.engine.GameCache;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.HostSettingsForm;
import necesse.engine.network.NetworkManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.PlatformSubForm;
import necesse.engine.network.NetworkManager.PlatformConnectApprovedData;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.PortedServerSettings;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.network.server.ServerHostSettings;
import necesse.engine.network.server.ServerSettings;
import necesse.engine.network.server.network.ServerNetwork;
import necesse.engine.platforms.desktop.forms.FormDesktopHostSettings;
import necesse.engine.platforms.desktop.network.server.network.DesktopServerOpenNetwork;
import necesse.engine.platforms.sharedOnPC.forms.FormJoinServerForm;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainMenu;
import necesse.engine.state.MainMenu.ContinueMode;
import necesse.engine.util.ObjectValue;
import necesse.engine.world.FileSystemClosedException;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.lists.FormGeneralList;

public class DesktopNetworkManager extends NetworkManager {
   @Override
   public boolean allowsHosting() {
      return true;
   }

   @Override
   public boolean allowsFriendJoining() {
      return false;
   }

   @Override
   public boolean allowsServerJoining() {
      return true;
   }

   @Override
   public ServerSettings getDefaultHostSettings(ServerCreationSettings serverCreationSettings) {
      return new PortedServerSettings(serverCreationSettings, Settings.serverSlots, Settings.serverPort);
   }

   @Override
   public HostSettingsForm getHostSettingsForm(GameMessage backButtonName, Runnable backButtonPressed, Runnable onHostStarted) {
      return new FormDesktopHostSettings(backButtonName, backButtonPressed, onHostStarted);
   }

   @Override
   public PlatformSubForm getJoinFriendForm(MainMenu mainMenu, Runnable onBackButton) {
      return null;
   }

   @Override
   public FormGeneralList getInviteForm(Client client, int x, int y, int width, int height) {
      return null;
   }

   @Override
   public PlatformSubForm getJoinServerForm(FormSwitcher parent, MainMenu mainMenu, final Runnable onBackButton) {
      return new FormJoinServerForm(400, 460, mainMenu) {
         @Override
         public void onBackPressed() {
            onBackButton.run();
         }
      };
   }

   @Override
   public Client startHostClient(ServerSettings settings, ServerHostSettings hostSettings) throws IOException, FileSystemClosedException {
      Server server = new Server(settings);
      server.pauseForSpawnedPlayer = true;
      Client client = new Client(GlobalData.getCurrentGameLoop(), server, false);
      server.makeHosted(client);
      server.start(hostSettings, true);
      client.start();
      String passwordString = settings.password != null && !settings.password.isEmpty() ? " with password \"" + settings.password + "\"" : "";
      System.out
         .println(
            "Started hosting using "
               + server.network.getDebugString()
               + " with "
               + server.getSlots()
               + " slots on world \""
               + server.world.filePath.getName()
               + "\""
               + passwordString
               + ", game version "
               + "1.3.3"
               + "."
         );
      System.out.println("Found " + server.getSavedClients().size() + " saved players.");
      String address = server.network.getAddress();
      if (address != null) {
         System.out.println("Local address: " + address);
      }

      return client;
   }

   @Override
   public Client startSingleplayerClient(ServerCreationSettings serverCreationSettings) throws IOException, FileSystemClosedException {
      ServerSettings settings = PortedServerSettings.SingleplayerServer(serverCreationSettings);
      Server server = new Server(settings);
      server.pauseForSpawnedPlayer = true;
      Client client = new Client(GlobalData.getCurrentGameLoop(), server, true);
      server.makeSingleplayer(client);
      server.start(null, true);
      client.start();
      System.out.println("Started singleplayer server on world " + server.world.filePath.getName() + ", game version " + "1.3.3");
      System.out.println("Found " + server.getSavedClients().size() + " saved players.");
      return client;
   }

   @Override
   public Client startJoinServerClient(String name, String address, int port) {
      SaveData continueSave = MainMenu.getContinueCacheSaveBase(ContinueMode.JOIN);
      continueSave.addSafeString("name", name);
      continueSave.addSafeString("address", address);
      continueSave.addInt("port", port);
      GameCache.cacheSave(continueSave, "continueLast");
      GameMessage playingOnDisplayName = new StaticMessage(name);
      Client client = new Client(GlobalData.getCurrentGameLoop(), address, port, playingOnDisplayName);
      client.start();
      System.out.println("Started client connecting to " + client.network.getDebugString() + ", game version " + "1.3.3");
      return client;
   }

   @Override
   public ServerSettings getServerSettingsFromSave(File worldFilePath, LoadData data) {
      return PortedServerSettings.createFromSave(worldFilePath, data);
   }

   @Override
   public ObjectValue<GameMessage, Runnable> getMainMenuContinueButtonForJoining(LoadData continueSave, MainMenu mainMenu) {
      String address = continueSave.getSafeString("address", null, false);
      if (address != null) {
         int port = continueSave.getInt("port", -1);
         if (port != -1) {
            String name = continueSave.getSafeString("name", null, false);
            if (name == null) {
               if (port == 14159) {
                  name = address;
               } else {
                  name = address + ":" + port;
               }
            }

            String finalName = name;
            return new ObjectValue<>(new LocalMessage("ui", "continuejoin", "name", name), () -> {
               Client client = this.startJoinServerClient(finalName, address, port);
               mainMenu.startConnection(client, null);
            });
         }
      }

      return null;
   }

   @Override
   public PlatformConnectApprovedData createPlatformConnectApprovedData() {
      return new DesktopNetworkManager.DesktopConnectApprovedData();
   }

   @Override
   public ServerNetwork createOpenServerNetwork(Server server, ServerSettings serverSettings) {
      return new DesktopServerOpenNetwork(server, serverSettings);
   }

   @Override
   public void startupInstantConnect(String connectTo, MainMenu mainMenu) {
      String[] split = connectTo.split(":");
      String address = connectTo;
      int port = 14159;
      if (split.length > 1) {
         address = split[0];

         try {
            port = Integer.parseInt(split[1]);
            if (port < 0 || port > 65535) {
               throw new Exception("Port out of range");
            }
         } catch (Exception ex) {
            System.err.println("Invalid instant connect port, using default");
            port = 14159;
         }
      }

      Client client = this.startJoinServerClient(address, address, port);
      mainMenu.startConnection(client, null);
   }

   @Override
   public Server startServer(ServerSettings settings, ServerHostSettings hostSettings) throws IOException, FileSystemClosedException {
      Server server = new Server(settings);
      server.start(hostSettings, false);
      String passwordString = settings.password != null && !settings.password.isEmpty() ? " with password \"" + settings.password + "\"" : "";
      System.out
         .println(
            "Started server using "
               + server.network.getDebugString()
               + " with "
               + server.getSlots()
               + " slots on world \""
               + server.world.filePath.getName()
               + "\""
               + passwordString
               + ", game version "
               + "1.3.3"
               + "."
         );
      System.out.println("Found " + server.getSavedClients().size() + " saved players.");
      if (Settings.strictServerAuthority) {
         GameLog.warn
            .println(
               "Strict server authority enabled. Are you sure you want this? This will result in a much worse experience for connected players and should only be used if absolutely necessary."
            );
      }

      String address = server.network.getAddress();
      if (address != null) {
         System.out.println("Local address: " + address);
      }

      System.out.println("Type help for list of commands.");
      return server;
   }

   public static class DesktopConnectApprovedData extends PlatformConnectApprovedData {
      @Override
      public void writePlatformData(PacketWriter writer, Server server, ServerClient client) {
      }

      @Override
      public void readPlatformData(PacketReader reader) {
      }
   }
}
