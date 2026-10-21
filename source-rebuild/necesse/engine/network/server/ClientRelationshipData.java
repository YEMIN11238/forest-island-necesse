package necesse.engine.network.server;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.worldData.SettlersWorldData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;

public class ClientRelationshipData {
   protected long relationshipIdentifier = -1L;
   protected boolean relationshipIsPlayerAuth;

   public void addSaveData(SaveData save) {
      if (this.relationshipIdentifier != -1L) {
         save.addLong("relationshipIdentifier", this.relationshipIdentifier);
         save.addBoolean("relationshipIsPlayerAuth", this.relationshipIsPlayerAuth);
      }
   }

   public void applySaveData(LoadData save) {
      this.relationshipIdentifier = save.getLong("relationshipIdentifier", this.relationshipIdentifier, false);
      this.relationshipIsPlayerAuth = save.getBoolean("relationshipIsPlayerAuth", this.relationshipIsPlayerAuth, false);
   }

   public void writePacket(PacketWriter writer) {
      if (this.isInAnyRelationship()) {
         writer.putNextBoolean(true);
         writer.putNextLong(this.relationshipIdentifier);
         writer.putNextBoolean(this.relationshipIsPlayerAuth);
      } else {
         writer.putNextBoolean(false);
      }
   }

   public void readPacket(PacketReader reader) {
      if (reader.getNextBoolean()) {
         this.relationshipIdentifier = reader.getNextLong();
         this.relationshipIsPlayerAuth = reader.getNextBoolean();
      } else {
         this.clearRelationship();
         this.relationshipIsPlayerAuth = false;
      }
   }

   public boolean hasNeverBeenInRelationship() {
      return this.relationshipIdentifier == -1L;
   }

   public boolean isInAnyRelationship() {
      return this.relationshipIdentifier != -1L && this.relationshipIdentifier != -2L;
   }

   public boolean isInPlayerRelationship() {
      return this.isInAnyRelationship() && this.relationshipIsPlayerAuth;
   }

   public boolean isInSettlerRelationship() {
      return this.isInAnyRelationship() && !this.relationshipIsPlayerAuth;
   }

   public boolean isInRelationshipWithPlayer(long playerAuth) {
      return this.relationshipIdentifier == playerAuth && this.relationshipIsPlayerAuth;
   }

   public boolean isInRelationshipWithPlayer(NetworkClient client) {
      return this.isInRelationshipWithPlayer(client.authentication);
   }

   public boolean isInRelationshipWithSettler(int mobUniqueID) {
      return this.relationshipIdentifier == mobUniqueID && !this.relationshipIsPlayerAuth;
   }

   public void setIsInRelationshipWith(NetworkClient client) {
      this.relationshipIdentifier = client.authentication;
      this.relationshipIsPlayerAuth = true;
   }

   public void setIsInRelationshipWith(Mob mob) {
      this.relationshipIdentifier = mob.getUniqueID();
      this.relationshipIsPlayerAuth = false;
   }

   public PlayerMob getRelationshipPlayer(Client client) {
      return !this.isInPlayerRelationship() ? null : client.getPlayerByAuth(this.relationshipIdentifier);
   }

   public PlayerMob getRelationshipPlayer(Server server) {
      return !this.isInPlayerRelationship() ? null : server.getPlayerByAuth(this.relationshipIdentifier);
   }

   public void clearRelationship() {
      if (this.relationshipIdentifier != -1L) {
         this.relationshipIdentifier = -2L;
      }
   }

   public boolean checkPlayerRelationshipValid(ServerClient client) {
      if (!this.isInPlayerRelationship()) {
         return true;
      }

      SavedServerClientData savedClient = client.getServer().getSavedClientData(this.relationshipIdentifier);
      if (savedClient != null && savedClient.relationshipData.isInRelationshipWithPlayer(client.authentication)) {
         return true;
      }

      if (savedClient != null) {
         savedClient.relationshipData.clearRelationship();
      }

      this.clearRelationship();
      return false;
   }

   public boolean checkSettlerRelationshipValid(Server server) {
      if (!this.isInSettlerRelationship()) {
         return true;
      } else {
         SettlersWorldData settlersData = SettlersWorldData.getSettlersData(server);
         int mobUniqueID = (int)this.relationshipIdentifier;
         if (settlersData.isRelationshipSettlerDead(mobUniqueID)) {
            settlersData.clearDeadRelationshipSettler(mobUniqueID);
            this.clearRelationship();
            return false;
         } else {
            return true;
         }
      }
   }
}
