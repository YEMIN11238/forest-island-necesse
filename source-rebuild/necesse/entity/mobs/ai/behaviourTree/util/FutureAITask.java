package necesse.entity.mobs.ai.behaviourTree.util;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import necesse.engine.DisposableExecutorService;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.level.maps.Level;

public class FutureAITask<T> {
   private final AtomicReference<FutureAITask.State> state = new AtomicReference<>(FutureAITask.State.Initial);
   private final DisposableExecutorService executor;
   private final Callable<T> task;
   private final Function<T, AINodeResult> handler;
   private long completeTime = -1L;
   private Exception exception;
   private T result;

   public FutureAITask(DisposableExecutorService executor, Callable<T> task, Function<T, AINodeResult> handler) {
      this.executor = executor;
      Objects.requireNonNull(task);
      Objects.requireNonNull(handler);
      this.task = task;
      this.handler = handler;
   }

   public FutureAITask(Level level, Callable<T> task, Function<T, AINodeResult> handler) {
      this(level.executor(), task, handler);
   }

   public synchronized void runConcurrently() {
      if (this.state.get() != FutureAITask.State.Initial) {
         throw new IllegalStateException("Cannot run task twice");
      }

      if (this.executor == null) {
         throw new IllegalStateException("Executor not supplied");
      }

      if (this.executor.isDisposed()) {
         this.state.set(FutureAITask.State.Failed);
      } else {
         this.state.set(FutureAITask.State.Processing);
         this.executor.submit(() -> {
            long time = System.nanoTime();

            try {
               this.result = this.task.call();
            } catch (Exception e) {
               this.exception = e;
            }

            this.completeTime = System.nanoTime() - time;
            this.state.set(FutureAITask.State.Complete);
            synchronized (this) {
               this.notifyAll();
            }
         });
      }
   }

   public synchronized void runNow() {
      if (this.state.get() != FutureAITask.State.Initial) {
         throw new IllegalStateException("Cannot run task twice");
      }

      this.state.set(FutureAITask.State.Processing);

      try {
         this.result = this.task.call();
      } catch (Exception e) {
         this.exception = e;
      }

      this.state.set(FutureAITask.State.Complete);
   }

   public synchronized boolean isComplete() {
      return this.state.get() == FutureAITask.State.Complete;
   }

   public synchronized boolean isStarted() {
      return this.state.get() != FutureAITask.State.Initial;
   }

   public synchronized boolean hasFailed() {
      return this.state.get() == FutureAITask.State.Failed;
   }

   public synchronized AINodeResult runComplete() throws Exception {
      if (!this.isComplete()) {
         throw new IllegalStateException("Task is not complete");
      } else if (this.exception != null) {
         throw this.exception;
      } else {
         return this.handler.apply(this.result);
      }
   }

   public long getCompleteTime() {
      return this.completeTime;
   }

   private enum State {
      Initial,
      Processing,
      Complete,
      Failed;
   }
}
