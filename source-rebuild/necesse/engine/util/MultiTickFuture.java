package necesse.engine.util;

import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class MultiTickFuture<T, R> extends CompletableFuture<R> {
   public int maxComputationsPerTick = 0;
   public float maxTimePerTickMS = 0.0F;
   private int taskCount = 0;
   private int lastTickComputations = 0;
   private double lastTickTimeMS = 0.0;
   private final Queue<T> tasks;

   public MultiTickFuture(Queue<T> tasks, float maxTimePerTickMS) {
      this(tasks, 0, maxTimePerTickMS);
   }

   public MultiTickFuture(Queue<T> tasks, int maxComputationsPerTick) {
      this(tasks, maxComputationsPerTick, 0.0F);
   }

   public MultiTickFuture(Queue<T> tasks, int maxComputationsPerTick, float maxTimePerTickMS) {
      this.tasks = tasks;
      this.maxComputationsPerTick = maxComputationsPerTick;
      this.maxTimePerTickMS = maxTimePerTickMS;
      this.taskCount = tasks.size();
   }

   @Override
   public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      long timeoutNanos = unit.toNanos(timeout);
      if (this.isCancelled()) {
         throw new CancellationException();
      }

      while (!this.isDone() && timeoutNanos > 0L) {
         long start = System.nanoTime();
         T task = this.tasks.poll();
         if (task != null) {
            this.compute(task);
         }

         timeoutNanos -= System.nanoTime() - start;
      }

      if (this.isDone()) {
         return this.getResultingData();
      } else {
         throw new TimeoutException();
      }
   }

   @Override
   public R get() throws InterruptedException, ExecutionException {
      if (this.isCancelled()) {
         throw new CancellationException();
      }

      while (!this.isDone()) {
         T task = this.tasks.poll();
         if (task != null) {
            this.compute(task);
         }
      }

      return (R)super.get();
   }

   @Override
   public R join() {
      if (this.isCancelled()) {
         throw new CancellationException();
      }

      while (!this.isDone()) {
         T task = this.tasks.poll();
         if (task != null) {
            this.compute(task);
         }
      }

      return (R)super.join();
   }

   @Override
   public boolean isDone() {
      boolean isDone = super.isDone();
      if (!isDone && this.tasks.isEmpty()) {
         this.complete(this.getResultingData());
         return true;
      } else {
         return isDone;
      }
   }

   public boolean tick() {
      if (this.isDone()) {
         return true;
      }

      if (this.maxTimePerTickMS <= 0.0F && this.maxComputationsPerTick <= 0) {
         throw new IllegalArgumentException("At least one of maxComputationsPerTick or maxTimePerTickMS must be greater than 0");
      }

      int maxComputations = this.maxComputationsPerTick <= 0 ? Integer.MAX_VALUE : this.maxComputationsPerTick;
      float maxTime = this.maxTimePerTickMS <= 0.0F ? Float.MAX_VALUE : this.maxTimePerTickMS;
      this.lastTickComputations = 0;
      this.lastTickTimeMS = 0.0;

      T task;
      for (double lastTaskTime = 0.0;
         !this.isCancelled()
            && this.lastTickComputations < maxComputations
            && this.lastTickTimeMS + lastTaskTime <= maxTime
            && (task = this.tasks.poll()) != null;
         this.lastTickTimeMS += lastTaskTime
      ) {
         long startTime = System.nanoTime();
         this.compute(task);
         lastTaskTime = (System.nanoTime() - startTime) / 1000000.0;
         this.lastTickComputations++;
      }

      return this.isDone();
   }

   protected abstract void compute(T var1);

   protected abstract R getResultingData();

   public double getLastTickTimeMS() {
      return this.lastTickTimeMS;
   }

   public int getLastTickComputations() {
      return this.lastTickComputations;
   }

   public int getTasksLeft() {
      return this.tasks.size();
   }

   public int getTotalTaskCount() {
      return this.taskCount;
   }

   public float getProgressPercent() {
      return this.taskCount <= 0 ? 1.0F : 1.0F - (float)this.tasks.size() / this.taskCount;
   }
}
