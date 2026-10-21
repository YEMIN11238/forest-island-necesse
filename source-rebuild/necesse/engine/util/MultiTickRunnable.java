package necesse.engine.util;

import java.util.Queue;

public class MultiTickRunnable extends MultiTickFuture<Runnable, Void> {
   public MultiTickRunnable(Queue<Runnable> tasks, float maxTimePerTickMS) {
      this(tasks, 0, maxTimePerTickMS);
   }

   public MultiTickRunnable(Queue<Runnable> tasks, int maxComputationsPerTick) {
      this(tasks, maxComputationsPerTick, 0.0F);
   }

   public MultiTickRunnable(Queue<Runnable> tasks, int maxComputationsPerTick, float maxTimePerTickMS) {
      super(tasks, maxComputationsPerTick, maxTimePerTickMS);
   }

   protected void compute(Runnable task) {
      task.run();
   }

   protected Void getResultingData() {
      return null;
   }
}
