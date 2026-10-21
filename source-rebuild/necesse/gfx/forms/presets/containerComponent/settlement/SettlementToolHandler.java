package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.Consumer;
import necesse.engine.window.GameWindow;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;

public interface SettlementToolHandler {
   default boolean onLeftClick(Point pos) {
      return false;
   }

   default DrawOptions getLeftClickDraw(Point startPos, Point endPos) {
      return null;
   }

   default boolean onLeftClickSelection(Point startPos, Point endPos, Rectangle selection) {
      return false;
   }

   default DrawOptions getLeftClickSelectionDraw(Point startPos, Point endPos, Rectangle selection) {
      return null;
   }

   default boolean onRightClick(Point pos) {
      return false;
   }

   default DrawOptions getRightClickDraw(Point startPos, Point endPos) {
      return null;
   }

   default boolean onRightClickSelection(Point startPos, Point endPos, Rectangle selection) {
      return false;
   }

   default DrawOptions getRightClickSelectionDraw(Point startPos, Point endPos, Rectangle selection) {
      return null;
   }

   default boolean onHover(Point pos, Consumer<ListGameTooltips> setTooltips, Consumer<GameWindow.CURSOR> setCursor) {
      return false;
   }
}
