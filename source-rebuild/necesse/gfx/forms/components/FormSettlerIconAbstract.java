package necesse.gfx.forms.components;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.PlayerSprite;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public abstract class FormSettlerIconAbstract extends FormButton implements FormPositionContainer {
   private FormPosition position;

   public FormSettlerIconAbstract(int x, int y) {
      this.position = new FormFixedPosition(x, y);
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      PlayerSprite.drawInForms((drawX, drawY) -> this.getFaceDrawOptions(32, drawX, drawY).draw(), this.getX(), this.getY());
      if (this.isHovering()) {
         GameTooltips tooltips = this.getTooltips();
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   public abstract DrawOptions getFaceDrawOptions(int var1, int var2, int var3);

   public abstract GameTooltips getTooltips();

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), 32, 32));
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
