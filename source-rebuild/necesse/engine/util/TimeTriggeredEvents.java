package necesse.engine.util;

public class TimeTriggeredEvents {
   protected GameLinkedList<TimeTriggeredEvents.Event> events = new GameLinkedList<>();

   public TimeTriggeredEvents addEvent(int time, Runnable action) {
      TimeTriggeredEvents.Event e = new TimeTriggeredEvents.Event(time, action);

      for (GameLinkedList<TimeTriggeredEvents.Event>.Element el : this.events.elements()) {
         if (((TimeTriggeredEvents.Event)el.object).time > time) {
            el.insertBefore(e);
            return this;
         }
      }

      this.events.addLast(e);
      return this;
   }

   public boolean tickEvents(long timeSinceStart) {
      while (!this.events.isEmpty()) {
         GameLinkedList<TimeTriggeredEvents.Event>.Element firstEl = this.events.getFirstElement();
         if (timeSinceStart >= ((TimeTriggeredEvents.Event)firstEl.object).time) {
            ((TimeTriggeredEvents.Event)firstEl.object).action.run();
            firstEl.remove();
            continue;
         }
         break;
      }

      return !this.events.isEmpty();
   }

   protected class Event {
      public final int time;
      public final Runnable action;

      public Event(int time, Runnable action) {
         this.time = time;
         this.action = action;
      }
   }
}
