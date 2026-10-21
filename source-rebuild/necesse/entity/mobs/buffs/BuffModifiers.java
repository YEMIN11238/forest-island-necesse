package necesse.entity.mobs.buffs;

import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierLimiter;
import necesse.engine.modifiers.ModifierList;
import necesse.engine.util.GameMath;

public class BuffModifiers {
   public static final ModifierList LIST = new ModifierList();
   public static final Modifier<Float> ALL_DAMAGE = new Modifier<>(
      LIST,
      "alldamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("damage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("damage")
   );
   public static final Modifier<Float> MELEE_DAMAGE = new Modifier<>(
      LIST,
      "meleedamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("meleedamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("meleedamage")
   );
   public static final Modifier<Float> RANGED_DAMAGE = new Modifier<>(
      LIST,
      "rangedamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("rangeddamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("rangeddamage")
   );
   public static final Modifier<Float> MAGIC_DAMAGE = new Modifier<>(
      LIST,
      "magicdamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("magicdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("magicdamage")
   );
   public static final Modifier<Float> SUMMON_DAMAGE = new Modifier<>(
      LIST,
      "summondamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("summondamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summondamage")
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
   public static final Modifier<Integer> TOOL_DAMAGE_FLAT = new Modifier<>(
      LIST,
      "tooldamageflat",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("tooldamageflat"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("tooldamageflat")
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
   public static final Modifier<Float> MELEE_CRIT_CHANCE = new Modifier<>(
      LIST,
      "meleecritchance",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("meleecritchance"),
      ModifierLimiter.NORMAL_PERC_LIMITER("meleecritchance")
   );
   public static final Modifier<Float> RANGED_CRIT_CHANCE = new Modifier<>(
      LIST,
      "rangedcritchance",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("rangedcritchance"),
      ModifierLimiter.NORMAL_PERC_LIMITER("rangedcritchance")
   );
   public static final Modifier<Float> MAGIC_CRIT_CHANCE = new Modifier<>(
      LIST,
      "magiccritchance",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("magiccritchance"),
      ModifierLimiter.NORMAL_PERC_LIMITER("magiccritchance")
   );
   public static final Modifier<Float> SUMMON_CRIT_CHANCE = new Modifier<>(
      LIST,
      "summoncritchance",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("summoncritchance"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summoncritchance")
   );
   public static final Modifier<Float> CRIT_DAMAGE = new Modifier<>(
      LIST,
      "critdamage",
      2.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(1.0F, v),
      Modifier.NORMAL_PERC_PARSER("critdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("critdamage")
   );
   public static final Modifier<Float> MELEE_CRIT_DAMAGE = new Modifier<>(
      LIST,
      "meleecritdamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("meleecritdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("meleecritdamage")
   );
   public static final Modifier<Float> RANGED_CRIT_DAMAGE = new Modifier<>(
      LIST,
      "rangedcritdamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("rangedcritdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("rangedcritdamage")
   );
   public static final Modifier<Float> MAGIC_CRIT_DAMAGE = new Modifier<>(
      LIST,
      "magiccritdamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("magiccritdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("magiccritdamage")
   );
   public static final Modifier<Float> SUMMON_CRIT_DAMAGE = new Modifier<>(
      LIST,
      "summoncritdamage",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("summoncritdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summoncritdamage")
   );
   public static final Modifier<Float> ARMOR_PEN = new Modifier<>(
      LIST,
      "armorpen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("armorpen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("armorpen")
   );
   public static final Modifier<Integer> ARMOR_PEN_FLAT = new Modifier<>(
      LIST,
      "armorpenflat",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("armorpenflat"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("armorpenflat")
   );
   public static final Modifier<Float> KNOCKBACK_OUT = new Modifier<>(
      LIST,
      "knockbackout",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("knockbackout"),
      ModifierLimiter.NORMAL_PERC_LIMITER("knockbackout")
   );
   public static final Modifier<Float> SPEED = new Modifier<>(
      LIST,
      "speed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("speed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("speed")
   );
   public static final Modifier<Float> SPEED_FLAT = new Modifier<>(
      LIST,
      "speedflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("speedflat"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("speedflat")
   );
   public static final Modifier<Float> SLOW = new Modifier<>(
      LIST,
      "slow",
      0.0F,
      0.0F,
      (currentValue, appendValue, count) -> Math.max(currentValue, appendValue),
      v -> GameMath.limit(v, 0.0F, 1.0F),
      Modifier.BAD_PERCENT_MODIFIER("slow"),
      ModifierLimiter.INVERSE_PERC_LIMITER("slow", "slowimun")
   );
   public static final Modifier<Float> SWIM_SPEED = new Modifier<>(
      LIST,
      "swimspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("swimspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("swimspeed")
   );
   public static final Modifier<Float> FRICTION = new Modifier<>(
      LIST,
      "friction",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("friction"),
      ModifierLimiter.NORMAL_PERC_LIMITER("friction")
   );
   public static final Modifier<Float> ACCELERATION = new Modifier<>(
      LIST,
      "acceleration",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("acceleration"),
      ModifierLimiter.NORMAL_PERC_LIMITER("acceleration")
   );
   public static final Modifier<Float> DECELERATION = new Modifier<>(
      LIST,
      "deceleration",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("deceleration"),
      ModifierLimiter.NORMAL_PERC_LIMITER("deceleration")
   );
   public static final Modifier<Float> ARMOR = new Modifier<>(
      LIST,
      "armor",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("armor"),
      ModifierLimiter.NORMAL_PERC_LIMITER("armor")
   );
   public static final Modifier<Integer> ARMOR_FLAT = new Modifier<>(
      LIST, "armorflat", 0, 0, Modifier.INT_ADD_APPEND, Modifier.NORMAL_FLAT_INT_PARSER("armorflat"), ModifierLimiter.NORMAL_FLAT_INT_LIMITER("armorflat")
   );
   public static final Modifier<Float> INCOMING_DAMAGE_MOD = new Modifier<>(
      LIST,
      "incomingdamagemod",
      1.0F,
      1.0F,
      Modifier.FLOAT_MULT_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.LESS_GOOD_PERCENT_MODIFIER("incdamage"),
      ModifierLimiter.NORMAL_PERC_LIMITER("incdamage")
   );
   public static final Modifier<Float> MAX_HEALTH = new Modifier<>(
      LIST,
      "maxhealth",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("maxhealth"),
      ModifierLimiter.NORMAL_PERC_LIMITER("maxhealth")
   );
   public static final Modifier<Integer> MAX_HEALTH_FLAT = new Modifier<>(
      LIST,
      "maxhealthflat",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("maxhealthflat"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("maxhealthflat")
   );
   public static final Modifier<Float> MAX_RESILIENCE = new Modifier<>(
      LIST,
      "maxresilience",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("maxresilience"),
      ModifierLimiter.NORMAL_PERC_LIMITER("maxresilience")
   );
   public static final Modifier<Integer> MAX_RESILIENCE_FLAT = new Modifier<>(
      LIST,
      "maxresilienceflat",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("maxresilienceflat"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("maxresilienceflat")
   );
   public static final Modifier<Float> MAX_MANA = new Modifier<>(
      LIST,
      "maxmana",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("maxmana"),
      ModifierLimiter.NORMAL_PERC_LIMITER("maxmana")
   );
   public static final Modifier<Integer> MAX_MANA_FLAT = new Modifier<>(
      LIST,
      "maxmanaflat",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("maxmanaflat"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("maxmanaflat")
   );
   public static final Modifier<Float> HEALTH_REGEN = new Modifier<>(
      LIST,
      "basehealthregen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("basehealthregen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("basehealthregen")
   );
   public static final Modifier<Float> HEALTH_REGEN_FLAT = new Modifier<>(
      LIST,
      "basehealthregenflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("basehealthregenflat"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("basehealthregenflat")
   );
   public static final Modifier<Float> COMBAT_HEALTH_REGEN = new Modifier<>(
      LIST,
      "combathealthregen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("combathealthregen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("combathealthregen")
   );
   public static final Modifier<Float> COMBAT_HEALTH_REGEN_FLAT = new Modifier<>(
      LIST,
      "combathealthregenflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("combathealthregenflat"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("combathealthregenflat")
   );
   @Deprecated
   public static final Modifier<Float> REGEN = HEALTH_REGEN;
   @Deprecated
   public static final Modifier<Float> REGEN_FLAT = HEALTH_REGEN_FLAT;
   @Deprecated
   public static final Modifier<Float> COMBAT_REGEN = COMBAT_HEALTH_REGEN;
   @Deprecated
   public static final Modifier<Float> COMBAT_REGEN_FLAT = COMBAT_HEALTH_REGEN_FLAT;
   public static final Modifier<Float> RESILIENCE_GAIN = new Modifier<>(
      LIST,
      "resiliencegain",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("resiliencegain"),
      ModifierLimiter.NORMAL_PERC_LIMITER("resiliencegain")
   );
   public static final Modifier<Float> RESILIENCE_DECAY = new Modifier<>(
      LIST,
      "resiliencedecay",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("resiliencedecay"),
      ModifierLimiter.INVERSE_PERC_LIMITER("resiliencedecay")
   );
   public static final Modifier<Float> RESILIENCE_DECAY_FLAT = new Modifier<>(
      LIST,
      "resiliencedecayflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_FLAT_FLOAT_PARSER("resiliencedecayflat"),
      ModifierLimiter.INVERSE_FLAT_FLOAT_LIMITER("resiliencedecayflat")
   );
   public static final Modifier<Float> RESILIENCE_REGEN = new Modifier<>(
      LIST,
      "resilienceregen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("resilienceregen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("resilienceregen")
   );
   public static final Modifier<Float> RESILIENCE_REGEN_FLAT = new Modifier<>(
      LIST,
      "resilienceregenflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_FLAT_FLOAT_PARSER("resilienceregenflat"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("resilienceregenflat")
   );
   public static final Modifier<Float> MANA_REGEN = new Modifier<>(
      LIST,
      "basemanaregen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("basemanaregen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("basemanaregen")
   );
   public static final Modifier<Float> MANA_REGEN_FLAT = new Modifier<>(
      LIST,
      "basemanaregenflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("basemanaregenflat"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("basemanaregenflat")
   );
   public static final Modifier<Float> COMBAT_MANA_REGEN = new Modifier<>(
      LIST,
      "combatmanaregen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("combatmanaregen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("combatmanaregen")
   );
   public static final Modifier<Float> COMBAT_MANA_REGEN_FLAT = new Modifier<>(
      LIST,
      "combatmanaregenflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("combatmanaregenflat"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("combatmanaregenflat")
   );
   public static final Modifier<Float> LIFE_ESSENCE_GAIN = new Modifier<>(
      LIST,
      "lifeessencegain",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("lifeessencegain"),
      ModifierLimiter.NORMAL_PERC_LIMITER("lifeessencegain")
   );
   public static final Modifier<Float> LIFE_ESSENCE_DURATION = new Modifier<>(
      LIST,
      "lifeessenceduration",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("lifeessenceduration"),
      ModifierLimiter.NORMAL_PERC_LIMITER("lifeessenceduration")
   );
   public static final Modifier<Float> POISON_DAMAGE = new Modifier<>(
      LIST,
      "poisondamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("poisondamage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("poisondamage", "poisondamageimun")
   );
   public static final Modifier<Float> POISON_DAMAGE_FLAT = new Modifier<>(
      LIST,
      "poisondamageflat",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("poisondamageflat"),
      ModifierLimiter.INVERSE_FLAT_FLOAT_LIMITER("poisondamageflat")
   );
   public static final Modifier<Float> FIRE_DAMAGE = new Modifier<>(
      LIST,
      "firedamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("firedamage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("firedamage", "firedamageimun")
   );
   public static final Modifier<Float> FIRE_DAMAGE_FLAT = new Modifier<>(
      LIST,
      "firedamageflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_FLAT_FLOAT_PARSER("firedamageflat"),
      ModifierLimiter.INVERSE_FLAT_FLOAT_LIMITER("firedamageflat")
   );
   public static final Modifier<Float> FROST_DAMAGE = new Modifier<>(
      LIST,
      "frostdamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("frostdamage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("frostdamage", "frostdamageimun")
   );
   public static final Modifier<Float> FROST_DAMAGE_FLAT = new Modifier<>(
      LIST,
      "frostdamageflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_FLAT_FLOAT_PARSER("frostdamageflat"),
      ModifierLimiter.INVERSE_FLAT_FLOAT_LIMITER("frostdamageflat")
   );
   public static final Modifier<Float> BLEED_DAMAGE = new Modifier<>(
      LIST,
      "bleeddamage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("bleeddamage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("bleeddamage", "bleeddamageimun")
   );
   public static final Modifier<Float> BLEED_DAMAGE_FLAT = new Modifier<>(
      LIST,
      "bleeddamageflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_FLAT_FLOAT_PARSER("bleeddamageflat"),
      ModifierLimiter.INVERSE_FLAT_FLOAT_LIMITER("bleeddamageflat")
   );
   public static final Modifier<Float> ATTACK_SPEED = new Modifier<>(
      LIST,
      "attackspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("attackspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("attackspeed")
   );
   public static final Modifier<Float> MELEE_ATTACK_SPEED = new Modifier<>(
      LIST,
      "meleeattackspeed",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("meleeattackspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("meleeattackspeed")
   );
   public static final Modifier<Float> RANGED_ATTACK_SPEED = new Modifier<>(
      LIST,
      "rangedattackspeed",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("rangedattackspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("rangedattackspeed")
   );
   public static final Modifier<Float> MAGIC_ATTACK_SPEED = new Modifier<>(
      LIST,
      "magicattackspeed",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("magicattackspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("magicattackspeed")
   );
   public static final Modifier<Float> SUMMON_ATTACK_SPEED = new Modifier<>(
      LIST,
      "summonattackspeed",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.NORMAL_PERC_PARSER("summonattackspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summonattackspeed")
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
   public static final Modifier<Float> BUILDING_SPEED = new Modifier<>(
      LIST,
      "buildingspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("buildingspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("buildingspeed")
   );
   public static final Modifier<Integer> TOOL_TIER = new Modifier<>(
      LIST, "tooltier", 0, 0, Modifier.INT_ADD_APPEND, Modifier.NORMAL_FLAT_INT_PARSER("tooltier"), ModifierLimiter.NORMAL_FLAT_INT_LIMITER("tooltier")
   );
   public static final Modifier<Float> MINING_RANGE = new Modifier<>(
      LIST,
      "miningrange",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("miningrange"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("miningrange")
   );
   public static final Modifier<Float> BUILD_RANGE = new Modifier<>(
      LIST,
      "buildrange",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      Modifier.NORMAL_FLAT_FLOAT_PARSER("buildingrange"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("buildingrange")
   );
   public static float MAX_PICKUP_RANGE_MODIFIER = 50.0F;
   public static final Modifier<Float> ITEM_PICKUP_RANGE = new Modifier<>(
      LIST,
      "itempickuprange",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.min(v, MAX_PICKUP_RANGE_MODIFIER),
      Modifier.NORMAL_FLAT_FLOAT_PARSER("itempickuprange"),
      ModifierLimiter.NORMAL_FLAT_FLOAT_LIMITER("itempickuprange")
   );
   public static final Modifier<Float> ARROW_USAGE = new Modifier<>(
      LIST,
      "arrowusage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> GameMath.limit(v, 0.0F, 1.0F),
      Modifier.INVERSE_PERC_PARSER("arrowusage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("arrowusage")
   );
   public static final Modifier<Float> BULLET_USAGE = new Modifier<>(
      LIST,
      "bulletusage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> GameMath.limit(v, 0.0F, 1.0F),
      Modifier.INVERSE_PERC_PARSER("bulletusage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("bulletusage")
   );
   public static final Modifier<Float> MANA_USAGE = new Modifier<>(
      LIST,
      "manausage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("manausage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("manausage")
   );
   public static final Modifier<Integer> MAX_SUMMONS = new Modifier<>(
      LIST,
      "maxsummons",
      1,
      0,
      Modifier.INT_ADD_APPEND,
      v -> Math.max(0, v),
      Modifier.NORMAL_FLAT_INT_PARSER("maxsummons"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("maxsummons")
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
   public static final Modifier<Float> SUMMONS_TARGET_RANGE = new Modifier<>(
      LIST,
      "summonstargetrange",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("summonstargetrange"),
      ModifierLimiter.NORMAL_PERC_LIMITER("summonstargetrange")
   );
   public static final Modifier<Float> ATTACK_MOVEMENT_MOD = new Modifier<>(
      LIST,
      "attackmovementmod",
      1.0F,
      1.0F,
      Modifier.FLOAT_MULT_APPEND,
      v -> GameMath.limit(v, 0.0F, 1.0F),
      Modifier.INVERSE_PERC_PARSER("attackmovement"),
      ModifierLimiter.INVERSE_PERC_LIMITER("attackmovement")
   );
   public static final Modifier<Float> PROJECTILE_VELOCITY = new Modifier<>(
      LIST,
      "projectilevelocity",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("projectilevel"),
      ModifierLimiter.NORMAL_PERC_LIMITER("projectilevel")
   );
   public static final Modifier<Float> THROWING_VELOCITY = new Modifier<>(
      LIST,
      "throwingvelocity",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("throwingvel"),
      ModifierLimiter.NORMAL_PERC_LIMITER("throwingvel")
   );
   public static final Modifier<Integer> PROJECTILE_BOUNCES = new Modifier<>(
      LIST,
      "projectilebounces",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      v -> Math.max(0, v),
      Modifier.NORMAL_FLAT_INT_PARSER("projectilebounce"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("projectilebounce")
   );
   public static final Modifier<Integer> PROJECTILE_PIERCES = new Modifier<>(
      LIST,
      "projectilepierces",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      v -> Math.max(0, v),
      Modifier.NORMAL_FLAT_INT_PARSER("projectilepierce"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("projectilepierce")
   );
   public static final Modifier<Float> BLINDNESS = new Modifier<>(
      LIST,
      "blindness",
      0.0F,
      0.0F,
      (currentValue, appendValue, count) -> Math.max(currentValue, appendValue),
      v -> GameMath.limit(v, 0.0F, 1.0F),
      Modifier.BAD_PERCENT_MODIFIER("blindness"),
      ModifierLimiter.INVERSE_PERC_LIMITER("blindness")
   );
   public static final Modifier<Integer> DASH_STACKS = new Modifier<>(
      LIST,
      "dashstacks",
      1,
      0,
      Modifier.INT_ADD_APPEND,
      v -> GameMath.limit(v, 1, 100),
      Modifier.NORMAL_FLAT_INT_PARSER("dashstacks"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("dashstacks")
   );
   public static final Modifier<Float> DASH_COOLDOWN = new Modifier<>(
      LIST,
      "dashcooldown",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("dashcooldown"),
      ModifierLimiter.INVERSE_PERC_LIMITER("dashcooldown")
   );
   public static final Modifier<Float> DASH_COOLDOWN_FLAT = new Modifier<>(
      LIST,
      "dashcooldownflat",
      0.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> v,
      Modifier.INVERSE_FLAT_FLOAT_PARSER("dashcooldownflat"),
      ModifierLimiter.INVERSE_FLAT_FLOAT_LIMITER("dashcooldownflat")
   );
   public static final Modifier<Float> POTION_DURATION = new Modifier<>(
      LIST,
      "potionduration",
      1.0F,
      1.0F,
      (currentValue, appendValue, count) -> currentValue * appendValue,
      v -> Math.max(v, 0.0F),
      Modifier.NORMAL_PERC_PARSER("potionduration"),
      ModifierLimiter.NORMAL_PERC_LIMITER("potionduration")
   );
   public static final Modifier<Float> STAMINA_CAPACITY = new Modifier<>(
      LIST,
      "staminacapacity",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("staminacap"),
      ModifierLimiter.NORMAL_PERC_LIMITER("staminacap")
   );
   public static final Modifier<Float> STAMINA_USAGE = new Modifier<>(
      LIST,
      "staminausage",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("staminausage"),
      ModifierLimiter.INVERSE_PERC_LIMITER("staminausage")
   );
   public static final Modifier<Float> STAMINA_REGEN = new Modifier<>(
      LIST,
      "staminaregen",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("staminaregen"),
      ModifierLimiter.NORMAL_PERC_LIMITER("staminaregen")
   );
   public static final Modifier<Integer> TRAVEL_DISTANCE = new Modifier<>(
      LIST,
      "traveldistance",
      3,
      0,
      Modifier.INT_ADD_APPEND,
      v -> GameMath.limit(v, 1, 100),
      Modifier.NORMAL_FLAT_INT_PARSER("traveldistance"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("traveldistance")
   );
   public static final Modifier<Integer> BIOME_VIEW_DISTANCE = new Modifier<>(
      LIST,
      "biomeviewdistance",
      1,
      0,
      Modifier.INT_ADD_APPEND,
      v -> Math.max(0, v),
      Modifier.NORMAL_FLAT_INT_PARSER("biomeviewdistance"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("biomeviewdistance")
   );
   public static final Modifier<Float> KNOCKBACK_INCOMING_MOD = new Modifier<>(
      LIST,
      "knockbackin",
      1.0F,
      1.0F,
      Modifier.FLOAT_MULT_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.LESS_GOOD_PERCENT_MODIFIER("knockbackin"),
      ModifierLimiter.INVERSE_PERC_LIMITER("knockbackin", "knockbackimun")
   );
   public static final Modifier<Integer> FISHING_POWER = new Modifier<>(
      LIST,
      "fishingpower",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("fishingpower"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("fishingpower")
   );
   public static final Modifier<Integer> FISHING_LINES = new Modifier<>(
      LIST,
      "fishinglines",
      0,
      0,
      Modifier.INT_ADD_APPEND,
      Modifier.NORMAL_FLAT_INT_PARSER("fishinglines"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("fishinglines")
   );
   public static final Modifier<Boolean> WATER_WALKING = new Modifier<>(
      LIST, "waterwalking", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("waterwalking"), null
   );
   public static final Modifier<Boolean> EMITS_LIGHT = new Modifier<>(
      LIST, "emitlight", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("emitslight"), null
   );
   public static final Modifier<Boolean> INVISIBILITY = new Modifier<>(
      LIST, "invisibility", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("invisibility"), null
   );
   public static final Modifier<Boolean> SPELUNKER = new Modifier<>(
      LIST, "spelunker", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("spelunker"), null
   );
   public static final Modifier<Boolean> TREASURE_HUNTER = new Modifier<>(
      LIST, "treasurehunter", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("treasurehunter"), null
   );
   public static final Modifier<Boolean> ENEMY_TRACKER = new Modifier<>(
      LIST, "enemytracker", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("enemytracker"), null
   );
   public static final Modifier<Boolean> BOUNCY = new Modifier<>(LIST, "bouncy", false, false, Modifier.OR_APPEND, Modifier.NEUTRAL_BOOL_PARSER("bouncy"), null);
   public static final Modifier<Boolean> PARALYZED = new Modifier<>(
      LIST, "paralyzed", false, false, Modifier.OR_APPEND, Modifier.INVERSE_BOOL_PARSER("paralyzed"), null
   );
   public static final Modifier<Boolean> UNTARGETABLE = new Modifier<>(
      LIST, "untargetable", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("untargetable"), null
   );
   public static final Modifier<Boolean> INTIMIDATED = new Modifier<>(
      LIST, "intimidated", false, false, Modifier.OR_APPEND, Modifier.INVERSE_BOOL_PARSER("intimidated"), null
   );
   public static final Modifier<Boolean> CONFUSED = new Modifier<>(
      LIST, "confused", false, false, Modifier.OR_APPEND, Modifier.INVERSE_BOOL_PARSER("confused"), null
   );
   public static final Modifier<Boolean> MANA_EXHAUSTED = new Modifier<>(
      LIST, "manaexhausted", false, false, Modifier.OR_APPEND, Modifier.INVERSE_BOOL_PARSER("manaexhausted"), null
   );
   public static final Modifier<Boolean> GROUNDED = new Modifier<>(
      LIST, "grounded", false, false, Modifier.OR_APPEND, Modifier.INVERSE_BOOL_PARSER("grounded"), null
   );
   public static final Modifier<Boolean> EXTENDED_MAP_DISCOVER_RANGE = new Modifier<>(
      LIST, "extendedmapdiscovery", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("extendedmapdiscovery"), null
   );
   public static final Modifier<Float> MOB_SPAWN_RATE = new Modifier<>(
      LIST,
      "mobspawnrate",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("mobspawnrate"),
      ModifierLimiter.INVERSE_PERC_LIMITER("mobspawnrate")
   );
   public static final Modifier<Float> MOB_SPAWN_CAP = new Modifier<>(
      LIST,
      "mobspawncap",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.INVERSE_PERC_PARSER("mobspawncap"),
      ModifierLimiter.INVERSE_PERC_LIMITER("mobspawncap")
   );
   public static final Modifier<Integer> MOB_SPAWN_LIGHT_THRESHOLD = new Modifier<>(
      LIST,
      "mobspawnlightthreshold",
      50,
      0,
      Modifier.INT_ADD_APPEND,
      v -> GameMath.limit(v, 0, 150),
      Modifier.NORMAL_FLAT_INT_PARSER("mobspawnlightthreshold"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("mobspawnlightthreshold")
   );
   public static float MAX_TARGET_RANGE_MODIFIER = 2.0F;
   public static final Modifier<Float> TARGET_RANGE = new Modifier<>(
      LIST,
      "targetrange",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> GameMath.limit(v, 0.0F, MAX_TARGET_RANGE_MODIFIER),
      Modifier.INVERSE_PERC_PARSER("targetrange"),
      ModifierLimiter.INVERSE_PERC_LIMITER("targetrange")
   );
   public static final Modifier<Float> CHASER_RANGE = new Modifier<>(
      LIST,
      "chaserrange",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("chaserrange"),
      ModifierLimiter.NORMAL_PERC_LIMITER("chaserrange")
   );
   public static final Modifier<Boolean> CAN_BREAK_OBJECTS = new Modifier<>(
      LIST, "canbreakobjects", false, false, Modifier.OR_APPEND, Modifier.BOOL_PARSER("canbreakobjects"), null
   );
   public static final Modifier<Float> WORK_SPEED = new Modifier<>(
      LIST,
      "workspeed",
      1.0F,
      0.0F,
      Modifier.FLOAT_ADD_APPEND,
      v -> Math.max(0.0F, v),
      Modifier.NORMAL_PERC_PARSER("workspeed"),
      ModifierLimiter.NORMAL_PERC_LIMITER("workspeed")
   );
   public static final int MAX_FOOD_BUFFS_TOTAL = 10;
   public static final Modifier<Integer> MAX_FOOD_BUFFS = new Modifier<>(
      LIST,
      "maxfoodbuffs",
      1,
      0,
      Modifier.INT_ADD_APPEND,
      v -> GameMath.limit(v, 1, 10),
      Modifier.NORMAL_FLAT_INT_PARSER("maxfoodbuffs"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("maxfoodbuffs")
   );
   public static final Modifier<Integer> MAX_AURAS = new Modifier<>(
      LIST,
      "maxauras",
      1,
      0,
      Modifier.INT_ADD_APPEND,
      v -> Math.max(0, v),
      Modifier.NORMAL_FLAT_INT_PARSER("maxfoodbuffs"),
      ModifierLimiter.NORMAL_FLAT_INT_LIMITER("maxfoodbuffs")
   );
}
