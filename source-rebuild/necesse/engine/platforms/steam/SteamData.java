package necesse.engine.platforms.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamApps;
import com.codedisaster.steamworks.SteamAppsCallback;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.SteamMatchmakingCallback;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserCallback;
import com.codedisaster.steamworks.SteamUtils;
import com.codedisaster.steamworks.SteamUtilsCallback;
import com.codedisaster.steamworks.SteamFriends.FriendFlags;
import com.codedisaster.steamworks.SteamFriends.FriendRelationship;
import com.codedisaster.steamworks.SteamFriends.OverlayToWebPageMode;
import com.codedisaster.steamworks.SteamFriends.PersonaState;
import com.codedisaster.steamworks.SteamMatchmaking.ChatRoomEnterResponse;
import com.codedisaster.steamworks.SteamUtils.FloatingGamepadTextInputMode;
import com.codedisaster.steamworks.SteamUtils.GamepadTextInputMode;
import com.codedisaster.steamworks.SteamUtils.GamepadTextLineMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.steam.network.SteamNetworkManager;
import necesse.engine.state.MainMenu;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.presets.ConfirmationContinueForm;

public class SteamData {
   private static boolean initialized = false;
   private static boolean created = false;
   private static SteamApps apps;
   private static SteamUser user;
   private static SteamFriends friends;
   private static SteamMatchmaking matchmaking;
   private static SteamUtils utils;
   private static boolean overlayActive = false;
   private static SteamID connectLobby = null;
   private static boolean joinedLobby;
   private static boolean inLobby;
   private static long connectLobbyTimeout;
   private static long lastCallbackError;

