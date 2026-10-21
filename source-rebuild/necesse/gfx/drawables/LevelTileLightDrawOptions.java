package necesse.gfx.drawables;

import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceWrapper;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class LevelTileLightDrawOptions {
   protected TileLightDrawList list = new TileLightDrawList();

   public void addLight(TickManager tickManager, Level level, int tileX, int tileY, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      if (Settings.smoothLighting) {
         this.list.add(drawX + 16, drawY + 16, getSmoothLight(tickManager, level, tileX, tileY));
      } else {
         this.list.add(drawX, drawY, getTiledLight(tickManager, level, tileX, tileY));
      }
   }

   public static float[] getTiledLight(TickManager tickManager, Level level, int x, int y) {
      PerformanceWrapper timer = Performance.wrapTimer(tickManager, "getLight");

      try {
         GameLight l1 = level.getLightLevel(x, y);
         return l1.getAdvColor();
      } finally {
         timer.end();
      }
   }

   public static float[] getSmoothLight(TickManager tickManager, Level level, int x, int y) {
      PerformanceWrapper timer = Performance.wrapTimer(tickManager, "getLight");

      try {
         GameLight l1 = level.getLightLevel(x, y);
         GameLight l2 = level.getLightLevel(x + 1, y);
         GameLight l3 = level.getLightLevel(x + 1, y + 1);
         GameLight l4 = level.getLightLevel(x, y + 1);
         return l1.getAdvColor(l2, l3, l4, 1.0F);
      } finally {
         timer.end();
      }
   }

   public void draw() {
      this.list.draw();
   }
}
