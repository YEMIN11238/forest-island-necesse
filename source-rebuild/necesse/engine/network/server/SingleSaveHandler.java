package necesse.engine.network.server;

import java.io.IOException;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;

public class SingleSaveHandler extends AbstractSaveHandler {
   private SingleSaveHandler.IORunnable runnable;

   public SingleSaveHandler(PerformanceTimerManager performanceTimer, String debugName, SingleSaveHandler.IORunnable runnable) {
      super(performanceTimer, debugName);
      this.runnable = runnable;
   }

   @Override
   public boolean tickInternal() throws IOException {
      if (this.runnable != null) {
         this.runnable.run();
         this.runnable = null;
         return false;
      } else {
         return true;
      }
   }

   public interface IORunnable {
      void run() throws IOException;
   }
}
