package necesse.engine.platforms.steam;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamFriends.FriendRelationship;
import com.codedisaster.steamworks.SteamFriends.PersonaState;

public abstract class SteamFriend {
   public final SteamID steamID;

   public SteamFriend(SteamID steamID) {
      this.steamID = steamID;
   }

   public abstract String getName();

   public abstract PersonaState getState();

   public abstract FriendRelationship getRelationship();
}
