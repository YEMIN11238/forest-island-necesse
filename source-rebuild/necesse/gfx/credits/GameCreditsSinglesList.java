package necesse.gfx.credits;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

public class GameCreditsSinglesList extends GameCreditsDisplay {
   protected ArrayList<GameCreditsDisplay> displays = new ArrayList<>();
   protected Dimension drawBounds = new Dimension(0, 0);
   protected TreeSet<GameCreditsSinglesList.TimedDisplay> initializedDisplays = new TreeSet<>(Comparator.comparingInt(c -> c.startTime));
   public boolean centerHorizontally;
   public boolean centerVertically;

   public GameCreditsSinglesList(boolean centerHorizontally, boolean centerVertically) {
      this.centerHorizontally = centerHorizontally;
      this.centerVertically = centerVertically;
   }

   public GameCreditsSinglesList add(GameCreditsDisplay display) {
      this.displays.add(display);
      return this;
   }

   @Override
   public int initDrawAndGetTotalTimeShown() {
      this.initializedDisplays.clear();
      int totalTime = 0;
      int maxWidth = 0;
      int maxHeight = 0;

      for (GameCreditsDisplay display : this.displays) {
         int timeShown = display.initDrawAndGetTotalTimeShown();
         if (timeShown > 0) {
            Dimension drawBounds = display.getDrawBounds();
            if (drawBounds.width > maxWidth) {
               maxWidth = drawBounds.width;
            }

            if (drawBounds.height > maxHeight) {
               maxHeight = drawBounds.height;
            }

            this.initializedDisplays.add(new GameCreditsSinglesList.TimedDisplay(display, totalTime, timeShown, drawBounds));
            totalTime += timeShown;
         }
      }

      for (GameCreditsSinglesList.TimedDisplay display : this.initializedDisplays) {
         if (this.centerHorizontally) {
            display.drawOffsetX = (maxWidth - display.drawBounds.width) / 2;
         }

         if (this.centerVertically) {
            display.drawOffsetY = (maxHeight - display.drawBounds.height) / 2;
         }
      }

      this.drawBounds = new Dimension(maxWidth, maxHeight);
      return totalTime;
   }

   protected GameCreditsSinglesList.TimedDisplay getCurrentDisplay(int currentTime) {
      NavigableSet<GameCreditsSinglesList.TimedDisplay> timedDisplays = this.initializedDisplays
         .headSet(new GameCreditsSinglesList.TimedDisplay(currentTime), true);
      return timedDisplays.isEmpty() ? null : timedDisplays.last();
   }

   @Override
   public Dimension getDrawBounds() {
      return this.drawBounds;
   }

   @Override
   public void draw(int currentTime, int x, int y, float alpha) {
      GameCreditsSinglesList.TimedDisplay current = this.getCurrentDisplay(currentTime);
      if (current != null) {
         current.display.draw(currentTime - current.startTime, x + current.drawOffsetX, y + current.drawOffsetY, alpha);
      }
   }

   @Override
   public boolean isDone(int currentTime) {
      return this.getCurrentDisplay(currentTime) == null;
   }

   protected static class TimedDisplay {
      public GameCreditsDisplay display;
      public int startTime;
      public int timeShown;
      public Dimension drawBounds;
      public int drawOffsetX;
      public int drawOffsetY;

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
