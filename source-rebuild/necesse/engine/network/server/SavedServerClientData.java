package necesse.engine.network.server;

import necesse.engine.playerStats.PlayerStats;
import necesse.gfx.HumanLook;

public class SavedServerClientData {
   public final long authentication;
   public String name;
   public HumanLook look;
   public PlayerStats characterStats;
   public ClientRelationshipData relationshipData;

   public SavedServerClientData(long authentication, String name, HumanLook look, PlayerStats characterStats, ClientRelationshipData relationshipData) {
      this.authentication = authentication;
      this.name = name;
      this.look = look;
      this.characterStats = characterStats;
      this.relationshipData = relationshipData;
   }

   public SavedServerClientData(ServerClient client) {
      this.authentication = client.authentication;
      this.update(client);
   }

   public void update(ServerClient client) {
      this.name = client.needAppearance() ? "N/A" : client.getName();
      this.look = client.playerMob == null ? null : client.playerMob.look;
      this.characterStats = client.characterStats();
      this.relationshipData = client.relationshipData;
   }
}
