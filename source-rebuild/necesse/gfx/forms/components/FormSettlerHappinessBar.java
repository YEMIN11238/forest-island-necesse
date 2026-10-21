package necesse.gfx.forms.components;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameBackground;
import necesse.gfx.GameBackgroundPaddingWrapper;
import necesse.gfx.GameBackgroundWrapper;
import necesse.gfx.GameColor;
import necesse.gfx.drawOptions.ProgressBarDrawOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;

public abstract class FormSettlerHappinessBar extends FormProgressBar {
   public FormSettlerHappinessBar(int x, int y, int width, boolean addPaddingToWidth) {
      super(x, y, width, addPaddingToWidth);
   }

   @Override
   public void modifyDrawOptions(ProgressBarDrawOptions drawOptions) {
      super.modifyDrawOptions(drawOptions);
      drawOptions.addIndicator(HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD / 100.0F);
      drawOptions.addIndicator(HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD / 100.0F);
   }

   @Override
   public float getProgress() {
      return GameMath.limit(this.getHappinessLevel() / 100.0F, 0.0F, 1.0F);
   }

   public abstract int getHappinessLevel();

   @Override
   public GameTooltips getTooltips(float progress) {
      ListGameTooltips tooltips = new ListGameTooltips();
      String settlerName = this.getSettlerName();
      int decreaseThreshold = HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD;
      int increaseThreshold = HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD;
      if (settlerName != null) {
         int happinessLevel = this.getHappinessLevel();
         String nameReplacement = GameColor.ITEM_QUEST.getColorCode() + settlerName + GameColor.NO_COLOR.getColorCode();
         GameMessage tooltip;
         if (happinessLevel < decreaseThreshold) {
            tooltip = new LocalMessage("settlement", "qoldecrease", "name", nameReplacement);
         } else if (happinessLevel >= increaseThreshold) {
            tooltip = new LocalMessage("settlement", "qolincrease", "name", nameReplacement);
         } else {
            tooltip = new LocalMessage("settlement", "qolstay", "name", nameReplacement);
         }

         tooltips.add(tooltip, 400);
         tooltips.add(new SpacerGameTooltip(10));
      }

      tooltips.add(new LocalMessage("settlement", "qolchangetip1", "value", decreaseThreshold + "%"), 400);
      tooltips.add(new SpacerGameTooltip(10));
      tooltips.add(new LocalMessage("settlement", "qolchangetip2", "value", increaseThreshold + "%"), 400);
      tooltips.add(new SpacerGameTooltip(10));
      tooltips.add(new LocalMessage("settlement", "qolchangetip3", "value", increaseThreshold - decreaseThreshold + "%"), 400);
      return tooltips;
   }

   @Override
   public GameBackground getTooltipBackground() {
      return new GameBackgroundPaddingWrapper(GameBackgroundWrapper.itemTooltip, 2);
   }

   public abstract String getSettlerName();
}
