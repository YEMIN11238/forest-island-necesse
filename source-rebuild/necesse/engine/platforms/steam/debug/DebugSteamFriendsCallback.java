package necesse.engine.platforms.steam.debug;

import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamFriends.PersonaChange;

public class DebugSteamFriendsCallback extends DebugSteamCallback implements SteamFriendsCallback {
   @Override
   public void onPersonaStateChange(SteamID steamID, PersonaChange change) {
      this.print("onPersonaStateChange", steamID, change);
   }

   @Override
   public void onGameOverlayActivated(boolean active, boolean userInitiated, int appID) {
      this.print("onGameOverlayActivated", active, userInitiated, appID);
   }

   @Override
   public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
      this.print("onGameLobbyJoinRequested", steamIDLobby, steamIDFriend);
   }

   @Override
   public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
      this.print("onAvatarImageLoaded", steamID, image, width, height);
   }

   @Override
   public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
      this.print("onFriendRichPresenceUpdate", steamIDFriend, appID);
   }

   @Override
   public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
      this.print("onGameRichPresenceJoinRequested", steamIDFriend, connect);
   }

   @Override
   public void onGameServerChangeRequested(String server, String password) {
      this.print("onGameServerChangeRequested", server, password);
   }
}
