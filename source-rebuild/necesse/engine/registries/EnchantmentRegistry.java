package necesse.engine.registries;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modifiers.ModifierList;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.inventory.enchants.EquipmentItemEnchant;
import necesse.inventory.enchants.ItemEnchantment;
import necesse.inventory.enchants.ToolDamageEnchantment;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.enchants.ToolItemModifiers;

public class EnchantmentRegistry extends GameRegistry<ItemEnchantment> {
   public static final EnchantmentRegistry instance = new EnchantmentRegistry();
   public static Set<Integer> equipmentEnchantments = new HashSet<>();
   public static Set<Integer> meleeItemEnchantments = new HashSet<>();
   public static Set<Integer> rangedItemEnchantments = new HashSet<>();
   public static Set<Integer> magicItemEnchantments = new HashSet<>();
   public static Set<Integer> summonItemEnchantments = new HashSet<>();
   public static Set<Integer> toolDamageEnchantments = new HashSet<>();
   public static int keen;
   public static int blunt;
   public static int nimble;
   public static int sluggish;
   public static int quick;
   public static int clumsy;
   public static int sturdy;
   public static int weak;
   public static int precise;
   public static int sloppy;
   public static int bulky;
   public static int puny;
   public static int magical;
   public static int draining;
   public static int tenacious;
   public static int flimsy;
   public static int tough;
   public static int fragile;
   public static int berserk;
   public static int adamant;
   public static int agile;
   public static int harmful;
   public static int grand;
   public static int docile;
   public static int shoddy;
   public static int amateur;
   public static int envious;
   public static int masterful;
   public static int skillful;
   public static int tightened;
   public static int modern;
   public static int trained;
   public static int loose;
   public static int eroding;
   public static int primitive;
   public static int faulty;
   public static int divine;
   public static int wrathful;
   public static int wise;
   public static int adept;
   public static int apprentice;
   public static int decaying;
   public static int novice;
   public static int daft;
   public static int corrupt;
   public static int savage;
   public static int athletic;
   public static int mindful;
   public static int proud;
   public static int aware;
   public static int spoiled;
   public static int sick;
   public static int spiteful;
   public static int naive;
   public static int master;
   public static int shining;
   public static int sharp;
   public static int absurd;
   public static int used;

   public EnchantmentRegistry() {
      super("Enchantment", 32767);
   }

