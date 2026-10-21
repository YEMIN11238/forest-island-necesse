package necesse.engine.events;

import necesse.engine.network.server.Server;

public class ServerStartEvent extends GameEvent {
   public final Server server;

   public ServerStartEvent(Server server) {
      this.server = server;
   }
}
