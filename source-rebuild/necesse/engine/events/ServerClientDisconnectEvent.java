package necesse.engine.events;

import necesse.engine.network.server.ServerClient;

public class ServerClientDisconnectEvent extends GameEvent {
   public final ServerClient client;

   public ServerClientDisconnectEvent(ServerClient client) {
      this.client = client;
   }
}
