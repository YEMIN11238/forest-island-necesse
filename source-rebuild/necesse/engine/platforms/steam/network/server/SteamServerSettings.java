package necesse.engine.platforms.steam.network.server;

import com.codedisaster.steamworks.SteamMatchmaking.LobbyType;
import java.io.File;
import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.network.server.ServerSettings;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class SteamServerSettings extends ServerSettings {
   public int port;
   public boolean allowConnectByIP = true;
   public SteamServerSettings.SteamLobbyType steamLobbyType = SteamServerSettings.SteamLobbyType.Open;

   public SteamServerSettings(
      ServerCreationSettings serverCreationSettings, int slots, boolean allowConnectionByIP, int port, SteamServerSettings.SteamLobbyType steamLobbyType
   ) {
      super(serverCreationSettings, slots);
      this.allowConnectByIP = allowConnectionByIP;
      this.port = port;
      this.steamLobbyType = steamLobbyType;
   }

   public static SteamServerSettings createHostServerSettings(
      ServerCreationSettings serverCreationSettings, int slots, boolean allowConnectionByIP, int port, SteamServerSettings.SteamLobbyType steamLobbyType
   ) {
      if (serverCreationSettings == null || serverCreationSettings.worldFilePath == null || serverCreationSettings.worldFilePath.getName().isEmpty()) {
         throw new IllegalArgumentException("Invalid world name");
      }

      if (slots >= 0 && slots <= 250) {
         if (!allowConnectionByIP || port >= 0 && port <= 65535) {
            return new SteamServerSettings(serverCreationSettings, slots, allowConnectionByIP, port, steamLobbyType);
         } else {
            throw new IllegalArgumentException("Invalid port");
         }
      } else {
         throw new IllegalArgumentException("Invalid slots");
      }
   }

   public static SteamServerSettings SingleplayerServer(ServerCreationSettings serverCreationSettings) {
      if (serverCreationSettings != null && serverCreationSettings.worldFilePath != null && !serverCreationSettings.worldFilePath.getName().isEmpty()) {
         return new SteamServerSettings(serverCreationSettings, 1, true, -1, null);
      } else {
         throw new IllegalArgumentException("Invalid world name");
      }
   }

   public static SteamServerSettings createFromSave(File worldFilePath, LoadData save) {
      int slots = save.getInt("slots", Settings.serverSlots);
      int port = save.getInt("port", Settings.serverPort);
      boolean allowConnectByIP = save.getBoolean("allowConnectByIP", true);
      SteamServerSettings.SteamLobbyType steamLobbyType = save.getEnum(
         SteamServerSettings.SteamLobbyType.class, "steamLobbyType", SteamServerSettings.SteamLobbyType.Open
      );
      SteamServerSettings settings = new SteamServerSettings(new ServerCreationSettings(worldFilePath), slots, allowConnectByIP, port, steamLobbyType);
      settings.password = save.getSafeString("password", settings.password);
      return settings;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("port", this.port);
      save.addBoolean("allowConnectByIP", this.allowConnectByIP);
      save.addEnum("steamLobbyType", this.steamLobbyType);
   }

   @Override
   public boolean isSinglePlayer() {
      return this.port == -1;
   }

   public enum SteamLobbyType {
      InviteOnly(LobbyType.Private, new LocalMessage("ui", "steamlobbyinvite")),
      Open(LobbyType.FriendsOnly, new LocalMessage("ui", "steamlobbyopen"));

      public final LobbyType steamLobbyType;
      public final GameMessage displayName;

      SteamLobbyType(LobbyType steamLobbyType, GameMessage displayName) {
         this.steamLobbyType = steamLobbyType;
         this.displayName = displayName;
      }
   }
}
