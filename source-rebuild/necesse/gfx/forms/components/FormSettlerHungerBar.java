package necesse.gfx.forms.components;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.gfx.GameBackground;
import necesse.gfx.GameBackgroundPaddingWrapper;
import necesse.gfx.GameBackgroundWrapper;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;

public abstract class FormSettlerHungerBar extends FormProgressBar {
   public FormSettlerHungerBar(int x, int y, int width, boolean addPaddingToWidth) {
      super(x, y, width, addPaddingToWidth);
   }

   @Override
   public float getProgress() {
      return GameMath.limit(this.getHungerLevel(), 0.0F, 1.0F);
   }

   public abstract float getHungerLevel();

   @Override
   public GameTooltips getTooltips(float progress) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(new LocalMessage("ui", "settlerhungerbartip1"), 400);
      tooltips.add(new SpacerGameTooltip(10));
      tooltips.add(new LocalMessage("ui", "settlerhungerbartip2"), 400);
      return tooltips;
   }

   @Override
   public GameBackground getTooltipBackground() {
      return new GameBackgroundPaddingWrapper(GameBackgroundWrapper.itemTooltip, 2);
   }
}
