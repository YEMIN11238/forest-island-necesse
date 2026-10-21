package necesse.engine.gameLoop.tickManager;

import java.util.HashMap;

public abstract class AbstractPerformanceTimer<T extends AbstractPerformanceTimer<T>> implements Comparable<AbstractPerformanceTimer> {
   public final String name;
   private final T parent;
   private final HashMap<String, T> children = new HashMap<>();

   public AbstractPerformanceTimer(String name, T parent) {
      this.name = name;
      this.parent = parent;
   }

   public int compareTo(AbstractPerformanceTimer other) {
      return this.name.compareTo(other.name);
   }

   public final synchronized T getParent() {
      return this.parent;
   }

   public final synchronized HashMap<String, T> getChildren() {
      return this.children;
   }

   public final synchronized T getPerformanceTimer(String path) {
      return (T)(path.equals("") ? this : PerformanceTimerUtils.getPerformanceTimer(path, this.getChildren()));
   }

   public final synchronized HashMap<String, T> getPerformanceTimers(String path) {
      if (path.equals("")) {
         return this.getChildren();
      }

      T timer = this.getPerformanceTimer(path);
      return timer == null ? null : timer.getChildren();
   }
}
