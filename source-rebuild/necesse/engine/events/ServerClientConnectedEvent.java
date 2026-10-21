package necesse.engine.events;

import necesse.engine.network.server.ServerClient;

public class ServerClientConnectedEvent extends GameEvent {
   public final ServerClient client;

   public ServerClientConnectedEvent(ServerClient client) {
      this.client = client;
   }
}
