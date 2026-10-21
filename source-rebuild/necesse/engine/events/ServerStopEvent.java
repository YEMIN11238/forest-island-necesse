package necesse.engine.events;

import necesse.engine.network.server.Server;

public class ServerStopEvent extends GameEvent {
   public final Server server;

   public ServerStopEvent(Server server) {
      this.server = server;
   }
}
