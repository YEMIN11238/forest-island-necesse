package necesse.engine.gameLoop.tickManager;

import java.util.Iterator;
import java.util.LinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;

public class PerformanceTimerAverage extends AbstractPerformanceTimer<PerformanceTimerAverage> {
   private LinkedList<PerformanceTimer> frames = new LinkedList<>();
   private long totalTime;
   private int totalCalls;

   public PerformanceTimerAverage(Iterator<PerformanceTimer> frames) {
      this("root", null, frames);
   }

   private PerformanceTimerAverage(String name, PerformanceTimerAverage parent, Iterator<PerformanceTimer> frames) {
      super(name, parent);
      this.applyFrames(frames);
   }

   private void applyFrames(Iterator<PerformanceTimer> frames) {
      while (frames.hasNext()) {
         PerformanceTimer frame = frames.next();
         if (!frame.isFirstFrame) {
            if (!frame.name.equals(this.name)) {
               continue;
            }

            this.frames.add(frame);
            this.totalTime = this.totalTime + frame.getTime();
            this.totalCalls = this.totalCalls + frame.getCalls();

            for (PerformanceTimer frameChild : frame.getChildren().values()) {
               PerformanceTimerAverage child = this.getChildren().getOrDefault(frameChild.name, null);
               if (child == null) {
                  this.getChildren().put(frameChild.name, new PerformanceTimerAverage(frameChild.name, this, frame.getChildren().values().iterator()));
               } else {
                  child.applyFrames(frame.getChildren().values().iterator());
               }
            }
            continue;
         }
         break;
      }
   }

   public long getTotalTime() {
      return this.totalTime;
   }

   public long getAverageTime() {
      return this.frames.isEmpty() ? 0L : this.totalTime / this.frames.size();
   }

   public int getTotalCalls() {
      return this.totalCalls;
   }

   public double getAverageCalls() {
      return this.frames.isEmpty() ? 0.0 : (double)this.totalCalls / this.frames.size();
   }

   public float getAverageTimePercent() {
      if (this.getParent() == null) {
         return 100.0F;
      }

      long parentAverage = this.getParent().getAverageTime();
      if (parentAverage == 0L) {
         return 100.0F;
      }

      double percent = (double)this.getAverageTime() / parentAverage;
      return (int)(percent * 100000.0) / 1000.0F;
   }

   public synchronized LinkedList<PerformanceTimer> getFrames() {
      return this.frames;
   }

   public void printTotalTimeTree() {
      this.printTotalTimeTree("", false);
   }

   private void printTotalTimeTree(String prefix, boolean printPercent) {
      if (printPercent) {
         float percent = this.getParent() == null ? 1.0F : (float)this.totalTime / (float)this.getParent().totalTime;
         System.out
            .println(
               prefix
                  + this.name
                  + " - "
                  + GameMath.toDecimals(percent * 100.0F, 2)
                  + "% - "
                  + GameUtils.getTimeStringNano(this.totalTime)
                  + " - "
                  + this.totalCalls
            );
      } else {
         System.out.println(prefix + this.name + " - " + GameUtils.getTimeStringNano(this.totalTime) + " - " + this.totalCalls);
      }

      for (PerformanceTimerAverage timer : this.getChildren().values()) {
         timer.printTotalTimeTree(prefix + "\t", true);
      }
   }

   public float getPercent() {
      if (this.totalTime == 0L) {
         return 1.0F;
      }

      double percent = (double)this.getAverageTime() / this.totalTime;
      return (int)(percent * 100000.0) / 1000.0F;
   }

   public double getPercentDouble() {
      return this.totalTime == 0L ? 100.0 : (double)this.getAverageTime() / this.totalTime;
   }
}
