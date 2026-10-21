package necesse.gfx.drawables;

import necesse.engine.gameLoop.tickManager.TickManager;

@FunctionalInterface
public interface Drawable {
   void draw(TickManager var1);
}