   @Override
   public void registerCore() {
      registerEnchantment("noenchant", new ItemEnchantment(new ModifierList(), 0));
      keen = registerEquipmentEnchantment("keen", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.04F)));
      blunt = registerEquipmentEnchantment("blunt", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.ALL_DAMAGE, -0.04F)));
      nimble = registerEquipmentEnchantment("nimble", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.SPEED, 0.04F)));
      sluggish = registerEquipmentEnchantment("sluggish", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.SPEED, -0.04F)));
      quick = registerEquipmentEnchantment("quick", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.04F)));
      clumsy = registerEquipmentEnchantment("clumsy", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.ATTACK_SPEED, -0.04F)));
      sturdy = registerEquipmentEnchantment("sturdy", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.ARMOR_FLAT, 4)));
      weak = registerEquipmentEnchantment("weak", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.ARMOR_FLAT, -4)));
      precise = registerEquipmentEnchantment("precise", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.CRIT_CHANCE, 0.04F)));
      sloppy = registerEquipmentEnchantment("sloppy", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.CRIT_CHANCE, -0.04F)));
      bulky = registerEquipmentEnchantment("bulky", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, 10)));
      puny = registerEquipmentEnchantment("puny", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.MAX_HEALTH_FLAT, -10)));
      magical = registerEquipmentEnchantment("magical", new EquipmentItemEnchant(5, new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, 0.2F)));
      draining = registerEquipmentEnchantment("draining", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.COMBAT_MANA_REGEN, -0.2F)));
      tenacious = registerEquipmentEnchantment("tenacious", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.04F)));
      flimsy = registerEquipmentEnchantment("flimsy", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, -0.04F)));
      tough = registerEquipmentEnchantment("tough", new EquipmentItemEnchant(10, new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 10)));
      fragile = registerEquipmentEnchantment("fragile", new EquipmentItemEnchant(-20, new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, -10)));
      berserk = registerMeleeEnchantment(
         "berserk",
         new ToolItemEnchantment(
            15,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.15F),
            new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F)
         )
      );
      adamant = registerMeleeEnchantment(
         "adamant",
         new ToolItemEnchantment(
            10,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.1F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.2F),
            new ModifierValue<>(ToolItemModifiers.KNOCKBACK, -1.0F)
         )
      );
      agile = registerMeleeEnchantment("agile", new ToolItemEnchantment(10, new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.3F)));
      harmful = registerMeleeEnchantment(
         "harmful", new ToolItemEnchantment(10, new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.2F), new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F))
      );
      grand = registerMeleeEnchantment(
         "grand",
         new ToolItemEnchantment(
            0,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.2F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.2F),
            new ModifierValue<>(ToolItemModifiers.KNOCKBACK, 0.5F)
         )
      );
      docile = registerMeleeEnchantment("docile", new ToolItemEnchantment(-20, new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.2F)));
      shoddy = registerMeleeEnchantment("shoddy", new ToolItemEnchantment(-20, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.15F)));
      amateur = registerMeleeEnchantment(
         "amateur",
         new ToolItemEnchantment(-30, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.15F), new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.1F))
      );
      envious = registerMeleeEnchantment(
         "envious",
         new ToolItemEnchantment(-40, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.2F), new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.1F))
      );
      masterful = registerRangedEnchantment(
         "masterful",
         new ToolItemEnchantment(
            20,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.15F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.2F),
            new ModifierValue<>(ToolItemModifiers.RANGE, 0.15F)
         )
      );
      skillful = registerRangedEnchantment(
         "skillful",
         new ToolItemEnchantment(
            15,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.1F),
            new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.15F),
            new ModifierValue<>(ToolItemModifiers.RANGE, 0.2F)
         )
      );
      tightened = registerRangedEnchantment(
         "tightened", new ToolItemEnchantment(10, new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.1F), new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.25F))
      );
      modern = registerRangedEnchantment(
         "modern",
         new ToolItemEnchantment(
            10,
            new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.25F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F)
         )
      );
      trained = registerRangedEnchantment(
         "trained",
         new ToolItemEnchantment(10, new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.05F), new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F))
      );
      loose = registerRangedEnchantment(
         "loose", new ToolItemEnchantment(-20, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.05F), new ModifierValue<>(ToolItemModifiers.VELOCITY, -0.25F))
      );
      eroding = registerRangedEnchantment(
         "eroding",
         new ToolItemEnchantment(-30, new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.15F), new ModifierValue<>(ToolItemModifiers.RANGE, -0.1F))
      );
      primitive = registerRangedEnchantment(
         "primitive", new ToolItemEnchantment(-30, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.1F), new ModifierValue<>(ToolItemModifiers.RANGE, -0.2F))
      );
      faulty = registerRangedEnchantment(
         "faulty", new ToolItemEnchantment(-40, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.15F), new ModifierValue<>(ToolItemModifiers.VELOCITY, -0.1F))
      );
      divine = registerMagicEnchantment(
         "divine",
         new ToolItemEnchantment(
            20,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.15F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.1F),
            new ModifierValue<>(ToolItemModifiers.RANGE, 0.2F),
            new ModifierValue<>(ToolItemModifiers.MANA_USAGE, -0.3F)
         )
      );
      wrathful = registerMagicEnchantment(
         "wrathful",
         new ToolItemEnchantment(
            15,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.1F),
            new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F)
         )
      );
      wise = registerMagicEnchantment(
         "wise",
         new ToolItemEnchantment(
            10,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.1F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.15F),
            new ModifierValue<>(ToolItemModifiers.RANGE, 0.25F),
            new ModifierValue<>(ToolItemModifiers.MANA_USAGE, -0.25F)
         )
      );
      adept = registerMagicEnchantment(
         "adept",
         new ToolItemEnchantment(
            10,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, 0.1F),
            new ModifierValue<>(ToolItemModifiers.MANA_USAGE, -0.1F)
         )
      );
      apprentice = registerMagicEnchantment(
         "apprentice",
         new ToolItemEnchantment(
            5,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, 0.1F)
         )
      );
      decaying = registerMagicEnchantment(
         "decaying",
         new ToolItemEnchantment(
            -10,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.05F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, -0.1F),
            new ModifierValue<>(ToolItemModifiers.MANA_USAGE, 0.1F)
         )
      );
      novice = registerMagicEnchantment(
         "novice",
         new ToolItemEnchantment(
            -20,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.1F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.05F),
            new ModifierValue<>(ToolItemModifiers.MANA_USAGE, 0.2F)
         )
      );
      daft = registerMagicEnchantment(
         "daft",
         new ToolItemEnchantment(
            -20,
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.1F),
            new ModifierValue<>(ToolItemModifiers.RANGE, -0.2F),
            new ModifierValue<>(ToolItemModifiers.MANA_USAGE, 0.3F)
         )
      );
      corrupt = registerMagicEnchantment(
         "corrupt",
         new ToolItemEnchantment(
            -40,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.15F),
            new ModifierValue<>(ToolItemModifiers.ATTACK_SPEED, -0.1F),
            new ModifierValue<>(ToolItemModifiers.VELOCITY, -0.1F)
         )
      );
      savage = registerSummonEnchantment(
         "savage",
         new ToolItemEnchantment(20, new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.15F), new ModifierValue<>(ToolItemModifiers.SUMMONS_SPEED, 0.1F))
      );
      athletic = registerSummonEnchantment(
         "athletic",
         new ToolItemEnchantment(
            10, new ModifierValue<>(ToolItemModifiers.SUMMONS_SPEED, 0.25F), new ModifierValue<>(ToolItemModifiers.SUMMONS_TARGET_RANGE, 0.2F)
         )
      );
      mindful = registerSummonEnchantment(
         "mindful", new ToolItemEnchantment(10, new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.1F), new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F))
      );
      proud = registerSummonEnchantment(
         "proud", new ToolItemEnchantment(5, new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.05F), new ModifierValue<>(ToolItemModifiers.SUMMONS_SPEED, 0.1F))
      );
      aware = registerSummonEnchantment(
         "aware",
         new ToolItemEnchantment(
            5,
            new ModifierValue<>(ToolItemModifiers.DAMAGE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.CRIT_CHANCE, 0.05F),
            new ModifierValue<>(ToolItemModifiers.SUMMONS_TARGET_RANGE, 0.2F)
         )
      );
      spoiled = registerSummonEnchantment("spoiled", new ToolItemEnchantment(-20, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.1F)));
      sick = registerSummonEnchantment(
         "sick",
         new ToolItemEnchantment(
            -20, new ModifierValue<>(ToolItemModifiers.SUMMONS_SPEED, -0.15F), new ModifierValue<>(ToolItemModifiers.SUMMONS_TARGET_RANGE, -0.1F)
         )
      );
      spiteful = registerSummonEnchantment(
         "spiteful",
         new ToolItemEnchantment(-20, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.1F), new ModifierValue<>(ToolItemModifiers.SUMMONS_SPEED, -0.05F))
      );
      naive = registerSummonEnchantment(
         "naive",
         new ToolItemEnchantment(-30, new ModifierValue<>(ToolItemModifiers.DAMAGE, -0.15F), new ModifierValue<>(ToolItemModifiers.SUMMONS_TARGET_RANGE, -0.1F))
      );
      master = registerToolEnchantment(
         "master",
         new ToolDamageEnchantment(20, new ModifierValue<>(ToolItemModifiers.TOOL_DAMAGE, 0.2F), new ModifierValue<>(ToolItemModifiers.MINING_SPEED, 0.15F))
      );
      shining = registerToolEnchantment(
         "shining",
         new ToolDamageEnchantment(10, new ModifierValue<>(ToolItemModifiers.TOOL_DAMAGE, 0.1F), new ModifierValue<>(ToolItemModifiers.MINING_SPEED, 0.1F))
      );
      sharp = registerToolEnchantment("sharp", new ToolDamageEnchantment(5, new ModifierValue<>(ToolItemModifiers.TOOL_DAMAGE, 0.1F)));
      absurd = registerToolEnchantment("absurd", new ToolDamageEnchantment(-20, new ModifierValue<>(ToolItemModifiers.MINING_SPEED, -0.1F)));
      used = registerToolEnchantment("used", new ToolDamageEnchantment(-30, new ModifierValue<>(ToolItemModifiers.TOOL_DAMAGE, -0.15F)));
   }

   public static int registerEnchantment(String stringID, ItemEnchantment enchantment) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register enchantments");
      } else {
         return instance.register(stringID, enchantment);
      }
   }

   public static int registerEquipmentEnchantment(String stringID, EquipmentItemEnchant enchantment) {
      int id = registerEnchantment(stringID, enchantment);
      equipmentEnchantments.add(id);
      return id;
   }

   public static int registerMeleeEnchantment(String stringID, ToolItemEnchantment enchantment) {
      int id = registerEnchantment(stringID, enchantment);
      meleeItemEnchantments.add(id);
      return id;
   }

   public static int registerRangedEnchantment(String stringID, ToolItemEnchantment enchantment) {
      int id = registerEnchantment(stringID, enchantment);
      rangedItemEnchantments.add(id);
      return id;
   }

   public static int registerMagicEnchantment(String stringID, ToolItemEnchantment enchantment) {
      int id = registerEnchantment(stringID, enchantment);
      magicItemEnchantments.add(id);
      return id;
   }

   public static int registerSummonEnchantment(String stringID, ToolItemEnchantment enchantment) {
      int id = registerEnchantment(stringID, enchantment);
      summonItemEnchantments.add(id);
      return id;
   }

   public static int registerToolEnchantment(String stringID, ToolDamageEnchantment enchantment) {
      int id = registerEnchantment(stringID, enchantment);
      toolDamageEnchantments.add(id);
      return id;
   }

   protected void onRegister(ItemEnchantment object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      for (ItemEnchantment element : this.getElements()) {
         element.onEnchantmentRegistryClosed();
      }

      equipmentEnchantments = Collections.unmodifiableSet(equipmentEnchantments);
      meleeItemEnchantments = Collections.unmodifiableSet(meleeItemEnchantments);
      rangedItemEnchantments = Collections.unmodifiableSet(rangedItemEnchantments);
      magicItemEnchantments = Collections.unmodifiableSet(magicItemEnchantments);
      summonItemEnchantments = Collections.unmodifiableSet(summonItemEnchantments);
      toolDamageEnchantments = Collections.unmodifiableSet(toolDamageEnchantments);
   }

   public static ItemEnchantment getEnchantment(int id) {
      try {
         return instance.getElementRaw(id);
      } catch (NoSuchElementException e) {
         return null;
      }
   }

   public static <T extends ItemEnchantment> T getEnchantment(int id, Class<T> expectedClass) {
      ItemEnchantment out = getEnchantment(id);
      return expectedClass.isInstance(out) ? expectedClass.cast(out) : null;
   }

   public static <T extends ItemEnchantment> T getEnchantment(int id, Class<T> expectedClass, T defaultReturn) {
      if (id == 0) {
         return defaultReturn;
      }

      T out = getEnchantment(id, expectedClass);
      return out == null ? defaultReturn : out;
   }

   public static int getEnchantmentID(String stringID) {
      try {
         return instance.getElementIDRaw(stringID);
      } catch (NoSuchElementException e) {
         return -1;
      }
   }

   public static String getEnchantmentStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static ItemEnchantment getEnchantment(String stringID) {
      return getEnchantment(getEnchantmentID(stringID));
   }

   public static <T extends ItemEnchantment> T getEnchantment(String stringID, Class<T> expectedClass) {
      return getEnchantment(getEnchantmentID(stringID), expectedClass);
   }

   public static List<ItemEnchantment> getEnchantments() {
      return instance.streamElements().collect(Collectors.toList());
   }
}
