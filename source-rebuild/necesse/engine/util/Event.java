package necesse.engine.util;

import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Event<T> {
   public final boolean autoClean;
   private final LinkedList<Event<T>.Listener> changedEvents = new LinkedList<>();

   public Event(boolean autoClean) {
      this.autoClean = autoClean;
   }

   public Event() {
      this(true);
   }

   public Event<T>.Listener addListener(Consumer<T> listener, Supplier<Boolean> isDisposed) {
      if (this.autoClean) {
         this.cleanListeners();
      }

      Event<T>.Listener out;
      this.changedEvents.add(out = new Event.Listener(listener, isDisposed));
      return out;
   }

   public void cleanListeners() {
      this.changedEvents.removeIf(l -> l.isDisposed.get());
   }

   public int getTotalChangeListeners() {
      if (this.autoClean) {
         this.cleanListeners();
      }

      return this.changedEvents.size();
   }

   public void invoke(T var) {
      if (this.autoClean) {
         this.cleanListeners();
      }

      this.changedEvents.forEach(e -> e.onChange.accept(var));
   }

   public class Listener {
      private final Consumer<T> onChange;
      private final Supplier<Boolean> isDisposed;

      private Listener(Consumer<T> onChange, Supplier<Boolean> isDisposed) {
         this.onChange = onChange;
         this.isDisposed = isDisposed;
      }

      public void dispose() {
         Event.this.changedEvents.remove(this);
      }
   }
}
