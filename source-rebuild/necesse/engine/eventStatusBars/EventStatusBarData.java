package necesse.engine.eventStatusBars;

import java.awt.Color;
import java.util.LinkedList;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.util.GameMath;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.FairTypeDrawOptionsContainer;
import necesse.gfx.gameFont.FontOptions;

public class EventStatusBarData {
   public int dataBufferTime = 1000;
   private final FairTypeDrawOptionsContainer displayNameDrawOptions;
   public final EventStatusBarData.BarCategory category;
   private final LinkedList<EventStatusBarData.StatusAtTime> buffer = new LinkedList<>();

   public EventStatusBarData(EventStatusBarData.BarCategory category, GameMessage displayName) {
      this.displayNameDrawOptions = new FairTypeDrawOptionsContainer(
         () -> new FairType().append(new FontOptions(16).outline(), displayName.translate()).getDrawOptions(FairType.TextAlign.CENTER)
      );
      this.displayNameDrawOptions.updateOnLanguageChange();
      this.category = category;
   }

   public EventStatusBarData append(int current, int max) {
      this.buffer.addFirst(new EventStatusBarData.StatusAtTime(current, max, System.currentTimeMillis()));
      return this;
   }

   public void cleanOldData() {
      while (!this.buffer.isEmpty()) {
         EventStatusBarData.StatusAtTime last = this.buffer.getLast();
         if (last.time + this.dataBufferTime < System.currentTimeMillis()) {
            this.buffer.removeLast();
            continue;
         }
         break;
      }
   }

   public boolean hasData() {
      return !this.buffer.isEmpty();
   }

   public EventStatusBarData.StatusAtTime getLatest() {
      return this.buffer.getFirst();
   }

   public EventStatusBarData.StatusAtTime getBuffered() {
      return this.buffer.getLast();
   }

   public FairTypeDrawOptions getDisplayNameDrawOptions() {
      return this.displayNameDrawOptions.get();
   }

   public GameMessage getStatusText(EventStatusBarData.StatusAtTime status) {
      float perc = status.getPercent();
      float percRounded = (int)(perc * 1000.0F) / 10.0F;
      return new StaticMessage(status.current + "/" + status.max + " " + percRounded + "%");
   }

   public Color getBufferColor() {
      return new Color(181, 80, 21, 150);
   }

   public Color getFillColor() {
      return new Color(201, 24, 24);
   }

   public enum BarCategory {
      boss,
      incursion;
   }

   public static class StatusAtTime {
      public final int current;
      public final int max;
      public final long time;

      public StatusAtTime(int current, int max, long time) {
         this.current = current;
         this.max = max;
         this.time = time;
      }

      public float getPercent() {
         return GameMath.limit((float)this.current / this.max, 0.0F, 1.0F);
      }
   }
}
