package necesse.engine.gameLoop.tickManager;

import java.util.function.Supplier;

public class Performance {
   public static void record(PerformanceTimerManager manager, String id, Runnable logic) {
      if (manager != null) {
         manager.recordPerformance(id, logic);
      } else {
         logic.run();
      }
   }

   public static void addRecordedTime(PerformanceTimerManager manager, String id, long nanoSeconds) {
      if (manager != null) {
         manager.addRecordedTime(id, nanoSeconds);
      }
   }

   public static void recordConstant(PerformanceTimerManager manager, String id, Runnable logic) {
      if (manager != null) {
         manager.recordConstantPerformance(id, logic);
      } else {
         logic.run();
      }
   }

   public static void addConstantRecordedTime(PerformanceTimerManager manager, String id, long nanoSeconds) {
      if (manager != null) {
         manager.addConstantRecordedTime(id, nanoSeconds);
      }
   }

   public static void record(PerformanceTimerManager manager, String id, boolean constant, Runnable logic) {
      if (manager != null) {
         if (constant) {
            manager.recordConstantPerformance(id, logic);
         } else {
            manager.recordPerformance(id, logic);
         }
      } else {
         logic.run();
      }
   }

   public static void addRecordedTime(PerformanceTimerManager manager, String id, boolean constant, long nanoSeconds) {
      if (manager != null) {
         if (constant) {
            manager.addConstantRecordedTime(id, nanoSeconds);
         } else {
            manager.addRecordedTime(id, nanoSeconds);
         }
      }
   }

   public static <T> T record(PerformanceTimerManager manager, String id, Supplier<T> task) {
      return manager != null ? manager.recordPerformance(id, task) : task.get();
   }

   public static <T> T recordConstant(PerformanceTimerManager manager, String id, Supplier<T> task) {
      return manager != null ? manager.recordConstantPerformance(id, task) : task.get();
   }

   public static <T> T record(PerformanceTimerManager manager, String id, boolean constant, Supplier<T> task) {
      if (manager != null) {
         return constant ? manager.recordConstantPerformance(id, task) : manager.recordPerformance(id, task);
      } else {
         return task.get();
      }
   }

   public static void recordGlobal(PerformanceTimerManager manager, String id, Runnable logic) {
      if (manager != null) {
         manager.recordGlobalPerformance(id, logic);
      } else {
         logic.run();
      }
   }

   public static void recordGlobalConstant(PerformanceTimerManager manager, String id, Runnable logic) {
      if (manager != null) {
         manager.recordGlobalConstantPerformance(id, logic);
      } else {
         logic.run();
      }
   }

   public static <T> T recordGlobal(PerformanceTimerManager manager, String id, Supplier<T> task) {
      return manager != null ? manager.recordGlobalPerformance(id, task) : task.get();
   }

   public static <T> T recordGlobalConstant(PerformanceTimerManager manager, String id, Supplier<T> task) {
      return manager != null ? manager.recordGlobalConstantPerformance(id, task) : task.get();
   }

   public static PerformanceWrapper wrapTimer(PerformanceTimerManager manager, String id) {
      return manager != null ? manager.wrapTimer(id) : new PerformanceWrapper() {
         @Override
         protected void endLogic() {
         }
      };
   }

   public static PerformanceWrapper wrapConstantTimer(PerformanceTimerManager manager, String id) {
      return manager != null ? manager.wrapConstantTimer(id) : new PerformanceWrapper() {
         @Override
         protected void endLogic() {
         }
      };
   }

   public static PerformanceWrapper wrapTimer(PerformanceTimerManager manager, String id, boolean constant) {
      if (manager != null) {
         return constant ? manager.wrapConstantTimer(id) : manager.wrapTimer(id);
      } else {
         return new PerformanceWrapper() {
            @Override
            protected void endLogic() {
            }
         };
      }
   }

   public static void runCustomTimer(PerformanceTimerManager manager, PerformanceTimerManager customTimer, Runnable logic) {
      if (manager != null) {
         manager.addCustomTimer(customTimer, logic);
      } else {
         logic.run();
      }
   }
}
