package necesse.gfx.forms.components;

import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.gfx.GameBackground;
import necesse.gfx.GameBackgroundPaddingWrapper;
import necesse.gfx.GameBackgroundWrapper;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;

public abstract class FormSettlerRecreationBar extends FormProgressBar {
   public FormSettlerRecreationBar(int x, int y, int width, boolean addPaddingToWidth) {
      super(x, y, width, addPaddingToWidth);
   }

   @Override
   public float getProgress() {
      return GameMath.limit(this.getRecreationLevel(), 0.0F, 1.0F);
   }

   public abstract float getRecreationLevel();

   @Override
   public GameTooltips getTooltips(float progress) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(new LocalMessage("ui", "settlerrecreationbartip1"), 400);
      tooltips.add(new SpacerGameTooltip(10));
      tooltips.add(new LocalMessage("ui", "settlerrecreationbartip2"), 400);
      return tooltips;
   }

   @Override
   public GameBackground getTooltipBackground() {
      return new GameBackgroundPaddingWrapper(GameBackgroundWrapper.itemTooltip, 2);
   }
}
