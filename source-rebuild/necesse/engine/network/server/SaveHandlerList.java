package necesse.engine.network.server;

import java.io.IOException;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;

public class SaveHandlerList extends AbstractSaveHandler {
   private final ArrayList<AbstractSaveHandler> handlers;
   private int currentHandlerIndex = 0;

   public SaveHandlerList(PerformanceTimerManager performanceTimer, String debugName, int size) {
      super(performanceTimer, debugName);
      this.handlers = new ArrayList<>(size);
   }

   public void addHandler(AbstractSaveHandler handler) {
      this.handlers.add(handler);
   }

   @Override
   public boolean tickInternal() throws IOException {
      if (this.currentHandlerIndex < this.handlers.size()) {
         AbstractSaveHandler currentHandler = this.handlers.get(this.currentHandlerIndex);
         if (currentHandler.tick()) {
            this.currentHandlerIndex++;
         }

         return false;
      } else {
         return true;
      }
   }
}
