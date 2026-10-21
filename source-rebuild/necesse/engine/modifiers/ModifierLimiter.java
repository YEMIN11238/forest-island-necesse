package necesse.engine.modifiers;

import java.awt.Color;
import java.util.function.BiFunction;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.inventory.item.DoubleItemStatTip;

public class ModifierLimiter<T> {
   private final BiFunction<T, T, T> min;
   private final BiFunction<T, T, T> max;
   private final ModifierLimiter.LimitTooltipGetter<T> minTooltip;
   private final ModifierLimiter.LimitTooltipGetter<T> maxTooltip;

   private static int normalFloatMinSign(Float limit, Float current, Float def) {
      if (current == null) {
         return limit > def ? 1 : 0;
      } else {
         return limit > current ? 1 : 0;
      }
   }

   private static int normalFloatMaxSign(Float limit, Float current, Float def) {
      if (current == null) {
         return limit < def ? -1 : 0;
      } else {
         return limit < current ? -1 : 0;
      }
   }

   private static int normalIntMinSign(Integer limit, Integer current, Integer def) {
      if (current == null) {
         return limit > def ? 1 : 0;
      } else {
         return limit > current ? 1 : 0;
      }
   }

   private static int normalIntMaxSign(Integer limit, Integer current, Integer def) {
      if (current == null) {
         return limit < def ? -1 : 0;
      } else {
         return limit < current ? -1 : 0;
      }
   }

