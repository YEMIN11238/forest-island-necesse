package necesse.engine.platforms.steam.network.server.network;

import com.codedisaster.steamworks.SteamID;
import java.util.HashMap;
import java.util.function.Consumer;
import necesse.engine.util.GameLinkedList;

public class ServerSteamDeniedConnections {
   private static final int connectionTimeout = 10000;
   private final HashMap<SteamID, GameLinkedList<ServerSteamDeniedConnections.InvitedUser>.Element> steamIDs = new HashMap<>();
   private final GameLinkedList<ServerSteamDeniedConnections.InvitedUser> timeoutQueue = new GameLinkedList<>();

   public synchronized void addDeniedUser(SteamID steamID) {
      GameLinkedList<ServerSteamDeniedConnections.InvitedUser>.Element current = this.steamIDs.get(steamID);
      if (current != null && !current.isRemoved()) {
         current.remove();
      }

      GameLinkedList<ServerSteamDeniedConnections.InvitedUser>.Element element = this.timeoutQueue
         .addLast(new ServerSteamDeniedConnections.InvitedUser(steamID));
      this.steamIDs.put(steamID, element);
   }

   public synchronized void removeDeniedUser(SteamID steamID) {
      GameLinkedList<ServerSteamDeniedConnections.InvitedUser>.Element current = this.steamIDs.remove(steamID);
      if (current != null && !current.isRemoved()) {
         current.remove();
      }
   }

   public synchronized boolean isDenied(SteamID steamID) {
      return this.steamIDs.containsKey(steamID);
   }

   public synchronized void runCleanup(Consumer<SteamID> onTimedOut) {
      while (!this.timeoutQueue.isEmpty()) {
         ServerSteamDeniedConnections.InvitedUser first = this.timeoutQueue.getFirst();
         long timeSinceInvite = System.currentTimeMillis() - first.invitedTime;
         if (timeSinceInvite > 10000L) {
            onTimedOut.accept(first.steamID);
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
