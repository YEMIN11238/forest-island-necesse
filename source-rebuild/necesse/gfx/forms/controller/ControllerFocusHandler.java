package necesse.gfx.forms.controller;

import java.awt.Point;
import java.awt.Rectangle;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.controller.ControllerEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.ui.HUD;

public interface ControllerFocusHandler {
   void handleControllerEvent(ControllerEvent var1, TickManager var2, PlayerMob var3);

   boolean handleControllerNavigate(int var1, ControllerEvent var2, TickManager var3, PlayerMob var4);

   default void onControllerFocused(ControllerFocus current) {
   }

   default void onControllerUnfocused(ControllerFocus current) {
   }

   default void frameTickControllerFocus(TickManager tickManager, ControllerFocus current) {
   }

   default void drawControllerFocus(ControllerFocus current) {
      Rectangle box = current.boundingBox;
      int padding = 5;
      box = new Rectangle(box.x - padding, box.y - padding, box.width + padding * 2, box.height + padding * 2);
      HUD.selectBoundOptions(Settings.UI.controllerFocusBoundsColor, true, box).draw();
   }

   default Point getControllerTooltipAndFloatMenuPoint(ControllerFocus current) {
      return null;
   }

   default int getControllerFocusHashcode() {
      return this.hashCode();
   }
}
