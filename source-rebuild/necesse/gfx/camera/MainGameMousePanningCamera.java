package necesse.gfx.camera;

import java.awt.geom.Point2D.Float;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;

public class MainGameMousePanningCamera extends MainGamePanningCamera {
   private final float speedModifier;

   public MainGameMousePanningCamera(int x, int y, float speedModifier) {
      super(x, y);
      this.speedModifier = speedModifier;
   }

   @Override
   public void tickCamera(TickManager tickManager, MainGame mainGame, Client client) {
      if (Settings.hideUI || !mainGame.formManager.isMouseOver()) {
         GameWindow window = WindowManager.getWindow();
         float fMouseX = (float)window.mousePos().sceneX / window.getSceneWidth() * 2.0F - 1.0F;
         float fMouseY = (float)window.mousePos().sceneY / window.getSceneHeight() * 2.0F - 1.0F;
         this.setSpeed((float)(new Float(fMouseX, fMouseY).distance(0.0, 0.0) * this.speedModifier));
         this.setDirection(fMouseX, fMouseY);
      }

      super.tickCamera(tickManager, mainGame, client);
   }
}
