package necesse.engine.gameLoop.tickManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Consumer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;

public class PerformanceTimerUtils {
   public static <T extends AbstractPerformanceTimer<T>> T getPerformanceTimer(String path, HashMap<String, T> timers) {
      if (path.equals("")) {
         return null;
      }

      String[] split = path.split("/");
      T timer = (T)timers.get(split[0]);
      if (timer == null) {
         return null;
      }

      for (int i = 1; i < split.length; i++) {
         timer = (T)timer.getChildren().getOrDefault(split[i], null);
         if (timer == null) {
            return null;
         }
      }

      return timer;
   }

   public static PerformanceTotal combineTimers(Collection<PerformanceTimer> timers) {
      if (timers.isEmpty()) {
         return null;
      }

      PerformanceTotal total = new PerformanceTotal("total", true);

      for (PerformanceTimer timer : timers) {
         total.append(timer);
      }

      return total;
   }

   public static void printPerformanceTimer(PerformanceTimer timer) {
      printPerformanceTimer(timer, System.out::println);
   }

   public static void printPerformanceTimer(PerformanceTimer timer, Consumer<String> linePrinter) {
      printPerformanceTimer("", 0, timer, linePrinter);
   }

   private static void printPerformanceTimer(String prefix, int nameLength, PerformanceTimer timer, Consumer<String> linePrinter) {
      int spaces = nameLength - timer.name.length();
      StringBuilder builder = new StringBuilder(prefix).append(timer.name).append(" ");

      for (int i = 0; i < spaces; i++) {
         builder.append(" ");
      }

      builder.append(GameUtils.getTimeStringNano(timer.getTime()));
      PerformanceTimer parent = timer.getParent();
      if (parent != null) {
         double perc = (double)timer.getTime() / parent.getTime() * 100.0;
         builder.append(" - ").append(GameMath.toDecimals(perc, 2)).append("%").append(" - ").append(timer.getCalls()).append(" calls");
      }

      linePrinter.accept(builder.toString());
      int childMinNameLength = 0;
      ArrayList<PerformanceTimer> sortedChildren = new ArrayList<>(timer.getChildren().size());

      for (PerformanceTimer child : timer.getChildren().values()) {
         childMinNameLength = Math.max(childMinNameLength, child.name.length());
         sortedChildren.add(child);
      }

      sortedChildren.sort(Comparator.comparing(PerformanceTimer::getTime).reversed());

      for (PerformanceTimer child : sortedChildren) {
         printPerformanceTimer(prefix + "\t", childMinNameLength, child, linePrinter);
      }
   }
}
