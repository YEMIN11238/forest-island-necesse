package necesse.gfx.camera;

import java.awt.geom.Point2D.Float;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;

public class MainGameFollowCamera extends MainGameCamera {
   @Override
   public void tickCamera(TickManager tickManager, MainGame mainGame, Client client) {
      PlayerMob player = client.getPlayer();
      if (player != null) {
         this.centerCamera(player.getDrawX(), player.getDrawY());
         if (mainGame.isRunning() && player.getSelectedItem() != null) {
            float zoomAmount = player.getSelectedItem().item.zoomAmount();
            if (zoomAmount != 0.0F) {
               float xDir;
               float yDir;
               if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
                  xDir = ControllerInput.getAimX();
                  yDir = ControllerInput.getAimY();
               } else {
                  GameWindow window = WindowManager.getWindow();
                  xDir = (float)window.mousePos().sceneX / window.getSceneWidth() * 2.0F - 1.0F;
                  yDir = (float)window.mousePos().sceneY / window.getSceneHeight() * 2.0F - 1.0F;
               }

               this.setPosition(this.getX() + (int)(xDir * zoomAmount), this.getY() + (int)(yDir * zoomAmount));
            }
         }
      }

      Float cameraShake = client.getCurrentCameraShake();
      this.setPosition(this.getX() + (int)cameraShake.x, this.getY() + (int)cameraShake.y);
   }
}
