package necesse.gfx.forms.components;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.drawOptions.ProgressBarDrawOptions;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public abstract class FormProgressBar extends FormComponent implements FormPositionContainer {
   public FormPosition position;
   public int width;
   public boolean addPaddingToWidth;
   private boolean isHovering;

   public FormProgressBar(int x, int y, int width, boolean addPaddingToWidth) {
      this.position = new FormFixedPosition(x, y);
      this.width = width;
      this.addPaddingToWidth = addPaddingToWidth;
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.isHovering = this.isMouseOver(event);
         if (this.isHovering) {
            event.useMove();
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      float progress = GameMath.limit(this.getProgress(), 0.0F, 1.0F);
      ProgressBarDrawOptions drawOptions = new ProgressBarDrawOptions(this.getInterfaceStyle().progressbar, this.getFullWidth());
      ProgressBarDrawOptions.ProgressBarOptions bar = drawOptions.addBar(progress);
      this.modifyBar(bar, progress);
      bar.end();
      this.modifyDrawOptions(drawOptions);
      drawOptions.draw(this.getX() - (this.addPaddingToWidth ? this.getHorizontalPadding() : 0), this.getY());
      if (this.isHovering()) {
         GameTooltips tooltips = this.getTooltips(progress);
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, this.getTooltipBackground(), TooltipLocation.FORM_FOCUS);
         }
      }
   }

   public void modifyBar(ProgressBarDrawOptions.ProgressBarOptions barOptions, float progress) {
   }

   public void modifyDrawOptions(ProgressBarDrawOptions drawOptions) {
   }

   public GameTooltips getTooltips(float progress) {
      return new StringTooltips((int)(progress * 100.0F) + " %");
   }

   public GameBackground getTooltipBackground() {
      return null;
   }

   public int getFullHeight() {
      return this.getInterfaceStyle().progressbar.background.getHeight();
   }

   public int getFillHeight() {
      return this.getInterfaceStyle().progressbar.fill.getHeight();
   }

   public int getHorizontalPadding() {
      return this.getInterfaceStyle().progressbar.horizontalPadding;
   }

   public int getFullWidth() {
      return this.width + (this.addPaddingToWidth ? this.getInterfaceStyle().progressbar.horizontalPadding * 2 : 0);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(
         new Rectangle(this.getX() - (this.addPaddingToWidth ? this.getHorizontalPadding() : 0), this.getY(), this.getFullWidth(), this.getFullHeight())
      );
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   protected boolean isHovering() {
      return this.isHovering || this.isControllerFocus();
   }

   public abstract float getProgress();
}
