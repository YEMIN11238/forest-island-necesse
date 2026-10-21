package necesse.engine.util;

import java.util.Queue;
import java.util.function.Function;

public class MultiTickFunction<T> extends MultiTickFuture<Function<T, T>, T> {
   private T data;

   public MultiTickFunction(Queue<Function<T, T>> tasks, T initialData, float maxTimePerTickMS) {
      this(tasks, initialData, 0, maxTimePerTickMS);
   }

   public MultiTickFunction(Queue<Function<T, T>> tasks, T initialData, int maxComputationsPerTick) {
      this(tasks, initialData, maxComputationsPerTick, 0.0F);
   }

   public MultiTickFunction(Queue<Function<T, T>> tasks, T initialData, int maxComputationsPerTick, float maxTimePerTickMS) {
      super(tasks, maxComputationsPerTick, maxTimePerTickMS);
      this.data = initialData;
   }

   protected void compute(Function<T, T> task) {
      this.data = task.apply(this.data);
   }

   @Override
   protected T getResultingData() {
      return this.data;
   }
}
