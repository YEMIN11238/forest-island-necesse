package necesse.engine.gameLoop;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.window.GameWindow;

public interface GameLoopListener {
   void frameTick(TickManager var1, GameWindow var2);

   void drawTick(TickManager var1);

   boolean isDisposed();
}
