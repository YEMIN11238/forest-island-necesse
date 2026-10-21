package necesse.engine.network;

import java.util.HashMap;
import necesse.engine.dlc.DLC;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.server.ClientRelationshipData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;

public abstract class NetworkClient {
   private boolean isRemote;
   public boolean pvpEnabled;
   private int teamID = -1;
   public final int slot;
   public final long authentication;
   public PlayerMob playerMob;
   protected boolean isDead;
   protected boolean hasSpawned;
   protected boolean isDisposed;
   public boolean craftingUsesNearbyInventories;
   public boolean trackNewQuests;
   public final HashMap<Integer, DLC> installedDLC = new HashMap<>();
   public final ClientRelationshipData relationshipData = new ClientRelationshipData();

   public NetworkClient(int slot, long authentication) {
      this.slot = slot;
      this.authentication = authentication;
   }

   public abstract boolean pvpEnabled();

   public void setTeamID(int teamID) {
      if (teamID >= -1 && teamID <= 32767) {
         this.teamID = teamID;
         if (this.playerMob != null) {
            this.playerMob.setTeam(teamID);
         }
      } else {
         System.out.println("Attempted invalid team for network client: " + teamID);
      }
   }

   public int getTeamID() {
      return this.teamID;
   }

   public boolean isSameTeam(NetworkClient other) {
      return this.isSameTeam(other.getTeamID());
   }

   public boolean isSameTeam(int teamID) {
      return this.getTeamID() != -1 && teamID != -1 ? this.getTeamID() == teamID : false;
   }

   protected void makeClientClient() {
      this.isRemote = true;
   }

   protected void makeServerClient() {
      this.isRemote = false;
   }

   public boolean isClient() {
      return this.isRemote;
   }

   public boolean isServer() {
      return !this.isRemote;
   }

   public String getHostString() {
      boolean server = this.isServer();
      boolean client = this.isClient();
      if (server && client) {
         return "BOTH";
      } else if (server) {
         return "SERVER";
      } else {
         return client ? "CLIENT" : "NONE";
      }
   }

   @Deprecated
   public boolean isClientClient() {
      return this.isClient();
   }

   @Deprecated
   public boolean isServerClient() {
      return this.isServer();
   }

   public ClientClient getClientClient() {
      return (ClientClient)this;
   }

   public ServerClient getServerClient() {
      return (ServerClient)this;
   }

   public boolean isDead() {
      return this.isDead;
   }

   public boolean hasSpawned() {
      return this.hasSpawned;
   }

   public void dispose() {
      this.isDisposed = true;
      if (this.playerMob != null) {
         this.playerMob.remove();
      }
   }

   public boolean isDisposed() {
      return this.isDisposed;
   }

   public abstract String getName();

   public abstract LevelIdentifier getLevelIdentifier();

   public final boolean isSamePlace(LevelIdentifier levelIdentifier) {
      return this.getLevelIdentifier().equals(levelIdentifier);
   }

   public final boolean isSamePlace(int islandX, int islandY, int dimension) {
      return this.getLevelIdentifier().equals(islandX, islandY, dimension);
   }

   public final boolean isSamePlace(NetworkClient other) {
      return this.isSamePlace(other.getLevelIdentifier());
   }

   public final boolean isSamePlace(Level level) {
      return level == null ? false : this.isSamePlace(level.getIdentifier());
   }

   public boolean isSupporter() {
      return this.hasDLC(DLC.SUPPORTER_PACK);
   }

   public boolean hasDLC(DLC dlc) {
      return this.installedDLC.containsKey(dlc.getID());
   }
}
