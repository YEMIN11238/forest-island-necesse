package necesse.level.maps.incursion;

import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierLimiter;
import necesse.engine.modifiers.ModifierList;

public class IncursionDataModifiers {
   public static final ModifierList LIST = new ModifierList();
   public static final Modifier<Float> MODIFIER_EXTRACTION_DROPS = new Modifier<>(
      LIST,
      "extractiondrops",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("extractiondrops"),
      ModifierLimiter.NORMAL_PERC_LIMITER("extractiondrops")
   );
   public static final Modifier<Float> MODIFIER_HUNT_DROPS = new Modifier<>(
      LIST,
      "huntdrops",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("huntdrops"),
      ModifierLimiter.NORMAL_PERC_LIMITER("huntdrops")
   );
}
