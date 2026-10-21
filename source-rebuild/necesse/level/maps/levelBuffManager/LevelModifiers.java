package necesse.level.maps.levelBuffManager;

import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierLimiter;
import necesse.engine.modifiers.ModifierList;

public class LevelModifiers {
   public static final ModifierList LIST = new ModifierList();
   public static final Modifier<Float> LOOT = new Modifier<>(
      LIST,
      "loot",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("loot"),
      ModifierLimiter.NORMAL_PERC_LIMITER("loot")
   );
   public static final Modifier<Float> ENEMY_DAMAGE = new Modifier<>(
      LIST,
      "enemydamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("enemydamage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("enemydamage")
   );
   public static final Modifier<Float> ENEMY_SPEED = new Modifier<>(
      LIST,
      "enemyspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("enemyspeed"),
      ModifierLimiter.INVERSE_PERC_LIMITER("enemyspeed")
   );
   public static final Modifier<Float> ENEMY_MAX_HEALTH = new Modifier<>(
      LIST,
      "enemymaxhealth",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("enemymaxhealth"),
      ModifierLimiter.INVERSE_PERC_LIMITER("enemymaxhealth")
   );
   public static final Modifier<Boolean> ENEMIES_RETREATING = new Modifier<>(
      LIST, "enemiesretreating", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("enemiesretreating"), null
   );
   public static final Modifier<Boolean> BANNER_OF_WAR_DISABLED = new Modifier<>(
      LIST,
      "bannerofwardisabled",
      false,
      false,
      Modifier.OR_APPEND,
      Modifier.BOOL_PARSER("bannerofwardisabled"),
      ModifierLimiter.BOOL_LIMITER("bannerofwardisabled")
   );
   public static final Modifier<Boolean> MODIFIERS_AFFECT_ENEMIES = new Modifier<>(
      LIST,
      "modifiersaffectenemies",
      false,
      false,
      Modifier.OR_APPEND,
      Modifier.BOOL_PARSER("modifiersaffectenemies"),
      ModifierLimiter.BOOL_LIMITER("modifiersaffectenemies")
   );
   public static final Modifier<Boolean> SPIRIT_CORRUPTED = new Modifier<>(
      LIST, "spiritcorrupted", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("spiritcorrupted"), null
   );
}
