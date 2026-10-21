package necesse.engine;

import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;

public interface GameState {
   boolean isClient();

   Client getClient();

   boolean isServer();

   Server getServer();

   default String getHostString() {
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
}
