package necesse.engine.platforms.steam.network.server.network;

import com.codedisaster.steamworks.SteamID;
import java.util.HashMap;
import necesse.engine.util.GameLinkedList;

public class ServerSteamInvitedUsers {
   private static final int invitedUsersTimeout = 600000;
   private final HashMap<SteamID, GameLinkedList<ServerSteamInvitedUsers.InvitedUser>.Element> steamIDs = new HashMap<>();
   private final GameLinkedList<ServerSteamInvitedUsers.InvitedUser> timeoutQueue = new GameLinkedList<>();

   public synchronized void addInvitedUser(SteamID steamID) {
      this.clean();
      GameLinkedList<ServerSteamInvitedUsers.InvitedUser>.Element current = this.steamIDs.get(steamID);
      if (current != null && !current.isRemoved()) {
         current.remove();
      }

      GameLinkedList<ServerSteamInvitedUsers.InvitedUser>.Element element = this.timeoutQueue.addLast(new ServerSteamInvitedUsers.InvitedUser(steamID));
      this.steamIDs.put(steamID, element);
   }

   public synchronized boolean isInvited(SteamID steamID) {
      this.clean();
      return this.steamIDs.containsKey(steamID);
   }

   public synchronized void clean() {
      while (!this.timeoutQueue.isEmpty()) {
         ServerSteamInvitedUsers.InvitedUser first = this.timeoutQueue.getFirst();
         long timeSinceInvite = System.currentTimeMillis() - first.invitedTime;
         if (timeSinceInvite > 600000L) {
            this.steamIDs.remove(first.steamID);
            this.timeoutQueue.removeFirst();
            continue;
         }
         break;
      }
   }

   private static class InvitedUser {
      public final SteamID steamID;
      public long invitedTime;

      public InvitedUser(SteamID steamID) {
         this.steamID = steamID;
         this.invitedTime = System.currentTimeMillis();
      }
   }
}
