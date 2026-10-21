package necesse.engine.gameLoop;

import java.util.LinkedList;
import java.util.ListIterator;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.window.GameWindow;

public abstract class GameLoop extends TickManager {
   private boolean stopMainGameLoop;
   private LinkedList<GameLoopListener> gameLoopListeners = new LinkedList<>();

   public GameLoop(String name, int maxFPS) {
      super(name, maxFPS);
   }

   public void runMainGameLoop() {
      while (!this.stopMainGameLoop) {
         this.tickLogic();
      }
   }

   public void stopMainGameLoop() {
      this.stopMainGameLoop = true;
   }

   public void addGameLoopListener(GameLoopListener listener) {
      this.gameLoopListeners.add(listener);
   }

   public void runGameLoopListenersFrameTick(GameWindow window) {
      ListIterator<GameLoopListener> iterator = this.gameLoopListeners.listIterator();

      while (iterator.hasNext()) {
         GameLoopListener next = iterator.next();
         if (next.isDisposed()) {
            iterator.remove();
         } else {
            next.frameTick(this, window);
         }
      }
   }

   public void runGameLoopListenersDrawTick() {
      ListIterator<GameLoopListener> iterator = this.gameLoopListeners.listIterator();

      while (iterator.hasNext()) {
         GameLoopListener next = iterator.next();
         if (next.isDisposed()) {
            iterator.remove();
         } else {
            next.drawTick(this);
         }
      }
   }
}
