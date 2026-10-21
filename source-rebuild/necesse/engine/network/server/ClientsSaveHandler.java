package necesse.engine.network.server;

import java.io.IOException;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;

public class ClientsSaveHandler extends AbstractSaveHandler {
   private final ArrayList<ServerClient> clientsToSave;
   private int currentClientsTick = 0;

   public ClientsSaveHandler(PerformanceTimerManager performanceTimer, Server server) {
      super(performanceTimer, "clientsSave");
      this.clientsToSave = new ArrayList<>(server.getSlots());

      for (int i = 0; i < server.getSlots(); i++) {
         ServerClient client = server.getClient(i);
         if (client != null) {
            this.clientsToSave.add(client);
         }
      }
   }

   @Override
   public boolean tickInternal() throws IOException {
      if (this.currentClientsTick >= 0 && !this.clientsToSave.isEmpty()) {
         for (int i = 0; i < ServerSaveHandler.saveClientsPerTick; i++) {
            this.clientsToSave.get(this.currentClientsTick).saveClient();
            this.currentClientsTick++;
            if (this.currentClientsTick >= this.clientsToSave.size()) {
               this.currentClientsTick = -1;
               break;
            }
         }

         return false;
      } else {
         return true;
      }
   }
}
