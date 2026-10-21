package necesse.gfx.credits;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

public class GameCreditsTopToBotList extends GameCreditsDisplay {
   protected ArrayList<GameCreditsDisplay> displays = new ArrayList<>();
   protected Dimension drawBounds = new Dimension(0, 0);
   protected TreeSet<GameCreditsTopToBotList.TimedDisplay> initializedDisplays = new TreeSet<>(Comparator.comparingInt(c -> c.startTime));
   public boolean centerText;
   public boolean keepShowingLast;

   public GameCreditsTopToBotList(boolean centerText, boolean keepShowingLast) {
      this.centerText = centerText;
      this.keepShowingLast = keepShowingLast;
   }

   public GameCreditsTopToBotList add(GameCreditsDisplay display) {
      this.displays.add(display);
      return this;
   }

   public int size() {
      return this.displays.size();
   }

   @Override
   public int initDrawAndGetTotalTimeShown() {
      this.initializedDisplays.clear();
      int totalTime = 0;
      int maxWidth = 0;

      for (GameCreditsDisplay display : this.displays) {
         int timeShown = display.initDrawAndGetTotalTimeShown();
         if (timeShown > 0) {
            Dimension drawBounds = display.getDrawBounds();
            if (drawBounds.width > maxWidth) {
               maxWidth = drawBounds.width;
            }

            this.initializedDisplays.add(new GameCreditsTopToBotList.TimedDisplay(display, totalTime, timeShown, drawBounds));
            totalTime += timeShown;
         }
      }

      int currentHeight = 0;

      for (GameCreditsTopToBotList.TimedDisplay display : this.initializedDisplays) {
         if (this.centerText) {
            display.drawXOffset = (maxWidth - display.drawBounds.width) / 2;
         }

         display.drawYOffset = currentHeight;
         currentHeight += display.drawBounds.height;
      }

      this.drawBounds = new Dimension(maxWidth, currentHeight);
      return totalTime;
   }

   protected NavigableSet<GameCreditsTopToBotList.TimedDisplay> getCurrentDisplays(int currentTime) {
      NavigableSet<GameCreditsTopToBotList.TimedDisplay> timedDisplays = this.initializedDisplays
         .headSet(new GameCreditsTopToBotList.TimedDisplay(currentTime), true);
      if (timedDisplays.isEmpty()) {
         return null;
      }

      GameCreditsTopToBotList.TimedDisplay last = timedDisplays.last();
      return !this.keepShowingLast && last.startTime + last.timeShown <= currentTime ? null : timedDisplays;
   }

   @Override
   public Dimension getDrawBounds() {
      return this.drawBounds;
   }

   @Override
   public void draw(int currentTime, int x, int y, float alpha) {
      NavigableSet<GameCreditsTopToBotList.TimedDisplay> currentDisplays = this.getCurrentDisplays(currentTime);
      if (currentDisplays != null) {
         for (GameCreditsTopToBotList.TimedDisplay currentDisplay : currentDisplays) {
            currentDisplay.display.draw(currentTime - currentDisplay.startTime, x + currentDisplay.drawXOffset, y + currentDisplay.drawYOffset, alpha);
         }
      }
   }

   @Override
   public boolean isDone(int currentTime) {
      NavigableSet<GameCreditsTopToBotList.TimedDisplay> current = this.getCurrentDisplays(currentTime);
      if (current != null && !current.isEmpty()) {
         GameCreditsTopToBotList.TimedDisplay last = current.last();
         return last.startTime + last.timeShown <= currentTime;
      } else {
         return true;
      }
   }

   protected static class TimedDisplay {
      public GameCreditsDisplay display;
      public int startTime;
      public int timeShown;
      public Dimension drawBounds;
      public int drawXOffset;
      public int drawYOffset;

      protected TimedDisplay(GameCreditsDisplay display, int startTime, int timeShown, Dimension drawBounds) {
         this.display = display;
         this.startTime = startTime;
         this.timeShown = timeShown;
         this.drawBounds = drawBounds;
      }

      protected TimedDisplay(int currentTime) {
         this.startTime = currentTime;
      }
   }
}
