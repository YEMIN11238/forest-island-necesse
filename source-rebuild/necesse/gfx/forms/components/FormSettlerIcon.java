package necesse.gfx.forms.components;

import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.PlayerSprite;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class FormSettlerIcon extends FormButton implements FormPositionContainer {
   private FormPosition position;
   private final Settler settler;
   private final Mob mob;
   public Supplier<GameTooltips> extraTooltips;

   public FormSettlerIcon(int x, int y, Settler settler, Mob mob) {
      this.position = new FormFixedPosition(x, y);
      this.settler = settler;
      this.mob = mob;
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      PlayerSprite.drawInForms((drawX, drawY) -> this.settler.getSettlerFaceDrawOptions(drawX, drawY, 32, this.mob).draw(), this.getX(), this.getY());
      if (this.isHovering()) {
         GameTooltips tooltips = this.getTooltips();
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   public GameTooltips getTooltips() {
      ListGameTooltips tooltips = new ListGameTooltips(this.settler.getGenericMobName());
      if (this.extraTooltips != null) {
         GameTooltips extra = this.extraTooltips.get();
         if (extra != null) {
            tooltips.add(extra);
         }
      }

      return tooltips;
   }

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
