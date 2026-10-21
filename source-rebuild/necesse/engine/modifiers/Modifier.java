package necesse.engine.modifiers;

import java.awt.Color;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.gfx.GameColor;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.LocalMessageDoubleItemStatTip;

public class Modifier<T> {
   public static final ModifierAppendFunction<Float> FLOAT_ADD_APPEND = (oldValue, newValue, count) -> oldValue + newValue * count;
   public static final ModifierAppendFunction<Integer> INT_ADD_APPEND = (oldValue, newValue, count) -> oldValue + newValue * count;
   public static final ModifierAppendFunction<Float> FLOAT_MULT_APPEND = (oldValue, newValue, count) -> oldValue
      * (count == 1 ? newValue : (float)Math.pow(newValue.floatValue(), count));
   public static final ModifierAppendFunction<Boolean> OR_APPEND = (oldValue, newValue, count) -> oldValue || newValue;
   public static final BiFunction<Float, Float, Float> FLOAT_ADD_APPEND_DEPRICATED = (oldValue, newValue) -> oldValue + newValue;
   public static final BiFunction<Integer, Integer, Integer> INT_ADD_APPEND_DEPRICATED = (oldValue, newValue) -> oldValue + newValue;
   public static final BiFunction<Float, Float, Float> FLOAT_MULT_APPEND_DEPRICATED = (oldValue, newValue) -> oldValue * newValue;
   public static final BiFunction<Boolean, Boolean, Boolean> OR_APPEND_DEPRICATED = (oldValue, newValue) -> oldValue || newValue;
   public final int index;
   public final ModifierList list;
   public final String stringID;
   public final T defaultBuffManagerValue;
   public final T defaultBuffValue;
   private ModifierAppendFunction<T> appendFunc;
   private Function<T, T> finalLimiter;
   private Modifier.ModifierTooltipGetter<T> tooltipFunc;
   public final ModifierLimiter<T> limiter;

   public static Modifier.ModifierTooltipGetter<Float> NORMAL_PERC_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         float v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", Math.round(v * 100.0F), 0);
         tip.setValueToString(value -> value >= 0.0 ? "+" + GameMath.removeDecimalIfZero(value) : GameMath.removeDecimalIfZero(value));
         if (lastValue != null) {
            tip.setCompareValue(Math.round((lastValue - defaultValue) * 100.0F));
         }

         return new ModifierTooltip((int)Math.signum(v), tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Float> INVERSE_PERC_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         float v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", Math.round(v * 100.0F), 0);
         tip.setValueToString(value -> value >= 0.0 ? "+" + GameMath.removeDecimalIfZero(value) : GameMath.removeDecimalIfZero(value));
         if (lastValue != null) {
            tip.setCompareValue(Math.round((lastValue - defaultValue) * 100.0F), false);
         }

         return new ModifierTooltip((int)Math.signum(-v), tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Float> NORMAL_FLAT_FLOAT_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         float v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", v, 2);
         tip.setValueToString(value -> value >= 0.0 ? "+" + GameMath.removeDecimalIfZero(value) : GameMath.removeDecimalIfZero(value));
         if (lastValue != null) {
            tip.setCompareValue(lastValue - defaultValue);
         }

         return new ModifierTooltip((int)Math.signum(v), tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Float> INVERSE_FLAT_FLOAT_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         float v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", v, 2);
         tip.setValueToString(value -> value >= 0.0 ? "+" + GameMath.removeDecimalIfZero(value) : GameMath.removeDecimalIfZero(value));
         if (lastValue != null) {
            tip.setCompareValue(lastValue - defaultValue, false);
         }

         return new ModifierTooltip((int)Math.signum(-v), tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Integer> NORMAL_FLAT_INT_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         int v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", v, 0);
         tip.setValueToString(value -> value >= 0.0 ? "+" + GameMath.removeDecimalIfZero(value) : GameMath.removeDecimalIfZero(value));
         if (lastValue != null) {
            tip.setCompareValue(lastValue - defaultValue);
         }

         return new ModifierTooltip((int)Math.signum(v), tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Integer> INVERSE_FLAT_INT_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         int v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", v, 0);
         tip.setValueToString(value -> value >= 0.0 ? "+" + GameMath.removeDecimalIfZero(value) : GameMath.removeDecimalIfZero(value));
         if (lastValue != null) {
            tip.setCompareValue(lastValue - defaultValue, false);
         }

         return new ModifierTooltip((int)Math.signum(-v), tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Boolean> BOOL_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> currentValue ? new ModifierTooltip(1, new ItemStatTip() {
         @Override
         public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
            LocalMessage message = new LocalMessage("buffmodifiers", localisationKey);
            return lastValue != null && !lastValue ? new GameMessageBuilder().append(GameColor.getCustomColorCode(betterColor)).append(message) : message;
         }
      }) : null;
   }

   public static Modifier.ModifierTooltipGetter<Boolean> INVERSE_BOOL_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> currentValue ? new ModifierTooltip(-1, new ItemStatTip() {
         @Override
         public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
            LocalMessage message = new LocalMessage("buffmodifiers", localisationKey);
            return lastValue != null && lastValue ? new GameMessageBuilder().append(GameColor.getCustomColorCode(worseColor)).append(message) : message;
         }
      }) : null;
   }

   public static Modifier.ModifierTooltipGetter<Boolean> NEUTRAL_BOOL_PARSER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> currentValue ? new ModifierTooltip(0, new ItemStatTip() {
         @Override
         public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
            return new LocalMessage("buffmodifiers", localisationKey);
         }
      }) : null;
   }

