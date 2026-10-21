package necesse.gfx.forms.components;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FormMouseHover extends FormButton implements FormPositionContainer {
   private FormPosition position;
   public int width;
   public int height;
   public boolean alwaysAddControllerFocus;

   public FormMouseHover(int x, int y, int width, int height, boolean useMoveEvents, boolean alwaysAddControllerFocus) {
      this.position = new FormFixedPosition(x, y);
      this.width = width;
      this.height = height;
      this.useHoverMoveEvents = useMoveEvents;
      this.alwaysAddControllerFocus = alwaysAddControllerFocus;
      this.handleClicksIfNoEventHandlers = alwaysAddControllerFocus;
   }

   public FormMouseHover(int x, int y, int width, int height, boolean useMoveEvents) {
      this(x, y, width, height, useMoveEvents, false);
   }

   public FormMouseHover(int x, int y, int width, int height) {
      this(x, y, width, height, false);
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.isHovering()) {
         GameTooltips tooltips = this.getTooltips(perspective);
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }

         GameWindow.CURSOR hoveringCursor = this.getHoveringCursor(perspective);
         if (hoveringCursor != null) {
            Renderer.setCursor(hoveringCursor);
         }
      }
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      if (this.clickedEvents.hasListeners() || this.alwaysAddControllerFocus) {
         ControllerFocus.add(
            list, area, this, this.getBoundingBox(), currentXOffset, currentYOffset, this.controllerInitialFocusPriority, customNavigationHandler
         );
      }
   }

   public GameTooltips getTooltips(PlayerMob perspective) {
      return null;
   }

   public GameWindow.CURSOR getHoveringCursor(PlayerMob perspective) {
      return null;
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), this.width, this.height));
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }
}
