package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class FormVarToggleIconButton extends FormButton implements FormPositionContainer {
   private FormPosition position;
   private int width;
   private int height;
   private GameMessage[] tooltips;
   private GameSprite onSprite;
   private GameSprite offSprite;
   private Supplier<Boolean> varGetter;

   public FormVarToggleIconButton(
      int x, int y, GameSprite onSprite, GameSprite offSprite, Consumer<Boolean> varSetter, Supplier<Boolean> varGetter, GameMessage... tooltips
   ) {
      this(x, y, onSprite, offSprite, varSetter, varGetter, onSprite.width, onSprite.height, tooltips);
   }

   public FormVarToggleIconButton(
      int x,
      int y,
      GameSprite onSprite,
      GameSprite offSprite,
      Consumer<Boolean> varSetter,
      Supplier<Boolean> varGetter,
      int width,
      int height,
      GameMessage... tooltips
   ) {
      this.position = new FormFixedPosition(x, y);
      this.width = width;
      this.height = height;
      this.onSprite = onSprite;
      this.offSprite = offSprite;
      this.varGetter = varGetter;
      this.tooltips = tooltips;
      this.onClicked(e -> varSetter.accept(!varGetter.get()));
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      Color drawCol = this.getDrawColor();
      if (this.varGetter.get()) {
         this.onSprite.initDraw().color(drawCol).draw(this.getX(), this.getY());
      } else {
         this.offSprite.initDraw().color(drawCol).draw(this.getX(), this.getY());
      }

      if (this.isHovering()) {
         GameTooltips tooltips = this.getTooltips();
         if (tooltips != null) {
            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }
      }
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), this.width, this.height));
   }

   public GameTooltips getTooltips() {
      if (this.tooltips.length == 0) {
         return null;
      }

      StringTooltips out = new StringTooltips();

      for (GameMessage tooltip : this.tooltips) {
         out.add(tooltip.translate());
      }

      return out;
   }

   public void setTooltips(GameMessage... tooltips) {
      this.tooltips = tooltips;
   }

   public void setIconSprite(GameSprite onSprite, GameSprite offSprite) {
      this.onSprite = onSprite;
      this.offSprite = offSprite;
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
