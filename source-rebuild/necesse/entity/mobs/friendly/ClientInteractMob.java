package necesse.entity.mobs.friendly;

import necesse.engine.network.server.ServerClient;

public interface ClientInteractMob {
   InteractingClients getInteractingClients();

   default boolean isBeingInteractedWith() {
      return !this.getInteractingClients().isEmpty();
   }

   default ServerClient getFirstInteractingServerClient() {
      return this.getInteractingClients().getFirstServerClient();
   }

   default void refreshInteracting(ServerClient client) {
      this.getInteractingClients().refresh(client);
   }

   default void removeInteracting(ServerClient client) {
      this.getInteractingClients().remove(client);
   }
}
