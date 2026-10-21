package necesse.engine.network.server;

import java.io.IOException;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.gameLoop.tickManager.PerformanceWrapper;

public abstract class AbstractSaveHandler {
   public final PerformanceTimerManager performanceTimer;
   public final String debugName;
   public int ticksToSkip = 0;
   private int currentTick = 0;

   public AbstractSaveHandler(PerformanceTimerManager performanceTimer, String debugName) {
      this.performanceTimer = performanceTimer;
      this.debugName = debugName;
   }

   protected abstract boolean tickInternal() throws IOException;

   public final boolean tick() throws IOException {
      if (this.currentTick < this.ticksToSkip) {
         this.currentTick++;
         return false;
      }

      this.currentTick = 0;
      if (this.debugName != null) {
         PerformanceWrapper wrapper = Performance.wrapTimer(this.performanceTimer, this.debugName);

         try {
            return this.tickInternal();
         } finally {
            wrapper.end();
         }
      } else {
         return this.tickInternal();
      }
   }
}
