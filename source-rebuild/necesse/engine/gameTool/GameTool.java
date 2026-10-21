package necesse.engine.gameTool;

import necesse.engine.GlobalData;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.gameTooltips.GameTooltips;

public interface GameTool {
   default void init() {
   }

   default void tick() {
   }

   boolean inputEvent(InputEvent var1);

   default boolean controllerEvent(ControllerEvent event) {
      return false;
   }

   default void onPaused() {
   }

   default void onRenewed() {
   }

   default boolean canCancel() {
      return true;
   }

   default boolean forceControllerCursor() {
      return this.canCancel();
   }

   default boolean startControllerCursor() {
      return this.forceControllerCursor();
   }

   default boolean shouldForceControllerMenuLayer() {
      return true;
   }

   default boolean shouldShowWires() {
      return false;
   }

   void isCancelled();

   void isCleared();

   GameTooltips getTooltips();

   default GameWindow.CURSOR getCursor() {
      return null;
   }

   default int getMouseX() {
      return WindowManager.getWindow().mousePos().sceneX + GlobalData.getCurrentState().getCamera().getX();
   }

   default int getMouseTileX() {
      return GameMath.getTileCoordinate(this.getMouseX());
   }

   default int getMouseY() {
      return WindowManager.getWindow().mousePos().sceneY + GlobalData.getCurrentState().getCamera().getY();
   }

   default int getMouseTileY() {
      return GameMath.getTileCoordinate(this.getMouseY());
   }
}
