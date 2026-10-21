package necesse.inventory.item;

import java.awt.Color;
import java.util.function.DoubleFunction;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.util.GameMath;
import necesse.gfx.GameColor;

public abstract class DoubleItemStatTip extends ItemStatTip {
   protected double value;
   protected double compareValue;
   protected int decimals;
   protected DoubleFunction<String> valueToString = GameMath::removeDecimalIfZero;
   protected DoubleFunction<String> deltaToString = delta -> delta > 0.0 ? "+" + GameMath.removeDecimalIfZero(delta) : GameMath.removeDecimalIfZero(delta);
   protected boolean addParenthesisToChange = true;
   protected boolean higherIsBetter = true;

   public DoubleItemStatTip(double value, int decimals) {
      this.value = value;
      this.decimals = decimals;
      this.compareValue = Double.NaN;
   }

   public DoubleItemStatTip setToString(DoubleFunction<String> valueToString) {
      this.valueToString = valueToString;
      this.deltaToString = delta -> delta > 0.0 ? "+" + valueToString.apply(delta) : valueToString.apply(delta);
      return this;
   }

   public DoubleItemStatTip setValueToString(DoubleFunction<String> valueToString) {
      this.valueToString = valueToString;
      return this;
   }

   public DoubleItemStatTip setDeltaToString(DoubleFunction<String> valueToString) {
      this.deltaToString = valueToString;
      return this;
   }

   public DoubleItemStatTip addParenthesisToChange(boolean value) {
      this.addParenthesisToChange = value;
      return this;
   }

   public DoubleItemStatTip setCompareValue(double compareValue, boolean higherIsBetter) {
      this.compareValue = compareValue;
      this.higherIsBetter = higherIsBetter;
      return this;
   }

   public DoubleItemStatTip setCompareValue(double compareValue) {
      return this.setCompareValue(compareValue, true);
   }

   protected String getReplaceValue(Color betterColor, Color worseColor, boolean showDifference) {
      double value = this.decimals <= 0 ? Math.round(this.value) : GameMath.toDecimals(this.value, this.decimals);
      String replaceString = this.valueToString.apply(value);
      if (!Double.isNaN(this.compareValue) && this.value != this.compareValue) {
         double delta = this.decimals <= 0 ? Math.round(this.value - this.compareValue) : GameMath.toDecimals(this.value - this.compareValue, this.decimals);
         boolean isBetter = this.higherIsBetter ? delta > 0.0 : delta < 0.0;
         if (showDifference) {
            String append = this.deltaToString.apply(delta);
            if (this.addParenthesisToChange) {
               append = "(" + append + ")";
            }

            if (isBetter) {
               if (betterColor != null) {
                  append = GameColor.getCustomColorCode(betterColor) + append + GameColor.NO_COLOR.getColorCode();
               }
            } else if (worseColor != null) {
               append = GameColor.getCustomColorCode(worseColor) + append + GameColor.NO_COLOR.getColorCode();
            }

            replaceString = replaceString + " " + append;
         } else if (isBetter) {
            if (betterColor != null) {
               replaceString = GameColor.getCustomColorCode(betterColor) + replaceString + GameColor.NO_COLOR.getColorCode();
            }
         } else if (worseColor != null) {
            replaceString = GameColor.getCustomColorCode(worseColor) + replaceString + GameColor.NO_COLOR.getColorCode();
         }
      }

      return replaceString;
   }

   protected GameMessage colorMessage(Color betterColor, Color worseColor, GameMessage message) {
      if (!Double.isNaN(this.compareValue) && this.value != this.compareValue) {
         double delta = this.decimals <= 0 ? Math.round(this.value - this.compareValue) : GameMath.toDecimals(this.value - this.compareValue, this.decimals);
         boolean isBetter = this.higherIsBetter ? delta > 0.0 : delta < 0.0;
         if (isBetter) {
            if (betterColor != null) {
               return new GameMessageBuilder().append(GameColor.getCustomColorCode(betterColor)).append(message).append(GameColor.NO_COLOR.getColorCode());
            }
         } else if (worseColor != null) {
            return new GameMessageBuilder().append(GameColor.getCustomColorCode(worseColor)).append(message).append(GameColor.NO_COLOR.getColorCode());
         }
      }

      return message;
   }
}
