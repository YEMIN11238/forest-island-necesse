package necesse.engine;

import necesse.engine.util.GameLinkedList;

public class GameEventsHandler<T> {
   private GameLinkedList<GameEventInterface<T>> listeners = new GameLinkedList<>();
   private boolean autoClean;

   public GameEventsHandler(boolean autoClean) {
      this.autoClean = autoClean;
   }

   public <R extends GameEventInterface<T>> R addListener(R listener) {
      if (this.autoClean) {
         this.cleanListeners();
      }

      GameLinkedList<GameEventInterface<T>>.Element e = this.listeners.addLast(listener);
      listener.init(() -> {
         if (!e.isRemoved()) {
            e.remove();
         }
      });
      return listener;
   }

   public void triggerEvent(T event) {
      for (GameLinkedList<GameEventInterface<T>>.Element e : this.listeners.elements()) {
         if (((GameEventInterface)e.object).isDisposed()) {
            e.remove();
         } else {
            ((GameEventInterface)e.object).onEvent(event);
         }
      }
   }

   public void cleanListeners() {
      for (GameLinkedList<GameEventInterface<T>>.Element e : this.listeners.elements()) {
         if (((GameEventInterface)e.object).isDisposed()) {
            e.remove();
         }
      }
   }

   public int getListenerCount() {
      return this.listeners.size();
   }
}