   public static void init() {
      if (!initialized) {
         if (SteamAPI.isSteamRunning()) {
            apps = new SteamApps(new SteamAppsCallback() {});
            user = new SteamUser(new SteamUserCallback() {});
            friends = new SteamFriends(new SteamFriendsCallback() {
               @Override
               public void onGameOverlayActivated(boolean active, boolean userInitiated, int appID) {
                  SteamData.overlayActive = active;
               }

               @Override
               public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
                  GameLog.debug.println("Got join lobby request from " + SteamData.friends.getFriendPersonaName(steamIDFriend) + ": " + steamIDLobby);
                  SteamData.connectLobby = steamIDLobby;
                  SteamData.joinedLobby = false;
                  SteamData.inLobby = false;
               }

               @Override
               public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
                  GameLog.debug.println("Got join request from " + SteamData.friends.getFriendPersonaName(steamIDFriend) + ": " + connect);
               }
            });
            matchmaking = new SteamMatchmaking(
               new SteamMatchmakingCallback() {
                  @Override
                  public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID) {
                     if (gameID == 1169040L && GlobalData.getCurrentState() instanceof MainMenu) {
                        MainMenu mainMenu = (MainMenu)GlobalData.getCurrentState();
                        ConfirmationContinueForm form = new ConfirmationContinueForm("inviterecevied");
                        String name = SteamData.tryToGetSteamName(steamIDUser).orElse(steamIDUser.toString());
                        form.setupConfirmation(
                           new LocalMessage("ui", "gotinvite", "name", name),
                           new LocalMessage("ui", "acceptbutton"),
                           new LocalMessage("ui", "declinebutton"),
                           () -> {
                              System.out.println("Accepted invite from " + name);
                              SteamData.connectLobby(steamIDLobby);
                           },
                           () -> System.out.println("Declined invite from " + name)
                        );
                        mainMenu.addContinueForm("gameinvite", form);
                        WindowManager.getWindow().requestAttention();
                     }
                  }

                  @Override
                  public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, ChatRoomEnterResponse response) {
                     GameLog.debug.println("Entered lobby " + steamIDLobby + ", " + chatPermissions + ", " + blocked + ", " + response);
                     SteamData.joinedLobby = false;
                     if (steamIDLobby.equals(SteamData.connectLobby)) {
                        SteamData.inLobby = true;
                     }
                  }
               }
            );
            utils = new SteamUtils(new SteamUtilsCallback() {
               @Override
               public void onGamepadTextInputDismissed(boolean submitted) {
                  if (submitted) {
                     int length = SteamData.utils.getEnteredGamepadTextLength();
                     System.out.println("LENGTH: " + length);
                     System.out.println("ENTERED: " + SteamData.utils.getEnteredGamepadTextInput(length));
                  } else {
                     System.out.println("CANCELLED");
                  }
               }
            });
            created = true;
         }

         initialized = true;
      }
   }

   private static boolean initAndIsCreated() {
      if (!initialized) {
         init();
      }

      return isCreated();
   }

   public static boolean isCreated() {
      return created;
   }

   public static SteamApps getApps() {
      return apps;
   }

   public static SteamUtils getUtils() {
      return utils;
   }

   public static SteamID getSteamID() {
      return !initAndIsCreated() ? null : user.getSteamID();
   }

   public static String getSteamName() {
      return !initAndIsCreated() ? null : friends.getPersonaName();
   }

   public static boolean isOverlayActive() {
      return overlayActive;
   }

   public static SteamFriend[] getFriends(FriendFlags... flags) {
      if (!initAndIsCreated()) {
         return new SteamFriend[0];
      }

      List<FriendFlags> flagsList = Arrays.asList(flags);
      int count = friends.getFriendCount(flagsList);
      SteamFriend[] out = new SteamFriend[count];

      for (int i = 0; i < count; i++) {
         SteamID steamID = friends.getFriendByIndex(i, flagsList);
         out[i] = new SteamFriend(steamID) {
            @Override
            public String getName() {
               return SteamData.friends.getFriendPersonaName(this.steamID);
            }

            @Override
            public PersonaState getState() {
               return SteamData.friends.getFriendPersonaState(this.steamID);
            }

            @Override
            public FriendRelationship getRelationship() {
               return SteamData.friends.getFriendRelationship(this.steamID);
            }
         };
      }

      return out;
   }

   public static String getFriendName(SteamID steamIdOfFriend) {
      return friends.getFriendPersonaName(steamIdOfFriend);
   }

   public static Optional<String> tryToGetSteamName(SteamID steamID) {
      SteamFriend[] friends = getFriends(FriendFlags.values());

      for (SteamFriend friend : friends) {
         if (friend.steamID.equals(steamID)) {
            return Optional.of(friend.getName());
         }
      }

      return Optional.empty();
   }

   public static void connectLobby(SteamID steamID) {
      if (steamID.isValid()) {
         connectLobby = steamID;
         joinedLobby = false;
         inLobby = false;
      } else {
         GameLog.warn.println(steamID + " was not a valid Steam lobby id");
      }
   }

   public static boolean isOnCallbackErrorCooldown() {
      long timeSinceLast = System.currentTimeMillis() - lastCallbackError;
      return lastCallbackError != 0L && timeSinceLast < 60000L;
   }

   public static void resetCallbackErrorCooldown() {
      lastCallbackError = System.currentTimeMillis();
   }

   public static boolean setRichPresence(String key, String value) {
      return friends.setRichPresence(key, value);
   }

   public static void clearRichPresence() {
      friends.clearRichPresence();
   }

   public static GameMessage getFriendStatusMessage(PersonaState state) {
      switch (state) {
         case Online:
            return new LocalMessage("ui", "statusonline");
         case Offline:
            return new LocalMessage("ui", "statusoffline");
         case Away:
            return new LocalMessage("ui", "statusaway");
         case Busy:
            return new LocalMessage("ui", "statusbusy");
         case Snooze:
            return new LocalMessage("ui", "statusSnooze");
         case LookingToPlay:
            return new LocalMessage("ui", "statuslookingtoplay");
         case LookingToTrade:
            return new LocalMessage("ui", "statuslookingtotrade");
         default:
            return new StaticMessage(state.toString());
      }
   }

   public static SteamData.ConnectInfo tickLobbyConnectRequested() {
      if (connectLobby != null) {
         if (!joinedLobby && !inLobby) {
            matchmaking.joinLobby(connectLobby);
            connectLobbyTimeout = System.currentTimeMillis() + 5000L;
            joinedLobby = true;
            return null;
         }

         if (inLobby) {
            String steamIDStr = matchmaking.getLobbyData(connectLobby, "serverHostSteamID");
            if (steamIDStr != null && !steamIDStr.isEmpty()) {
               try {
                  long handle = Long.parseLong(steamIDStr);
                  if (handle == 0L) {
                     throw new NumberFormatException();
                  }

                  matchmaking.leaveLobby(connectLobby);
                  connectLobby = null;
                  joinedLobby = false;
                  inLobby = false;
                  return new SteamData.SteamConnectInfo(SteamID.createFromNativeHandle(handle));
               } catch (NumberFormatException e) {
                  GameLog.warn.println("Lobby remote SteamID invalid: " + steamIDStr);
               }
            } else {
               String port = matchmaking.getLobbyData(connectLobby, "serverPort");
               if (port != null && !port.isEmpty()) {
                  String address = matchmaking.getLobbyData(connectLobby, "serverAddress");
                  if (address != null && !address.isEmpty()) {
                     try {
                        int portInt = Integer.parseInt(port);
                        if (portInt < 0) {
                           throw new NumberFormatException();
                        }

                        matchmaking.leaveLobby(connectLobby);
                        connectLobby = null;
                        joinedLobby = false;
                        inLobby = false;
                        return new SteamData.AddressConnectInfo(address, portInt);
                     } catch (NumberFormatException e) {
                        GameLog.warn.println("Lobby port invalid: " + port);
                     }
                  }
               }
            }
         }

         if ((joinedLobby || inLobby) && connectLobbyTimeout < System.currentTimeMillis()) {
            GameLog.warn.println("Timed out getting connect info from lobby " + connectLobby);
            matchmaking.leaveLobby(connectLobby);
            connectLobby = null;
            joinedLobby = false;
            inLobby = false;
            return null;
         }
      }

      return null;
   }

   public static void activateGameOverlayToWebPage(String url) {
      friends.activateGameOverlayToWebPage(url, OverlayToWebPageMode.Default);
   }

   public static boolean showGamepadTextInput(
      GamepadTextInputMode inputMode, GamepadTextLineMode lineMode, String description, int maxCharacters, String existingText
   ) {
      return utils.showGamepadTextInput(inputMode, lineMode, description, maxCharacters, existingText);
   }

   public static boolean showFloatingGamepadTextInput(
      FloatingGamepadTextInputMode keyboardMode, int textFieldXPosition, int textFieldYPosition, int textFieldWidth, int textFieldHeight
   ) {
      return utils.showFloatingGamepadTextInput(keyboardMode, textFieldXPosition, textFieldYPosition, textFieldWidth, textFieldHeight);
   }

   public static void dispose() {
      if (isCreated()) {
         apps.dispose();
         user.dispose();
         friends.dispose();
         matchmaking.dispose();
         utils.dispose();
      }
   }

   public static class AddressConnectInfo extends SteamData.ConnectInfo {
      public final String address;
      public final int port;

      protected AddressConnectInfo(String address, int port) {
         this.address = address;
         this.port = port;
      }

      @Override
      public void startConnectionClient(MainMenu mainMenu) {
         Client client = Platform.getNetworkManager().startJoinServerClient(this.address, this.address, this.port);
         mainMenu.startConnection(client, null);
      }

      @Override
      public String toString() {
         return this.address + ":" + this.port;
      }
   }

   public abstract static class ConnectInfo {
      public abstract void startConnectionClient(MainMenu var1);
   }

   public static class SteamConnectInfo extends SteamData.ConnectInfo {
      public final SteamID remoteID;
      public final String friendName;

      public SteamConnectInfo(SteamID remoteID) {
         this.friendName = SteamData.getFriendName(remoteID);
         this.remoteID = remoteID;
      }

      @Override
      public void startConnectionClient(MainMenu mainMenu) {
         mainMenu.startConnection(((SteamNetworkManager)Platform.getNetworkManager()).startJoinFriendClient(this.friendName, this.remoteID), null);
      }

      @Override
      public String toString() {
         return "STEAM:" + SteamID.getNativeHandle(this.remoteID);
      }
   }
}
