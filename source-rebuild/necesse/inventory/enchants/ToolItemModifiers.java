package necesse.inventory.enchants;

import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierLimiter;
import necesse.engine.modifiers.ModifierList;

public class ToolItemModifiers {
   public static final ModifierList LIST = new ModifierList();
   public static final Modifier<Float> DAMAGE = new Modifier<>(
      LIST,
      "damage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("damage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("damage")
   );
   public static final Modifier<Float> CRIT_CHANCE = new Modifier<>(
      LIST,
      "critchance",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("critchance"),
      ModifierLimiter.NORMAL_PERC_LIMITER("critchance")
   );
   public static final Modifier<Integer> ARMOR_PEN = new Modifier<>(
      LIST, "armorpen", 0, 0, Modifier.INT_ADD_APPEND, Modifier.NORMAL_FLAT_INT_PARSER("armorpenflat"), ModifierLimiter.NORMAL_FLAT_INT_LIMITER("armorpenflat")
   );
   public static final Modifier<Float> KNOCKBACK = new Modifier<>(
      LIST,
      "knockback",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("toolknockback"),
      ModifierLimiter.NORMAL_PERC_LIMITER("toolknockback")
   );
   public static final Modifier<Float> RESILIENCE_GAIN = new Modifier<>(
      LIST,
      "resilience",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("resiliencegain"),
      ModifierLimiter.NORMAL_PERC_LIMITER("resiliencegain")
   );
   public static final Modifier<Float> RANGE = new Modifier<>(
      LIST,
      "range",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("toolrange"),
      ModifierLimiter.NORMAL_PERC_LIMITER("toolrange")
   );
   public static final Modifier<Float> SUMMONS_TARGET_RANGE = new Modifier<>(
      LIST,
      "summontargetrange",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("summonstargetrange"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summonstargetrange")
   );
   public static final Modifier<Float> ATTACK_SPEED = new Modifier<>(
      LIST,
      "attackspeed",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("attackspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("attackspeed")
   );
   public static final Modifier<Float> SUMMONS_SPEED = new Modifier<>(
      LIST,
      "summonsspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("summonsspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summonsspeed")
   );
   public static final Modifier<Float> VELOCITY = new Modifier<>(
      LIST,
      "velocity",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("projectilevel"),
      ModifierLimiter.NORMAL_PERC_LIMITER("projectilevel")
   );
   public static final Modifier<Float> MANA_USAGE = new Modifier<>(
      LIST,
      "manausage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("manausage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("manausage")
   );
   public static final Modifier<Float> TOOL_DAMAGE = new Modifier<>(
      LIST,
      "tooldamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("tooldamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("tooldamage")
   );
   public static final Modifier<Float> MINING_SPEED = new Modifier<>(
      LIST,
      "miningspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("miningspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("miningspeed")
   );
}
