package necesse.engine.platforms.steam.network.client;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.SteamMatchmakingCallback;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamMatchmaking.ChatEntryType;
import com.codedisaster.steamworks.SteamMatchmaking.ChatMemberStateChange;
import com.codedisaster.steamworks.SteamMatchmaking.ChatRoomEnterResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.platforms.steam.SteamData;
import necesse.engine.platforms.steam.network.packet.PacketAddSteamInvite;
import necesse.engine.platforms.steam.network.server.SteamServerSettings;

public class ClientSteamLobby {
   private final Client client;
   private final SteamMatchmaking matchmaking;
   private SteamServerSettings.SteamLobbyType lobbyType;
   private long waitingForLobbyCreate;
   private SteamID lobbySteamID;

   public ClientSteamLobby(final Client client) {
      this.client = client;
      this.matchmaking = new SteamMatchmaking(
         new SteamMatchmakingCallback() {
            @Override
            public void onFavoritesListChanged(int ip, int queryPort, int connPort, int appID, int flags, boolean add, int accountID) {
            }

            @Override
            public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID) {
               GameLog.debug.println("onLobbyInvite " + steamIDUser + ", " + steamIDLobby + ", " + gameID);
            }

            @Override
            public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response) {
               GameLog.debug.println("onLobbyEnter " + steamIDLobby + ", " + chatPermissions + ", " + blocked + ", " + response);
            }

            @Override
            public void onLobbyDataUpdate(SteamID steamIDLobby, SteamID steamIDMember, boolean success) {
               GameLog.debug.println("onLobbyDataUpdate " + steamIDLobby + ", " + steamIDMember + ", " + success);
            }

            @Override
            public void onLobbyChatUpdate(SteamID steamIDLobby, SteamID steamIDUserChanged, SteamID steamIDMakingChange, ChatMemberStateChange stateChange) {
               if (stateChange == ChatMemberStateChange.Entered) {
                  GameLog.debug.println("User " + steamIDUserChanged + " entered lobby " + steamIDLobby);
                  if (ClientSteamLobby.this.lobbyType == SteamServerSettings.SteamLobbyType.InviteOnly) {
                     client.network.sendPacket(new PacketAddSteamInvite(steamIDUserChanged));
                  }
               } else {
                  GameLog.debug.println("User " + steamIDUserChanged + " " + stateChange + " lobby " + steamIDLobby);
               }
            }

            @Override
            public void onLobbyChatMessage(SteamID steamIDLobby, SteamID steamIDUser, ChatEntryType entryType, int chatID) {
            }

            @Override
            public void onLobbyGameCreated(SteamID steamIDLobby, SteamID steamIDGameServer, int ip, short port) {
            }

            @Override
            public void onLobbyMatchList(int lobbiesMatching) {
            }

            @Override
            public void onLobbyKicked(SteamID steamIDLobby, SteamID steamIDAdmin, boolean kickedDueToDisconnect) {
               GameLog.debug.println("onLobbyKicked " + steamIDLobby + ", " + steamIDAdmin + ", " + kickedDueToDisconnect);
               if (ClientSteamLobby.this.lobbySteamID != null && ClientSteamLobby.this.lobbySteamID.equals(steamIDLobby)) {
                  ClientSteamLobby.this.lobbySteamID = null;
               }
            }

            @Override
            public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
               if (result == SteamResult.OK) {
                  ClientSteamLobby.this.lobbySteamID = steamIDLobby;
                  GameLog.debug.println("Created Steam lobby " + steamIDLobby);
                  if (client.getLocalServer() == null) {
                     client.network
                        .writeLobbyConnectInfo((key, value) -> ClientSteamLobby.this.matchmaking.setLobbyData(ClientSteamLobby.this.lobbySteamID, key, value));
                  } else {
                     GameLog.debug.println("Local game is hosting server");
                     SteamID steamID = SteamData.getSteamID();
                     if (steamID != null) {
                        ClientSteamLobby.this.matchmaking
                           .setLobbyData(ClientSteamLobby.this.lobbySteamID, "serverHostSteamID", String.valueOf(SteamID.getNativeHandle(steamID)));
                     }
                  }
               } else {
                  GameLog.warn.println("Failed to create Steam lobby: " + result);
                  ClientSteamLobby.this.waitingForLobbyCreate = System.currentTimeMillis() + 5000L;
               }
            }

            @Override
            public void onFavoritesListAccountsUpdated(SteamResult result) {
            }
         }
      );
   }

   public void createLobby(SteamServerSettings.SteamLobbyType type) {
      if (!this.isWaitingForLobbyCreate()) {
         this.lobbyType = type;
         if (this.matchmaking.createLobby(type.steamLobbyType, 10).isValid()) {
            this.waitingForLobbyCreate = System.currentTimeMillis() + 60000L;
         } else {
            this.waitingForLobbyCreate = System.currentTimeMillis() + 5000L;
         }
      }
   }

   public boolean isLobbyCreated() {
      return this.lobbySteamID != null;
   }

   public boolean isWaitingForLobbyCreate() {
      return System.currentTimeMillis() < this.waitingForLobbyCreate;
   }

   protected void leaveLobby() {
      if (this.isLobbyCreated()) {
         this.matchmaking.leaveLobby(this.lobbySteamID);
         GameLog.debug.println("Left lobby " + this.lobbySteamID);
      }

      this.lobbySteamID = null;
   }

   public boolean inviteUser(SteamID steamID) {
      return this.isLobbyCreated() ? this.matchmaking.inviteUserToLobby(this.lobbySteamID, steamID) : false;
   }

   public static void requestPublicIP(Consumer<String> onReceived) {
      Thread thread = new Thread(() -> {
         try {
            Scanner s = new Scanner(new URL("https://api.ipify.org").openStream(), GlobalData.charset.name()).useDelimiter("\\A");

            try {
               onReceived.accept(s.next());
            } catch (Throwable var5) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (s != null) {
               s.close();
            }
         } catch (IOException e) {
            onReceived.accept(null);
         }
      }, "Public IP Request");
      thread.start();
   }

   public void dispose() {
      this.leaveLobby();
      this.matchmaking.dispose();
   }
}