   public static Modifier.ModifierTooltipGetter<Float> BAD_PERCENT_MODIFIER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         float v = currentValue - defaultValue;
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", Math.round(v * 100.0F), 0);
         if (lastValue != null) {
            tip.setCompareValue(Math.round((lastValue - defaultValue) * 100.0F), false);
         }

         return new ModifierTooltip(-1, tip);
      };
   }

   public static Modifier.ModifierTooltipGetter<Float> LESS_GOOD_PERCENT_MODIFIER(String localisationKey) {
      return (currentValue, lastValue, defaultValue) -> {
         LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("buffmodifiers", localisationKey, "mod", Math.round(currentValue * 100.0F), 0);
         if (lastValue != null) {
            tip.setCompareValue(Math.round(lastValue * 100.0F), false);
         }

         return new ModifierTooltip(currentValue < 1.0F ? 1 : -1, tip);
      };
   }

   public Modifier(
      ModifierList list,
      String stringID,
      T defaultBuffManagerValue,
      T defaultBuffValue,
      ModifierAppendFunction<T> appendFunc,
      Function<T, T> finalLimiter,
      Modifier.ModifierTooltipGetter<T> tooltipFunc,
      ModifierLimiter<T> limiter
   ) {
      Objects.requireNonNull(appendFunc);
      this.stringID = stringID;
      this.list = list;
      this.defaultBuffManagerValue = defaultBuffManagerValue;
      this.defaultBuffValue = defaultBuffValue;
      this.appendFunc = appendFunc;
      this.finalLimiter = finalLimiter;
      this.tooltipFunc = tooltipFunc;
      this.limiter = limiter;
      this.index = list.addModifier(this);
   }

   @Deprecated
   public Modifier(
      ModifierList list,
      T defaultBuffManagerValue,
      T defaultBuffValue,
      ModifierAppendFunction<T> appendFunc,
      Function<T, T> finalLimiter,
      Modifier.ModifierTooltipGetter<T> tooltipFunc,
      ModifierLimiter<T> limiter
   ) {
      this(list, null, defaultBuffManagerValue, defaultBuffValue, appendFunc, finalLimiter, tooltipFunc, limiter);
   }

   public Modifier(
      ModifierList list,
      String stringID,
      T defaultBuffManagerValue,
      T defaultBuffValue,
      ModifierAppendFunction<T> appendFunc,
      Modifier.ModifierTooltipGetter<T> tooltipFunc,
      ModifierLimiter<T> limiter
   ) {
      this(list, stringID, defaultBuffManagerValue, defaultBuffValue, appendFunc, null, tooltipFunc, limiter);
   }

   @Deprecated
   public Modifier(
      ModifierList list,
      T defaultBuffManagerValue,
      T defaultBuffValue,
      ModifierAppendFunction<T> appendFunc,
      Modifier.ModifierTooltipGetter<T> tooltipFunc,
      ModifierLimiter<T> limiter
   ) {
      this(list, null, defaultBuffManagerValue, defaultBuffValue, appendFunc, null, tooltipFunc, limiter);
   }

   @Deprecated
   public Modifier(
      ModifierList list,
      T defaultBuffManagerValue,
      T defaultBuffValue,
      BiFunction<T, T, T> appendFunc,
      Function<T, T> finalLimiter,
      Modifier.ModifierTooltipGetter<T> tooltipFunc,
      ModifierLimiter<T> limiter
   ) {
      this(
         list,
         null,
         defaultBuffManagerValue,
         defaultBuffValue,
         (currentValue, appendValue, count) -> appendFunc.apply(currentValue, appendValue),
         finalLimiter,
         tooltipFunc,
         limiter
      );
   }

   @Deprecated
   public Modifier(
      ModifierList list,
      T defaultBuffManagerValue,
      T defaultBuffValue,
      BiFunction<T, T, T> appendFunc,
      Modifier.ModifierTooltipGetter<T> tooltipFunc,
      ModifierLimiter<T> limiter
   ) {
      this(
         list,
         null,
         defaultBuffManagerValue,
         defaultBuffValue,
         (currentValue, appendValue, count) -> appendFunc.apply(currentValue, appendValue),
         null,
         tooltipFunc,
         limiter
      );
   }

   public ModifierTooltip getTooltip(T value, T lastValue, T defaultValue, boolean forceNonNull) {
      if (!forceNonNull && Objects.equals(value, defaultValue)) {
         return null;
      } else {
         return this.tooltipFunc == null ? null : this.tooltipFunc.get(value, lastValue, defaultValue);
      }
   }

   public ModifierTooltip getTooltip(T value, T lastValue, T defaultValue) {
      return this.getTooltip(value, lastValue, defaultValue, false);
   }

   public ModifierTooltip getTooltip(T value, T defaultValue) {
      return this.getTooltip(value, null, defaultValue);
   }

   public T appendManager(T currentValue, T appendValue) {
      return this.appendManager(currentValue, appendValue, 1);
   }

   public T appendManager(T currentValue, T appendValue, int count) {
      return count <= 0 ? currentValue : this.appendFunc.apply(currentValue, appendValue, count);
   }

   public T finalLimit(T currentValue) {
      return this.finalLimiter != null ? this.finalLimiter.apply(currentValue) : currentValue;
   }

   public T max(T currentValue, T maxValue) {
      return this.limiter != null ? this.limiter.max(currentValue, maxValue) : currentValue;
   }

   public T min(T currentValue, T minValue) {
      return this.limiter != null ? this.limiter.min(currentValue, minValue) : currentValue;
   }

   public ModifierTooltip getMinTooltip(T minValue, T lastMinValue, T currentNonLimitedValue) {
      return this.limiter != null ? this.limiter.getMinTooltip(minValue, lastMinValue, currentNonLimitedValue, this.defaultBuffManagerValue) : null;
   }

   public ModifierTooltip getMinTooltip(T minValue, T currentNonLimitedValue) {
      return this.getMinTooltip(minValue, null, currentNonLimitedValue);
   }

   public ModifierTooltip getMinTooltip(T minValue) {
      return this.getMinTooltip(minValue, null);
   }

   public ModifierTooltip getMaxTooltip(T maxValue, T lastMaxValue, T currentNonLimitedValue) {
      return this.limiter != null ? this.limiter.getMaxTooltip(maxValue, lastMaxValue, currentNonLimitedValue, this.defaultBuffManagerValue) : null;
   }

   public ModifierTooltip getMaxTooltip(T maxValue, T currentNonLimitedValue) {
      return this.getMaxTooltip(maxValue, null, currentNonLimitedValue);
   }

   public ModifierTooltip getMaxTooltip(T maxValue) {
      return this.getMaxTooltip(maxValue, null);
   }

   @FunctionalInterface
   public interface ModifierTooltipGetter<V> {
      ModifierTooltip get(V var1, V var2, V var3);
   }
}