   public static ModifierLimiter<Float> NORMAL_PERC_LIMITER(String localisationKey) {
      return new ModifierLimiter<>(
         Math::min,
         Math::max,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue * 100.0F, 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmin",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue * 100.0F);
            }

            return new ModifierTooltip(normalFloatMinSign(limitValue, currentValue, defaultValue), tip);
         },
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue * 100.0F, 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmax",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue * 100.0F);
            }

            return new ModifierTooltip(normalFloatMaxSign(limitValue, currentValue, defaultValue), tip);
         }
      );
   }

   @Deprecated
   public static ModifierLimiter<Float> PERC_LIMITER(String localisationKey) {
      return NORMAL_PERC_LIMITER(localisationKey);
   }

   public static ModifierLimiter<Float> INVERSE_PERC_LIMITER(String localisationKey) {
      return INVERSE_PERC_LIMITER(localisationKey, null);
   }

   public static ModifierLimiter<Float> INVERSE_PERC_LIMITER(String localisationKey, String maxZeroLocalizationKey) {
      return new ModifierLimiter<>(
         Math::min,
         Math::max,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue * 100.0F, 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmin",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue * 100.0F, false);
            }

            return new ModifierTooltip(-normalFloatMinSign(limitValue, currentValue, defaultValue), tip);
         },
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue * 100.0F, 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return this.value == 0.0 && maxZeroLocalizationKey != null
                     ? this.colorMessage(betterColor, worseColor, new LocalMessage("buffmodifiers", maxZeroLocalizationKey))
                     : new LocalMessage(
                        "buffmodifiers",
                        "limitmax",
                        "modifier",
                        new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                     );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue * 100.0F, false);
            }

            return new ModifierTooltip(-normalFloatMaxSign(limitValue, currentValue, defaultValue), tip);
         }
      );
   }

   public static ModifierLimiter<Integer> NORMAL_FLAT_INT_LIMITER(String localisationKey) {
      return new ModifierLimiter<>(
         Math::min,
         Math::max,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.intValue(), 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmin",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.intValue());
            }

            return new ModifierTooltip(normalIntMinSign(limitValue, currentValue, defaultValue), tip);
         },
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.intValue(), 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmax",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.intValue());
            }

            return new ModifierTooltip(normalIntMaxSign(limitValue, currentValue, defaultValue), tip);
         }
      );
   }

   @Deprecated
   public static ModifierLimiter<Integer> FLAT_INT_LIMITER(String localisationKey) {
      return NORMAL_FLAT_INT_LIMITER(localisationKey);
   }

   public static ModifierLimiter<Integer> INVERSE_FLAT_INT_LIMITER(String localisationKey) {
      return new ModifierLimiter<>(
         Math::min,
         Math::max,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.intValue(), 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmin",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.intValue(), false);
            }

            return new ModifierTooltip(-normalIntMinSign(limitValue, currentValue, defaultValue), tip);
         },
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.intValue(), 0) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmax",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.intValue(), false);
            }

            return new ModifierTooltip(-normalIntMaxSign(limitValue, currentValue, defaultValue), tip);
         }
      );
   }

   public static ModifierLimiter<Float> NORMAL_FLAT_FLOAT_LIMITER(String localisationKey) {
      return new ModifierLimiter<>(
         Math::min,
         Math::max,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.floatValue(), 2) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmin",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.floatValue());
            }

            return new ModifierTooltip(normalFloatMinSign(limitValue, currentValue, defaultValue), tip);
         },
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.floatValue(), 2) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmax",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.floatValue());
            }

            return new ModifierTooltip(normalFloatMaxSign(limitValue, currentValue, defaultValue), tip);
         }
      );
   }

   @Deprecated
   public static ModifierLimiter<Float> FLAT_FLOAT_LIMITER(String localisationKey) {
      return NORMAL_FLAT_FLOAT_LIMITER(localisationKey);
   }

   public static ModifierLimiter<Float> INVERSE_FLAT_FLOAT_LIMITER(String localisationKey) {
      return new ModifierLimiter<>(
         Math::min,
         Math::max,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.floatValue(), 2) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmin",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.floatValue(), false);
            }

            return new ModifierTooltip(-normalFloatMinSign(limitValue, currentValue, defaultValue), tip);
         },
         (limitValue, lastLimitValue, currentValue, defaultValue) -> {
            DoubleItemStatTip tip = new DoubleItemStatTip(limitValue.floatValue(), 2) {
               @Override
               public GameMessage toMessage(Color betterColor, Color worseColor, Color neutralColor, boolean showDifference) {
                  return new LocalMessage(
                     "buffmodifiers",
                     "limitmax",
                     "modifier",
                     new LocalMessage("buffmodifiers", localisationKey, "mod", this.getReplaceValue(betterColor, worseColor, showDifference))
                  );
               }
            };
            if (lastLimitValue != null) {
               tip.setCompareValue(lastLimitValue.floatValue(), false);
            }

            return new ModifierTooltip(-normalFloatMaxSign(limitValue, currentValue, defaultValue), tip);
         }
      );
   }

   public static ModifierLimiter<Boolean> BOOL_LIMITER(String localizationKey) {
      return new ModifierLimiter<>(
         (v1, v2) -> v2 ? v1 : false,
         (v1, v2) -> v2 || v1,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> null,
         (limitValue, lastLimitValue, currentValue, defaultValue) -> null
      );
   }

   public ModifierLimiter(
      BiFunction<T, T, T> min, BiFunction<T, T, T> max, ModifierLimiter.LimitTooltipGetter<T> minTooltip, ModifierLimiter.LimitTooltipGetter<T> maxTooltip
   ) {
      this.max = max;
      this.min = min;
      this.maxTooltip = maxTooltip;
      this.minTooltip = minTooltip;
   }

   public T min(T currentValue, T minValue) {
      return this.min != null ? this.min.apply(currentValue, minValue) : currentValue;
   }

   public T max(T currentValue, T maxValue) {
      return this.max != null ? this.max.apply(currentValue, maxValue) : currentValue;
   }

   public ModifierTooltip getMinTooltip(T minValue, T lastMinValue, T currentNonLimitedValue, T defaultValue) {
      return this.minTooltip == null ? null : this.minTooltip.get(minValue, lastMinValue, currentNonLimitedValue, defaultValue);
   }

   public ModifierTooltip getMaxTooltip(T maxValue, T lastMaxValue, T currentNonLimitedValue, T defaultValue) {
      return this.maxTooltip == null ? null : this.maxTooltip.get(maxValue, lastMaxValue, currentNonLimitedValue, defaultValue);
   }

   @FunctionalInterface
   public interface LimitTooltipGetter<T> {
      ModifierTooltip get(T var1, T var2, T var3, T var4);
   }
}
