package necesse.engine.registries;

import java.util.ArrayList;
import java.util.Collections;
import necesse.engine.GameLoadingScreen;
import necesse.engine.journal.JournalEntry;
import necesse.engine.localization.Localization;
import necesse.engine.util.HashMapLinkedList;
import necesse.engine.util.LevelIdentifier;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.level.maps.biomes.Biome;

public class JournalRegistry extends GameRegistry<JournalEntry> {
   public static final JournalRegistry instance = new JournalRegistry();
   private static HashMapLinkedList<Integer, JournalEntry> biomeIDToEntries = new HashMapLinkedList<>();

   private JournalRegistry() {
      super("Biome", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "biomes"));
      JournalEntry forestSurface = registerJournalEntry("forestsurface", new JournalEntry(BiomeRegistry.FOREST, LevelIdentifier.SURFACE_IDENTIFIER));
      forestSurface.addBiomeLootEntry(
         "oaklog", "sprucelog", "apple", "blueberry", "firemone", "gobfish", "halffish", "furfish", "carp", "herring", "mackerel", "salmon", "trout"
      );
      forestSurface.addMobEntries(
         "cow", "bull", "sheep", "ram", "rabbit", "duck", "shark", "fakeshark", "zombie", "zombiearcher", "phoenix", "ashgolem", "stabbybush", "evilsprotector"
      );
      forestSurface.addTreasureEntry(LootTablePresets.surfaceRuinsChest, LootTablePresets.rollingPinDisplayStand);
      forestSurface.addEntryChallenges(JournalChallengeRegistry.FOREST_SURFACE_CHALLENGES_ID);
      JournalEntry forestCave = registerJournalEntry("forestcave", new JournalEntry(BiomeRegistry.FOREST, LevelIdentifier.CAVE_IDENTIFIER));
      forestCave.addBiomeLootEntry("stone", "clay", "copperore", "ironore", "goldore", "sapphire", "salmon", "trout", "carp", "rockfish", "terrorfish");
      forestCave.addMobEntries(
         "zombie",
         "zombiearcher",
         "crawlingzombie",
         "goblin",
         "vampire",
         "cavemole",
         "giantcavespider",
         "trenchcoatgoblinstacked",
         "grizzlybear",
         "cropplerbeet",
         "stonecaveling"
      );
      forestCave.addTreasureEntry(LootTablePresets.basicCaveChest, LootTablePresets.basicCaveRuinsChest, LootTablePresets.caveCryptUniqueItems);
      forestCave.addEntryChallenges(JournalChallengeRegistry.FOREST_CAVES_CHALLENGES_ID);
      JournalEntry forestDeepCave = registerJournalEntry("forestdeepcave", new JournalEntry(BiomeRegistry.FOREST, LevelIdentifier.DEEP_CAVE_IDENTIFIER));
      forestDeepCave.addBiomeLootEntry("deepstone", "copperore", "ironore", "goldore", "obsidian", "tungstenore", "lifequartz", "ruby");
      forestDeepCave.addMobEntries(
         "skeleton", "skeletonthrower", "skeletonminer", "deepcavespirit", "cropplerbeet", "croppleronion", "deepstonecaveling", "reaper"
      );
      forestDeepCave.addTreasureEntry(LootTablePresets.deepCaveChest, LootTablePresets.basicDeepCaveRuinsChest);
      forestDeepCave.addEntryChallenges(JournalChallengeRegistry.FOREST_DEEP_CAVES_CHALLENGES_ID);
      JournalEntry pirateVillage = registerJournalEntry(
         "forestpiratevillage", new JournalEntry(BiomeRegistry.PIRATE_VILLAGE, LevelIdentifier.SURFACE_IDENTIFIER)
      );
      pirateVillage.addBiomeLootEntry("oaklog", "sprucelog", "apple", "blueberry", "firemone");
      pirateVillage.addMobEntries("pirateparrot", "piraterecruit", "piratecaptain");
      pirateVillage.addTreasureEntry(LootTablePresets.pirateChest, LootTablePresets.pirateDisplayStand);
      pirateVillage.addEntryChallenges(JournalChallengeRegistry.PIRATE_VILLAGE_CHALLENGES_ID);
      JournalEntry snowSurface = registerJournalEntry("snowsurface", new JournalEntry(BiomeRegistry.SNOW, LevelIdentifier.SURFACE_IDENTIFIER));
      snowSurface.addBiomeLootEntry("pinelog", "blackberry", "iceblossom", "gobfish", "halffish", "icefish", "carp", "cod", "salmon", "trout");
      snowSurface.addMobEntries("sheep", "ram", "penguin", "snowhare", "duck", "shark", "fakeshark", "polarbear", "zombie", "zombiearcher", "trapperzombie");
      snowSurface.addTreasureEntry(LootTablePresets.surfaceRuinsChest, LootTablePresets.rollingPinDisplayStand);
      snowSurface.addEntryChallenges(JournalChallengeRegistry.SNOW_SURFACE_CHALLENGES_ID);
      JournalEntry snowCave = registerJournalEntry("snowcave", new JournalEntry(BiomeRegistry.SNOW, LevelIdentifier.CAVE_IDENTIFIER));
      snowCave.addBiomeLootEntry("snowstone", "copperore", "ironore", "goldore", "frostshard", "salmon", "trout", "carp", "rockfish", "terrorfish");
      snowCave.addMobEntries(
         "zombie",
         "zombiearcher",
         "trapperzombie",
         "crawlingzombie",
         "cavemole",
         "frozendwarf",
         "frostsentry",
         "goblin",
         "vampire",
         "blackcavespider",
         "cropplerbeet",
         "snowstonecaveling",
         "queenspider"
      );
      snowCave.addTreasureEntry(LootTablePresets.snowCaveChest, LootTablePresets.snowCaveRuinsChest);
      snowCave.addEntryChallenges(JournalChallengeRegistry.SNOW_CAVES_CHALLENGES_ID);
      JournalEntry snowDeepCave = registerJournalEntry("snowdeepcave", new JournalEntry(BiomeRegistry.SNOW, LevelIdentifier.DEEP_CAVE_IDENTIFIER));
      snowDeepCave.addBiomeLootEntry(
         "deepsnowstone", "copperore", "ironore", "goldore", "tungstenore", "lifequartz", "glacialore", "salmon", "trout", "carp", "rockfish", "terrorfish"
      );
      snowDeepCave.addMobEntries(
         "skeleton", "skeletonthrower", "snowwolf", "cryoflake", "ninja", "cropplerbeet", "cropplerpumpkin", "deepsnowstonecaveling", "cryoqueen"
      );
      snowDeepCave.addTreasureEntry(LootTablePresets.deepSnowCaveChest, LootTablePresets.snowDeepCaveRuinsChest, LootTablePresets.stringsVinyls2LootTable);
      snowDeepCave.addEntryChallenges(JournalChallengeRegistry.SNOW_DEEP_CAVES_CHALLENGES_ID);
      JournalEntry dungeon = registerJournalEntry("dungeon", new JournalEntry(BiomeRegistry.DUNGEON));
      dungeon.addMobEntries("enchantedzombie", "enchantedzombiearcher", "enchantedcrawlingzombie", "voidapprentice", "cropplerbeet", "voidwizard");
      dungeon.addTreasureEntry(LootTablePresets.dungeonChest);
      dungeon.addTreasureEntry("bashybush");
      dungeon.addEntryChallenges(JournalChallengeRegistry.DUNGEON_CHALLENGES_ID);
      JournalEntry plainsSurface = registerJournalEntry("plainssurface", new JournalEntry(BiomeRegistry.PLAINS, LevelIdentifier.SURFACE_IDENTIFIER));
      plainsSurface.addBiomeLootEntry(
         "birchlog", "maplelog", "raspberry", "sunflower", "gobfish", "halffish", "furfish", "carp", "herring", "mackerel", "salmon", "trout"
      );
      plainsSurface.addMobEntries(
         "cow", "bull", "sheep", "ram", "rabbit", "duck", "shark", "fakeshark", "zombie", "zombiearcher", "phoenix", "ashgolem", "stabbybush", "evilsprotector"
      );
      plainsSurface.addTreasureEntry(LootTablePresets.surfaceRuinsChest, LootTablePresets.rollingPinDisplayStand);
      plainsSurface.addEntryChallenges(JournalChallengeRegistry.PLAINS_SURFACE_CHALLENGES_ID);
      JournalEntry plainsCave = registerJournalEntry("plainscave", new JournalEntry(BiomeRegistry.PLAINS, LevelIdentifier.CAVE_IDENTIFIER));
      plainsCave.addBiomeLootEntry("granite", "runestone", "copperore", "ironore", "goldore", "salmon", "trout", "carp", "rockfish", "terrorfish");
      plainsCave.addMobEntries(
         "runeboundbrute",
         "runeboundshaman",
         "runeboundtrapper",
         "bonewalker",
         "goblin",
         "trenchcoatgoblinstacked",
         "grizzlybear",
         "cropplerbeet",
         "granitecaveling",
         "chieftain"
      );
      plainsCave.addTreasureEntry(LootTablePresets.plainsCaveChest, LootTablePresets.plainsCaveRuinsChest);
      plainsCave.addEntryChallenges(JournalChallengeRegistry.PLAINS_CAVES_CHALLENGES_ID);
      JournalEntry plainsDeepCave = registerJournalEntry("plainsdeepcave", new JournalEntry(BiomeRegistry.PLAINS, LevelIdentifier.DEEP_CAVE_IDENTIFIER));
      plainsDeepCave.addBiomeLootEntry(
         "basalt",
         "amber",
         "dryadlog",
         "birchlog",
         "maplelog",
         "copperore",
         "ironore",
         "goldore",
         "tungstenore",
         "lifequartz",
         "topaz",
         "salmon",
         "trout",
         "carp",
         "rockfish",
         "terrorfish"
      );
      plainsDeepCave.addMobEntries(
         "forestspector", "dryadsentinel", "spiritghoul", "cropplerbeet", "croppleronion", "cropplerpumpkin", "dryadcaveling", "thecursedcrone"
      );
      plainsDeepCave.addTreasureEntry(LootTablePresets.deepPlainsCaveChest, LootTablePresets.plainsDeepCaveRuinsChest);
      plainsDeepCave.addEntryChallenges(JournalChallengeRegistry.PLAINS_DEEP_CAVES_CHALLENGES_ID);
      JournalEntry swampSurface = registerJournalEntry("swampsurface", new JournalEntry(BiomeRegistry.SWAMP, LevelIdentifier.SURFACE_IDENTIFIER));
      swampSurface.addBiomeLootEntry("willowlog", "cattail", "mushroom", "gobfish", "halffish", "swampfish", "carp", "mackerel", "salmon", "tuna");
      swampSurface.addMobEntries(
         "cow", "bull", "sheep", "ram", "swampslug", "frog", "duck", "shark", "fakeshark", "zombie", "zombiearcher", "swampzombie", "swampslime"
      );
      swampSurface.addTreasureEntry(LootTablePresets.surfaceRuinsChest, LootTablePresets.rollingPinDisplayStand);
      swampSurface.addEntryChallenges(JournalChallengeRegistry.SWAMP_SURFACE_CHALLENGES_ID);
      JournalEntry swampCave = registerJournalEntry("swampcave", new JournalEntry(BiomeRegistry.SWAMP, LevelIdentifier.CAVE_IDENTIFIER));
      swampCave.addBiomeLootEntry("swampstone", "copperore", "ironore", "goldore", "ivyore", "salmon", "carp", "rockfish", "terrorfish");
      swampCave.addMobEntries(
         "frog",
         "zombie",
         "zombiearcher",
         "crawlingzombie",
         "swampzombie",
         "swampslime",
         "swampshooter",
         "goblin",
         "vampire",
         "cavemole",
         "swampcavespider",
         "evilwitch",
         "cropplerbeet",
         "cropplerpotato",
         "swampstonecaveling",
         "swampguardian"
      );
      swampCave.addTreasureEntry(LootTablePresets.swampCaveChest, LootTablePresets.swampCaveRuinsChest, LootTablePresets.evilWitchChest);
      swampCave.addEntryChallenges(JournalChallengeRegistry.SWAMP_CAVES_CHALLENGES_ID);
      JournalEntry swampDeepCave = registerJournalEntry("swampdeepcave", new JournalEntry(BiomeRegistry.SWAMP, LevelIdentifier.DEEP_CAVE_IDENTIFIER));
      swampDeepCave.addBiomeLootEntry("deepswampstone", "copperore", "ironore", "goldore", "tungstenore", "lifequartz", "myceliumore", "emerald");
      swampDeepCave.addMobEntries(
         "ancientskeleton",
         "ancientskeletonthrower",
         "swampskeleton",
         "swampdweller",
         "giantswampslime",
         "smallswampcavespider",
         "staticjellyfish",
         "fishianhookwarrior",
         "fishianhealer",
         "mosquito",
         "agedchampion",
         "cropplerbeet",
         "cropplerpotato",
         "cropplerpumpkin",
         "deepswampstonecaveling",
         "pestwarden"
      );
      swampDeepCave.addTreasureEntry(LootTablePresets.deepSwampCaveChest, LootTablePresets.swampDeepCaveRuinsChest, LootTablePresets.fishianBarrel);
      swampDeepCave.addEntryChallenges(JournalChallengeRegistry.SWAMP_DEEP_CAVES_CHALLENGES_ID);
      JournalEntry desertSurface = registerJournalEntry("desertsurface", new JournalEntry(BiomeRegistry.DESERT, LevelIdentifier.SURFACE_IDENTIFIER));
      desertSurface.addBiomeLootEntry("palmlog", "coconut", "gobfish", "halffish", "carp", "mackerel", "salmon", "tuna");
      desertSurface.addMobEntries("wildostrich", "duck", "shark", "fakeshark", "zombie", "zombiearcher", "mummy");
      desertSurface.addTreasureEntry(LootTablePresets.surfaceRuinsChest, LootTablePresets.rollingPinDisplayStand);
      desertSurface.addEntryChallenges(JournalChallengeRegistry.DESERT_SURFACE_CHALLENGES_ID);
      JournalEntry desertCave = registerJournalEntry("desertcave", new JournalEntry(BiomeRegistry.DESERT, LevelIdentifier.CAVE_IDENTIFIER));
      desertCave.addBiomeLootEntry("sandstone", "copperore", "ironore", "goldore", "quartz", "amethyst", "salmon", "carp", "rockfish", "terrorfish");
      desertCave.addMobEntries("mummy", "mummymage", "sandspirit", "jackal", "cropplerbeet", "cropplercarrot", "sandstonecaveling", "ancientvulture");
      desertCave.addTreasureEntry(LootTablePresets.desertCaveChest, LootTablePresets.desertCaveRuinsChest);
      desertCave.addEntryChallenges(JournalChallengeRegistry.DESERT_CAVES_CHALLENGES_ID);
      JournalEntry desertDeepCave = registerJournalEntry("desertdeepcave", new JournalEntry(BiomeRegistry.DESERT, LevelIdentifier.DEEP_CAVE_IDENTIFIER));
      desertDeepCave.addBiomeLootEntry("deepsandstone", "copperore", "ironore", "goldore", "ancientfossilore", "lifequartz", "myceliumore");
      desertDeepCave.addMobEntries(
         "ancientskeleton",
         "ancientskeletonthrower",
         "desertcrawler",
         "sandworm",
         "cropplerbeet",
         "cropplercarrot",
         "croppleronion",
         "deepsandstonecaveling",
         "sageandgrit"
      );
      desertDeepCave.addTreasureEntry(LootTablePresets.deepDesertCaveChest, LootTablePresets.desertDeepCaveRuinsChest);
      desertDeepCave.addEntryChallenges(JournalChallengeRegistry.DESERT_DEEP_CAVES_CHALLENGES_ID);
      JournalEntry temple = registerJournalEntry("temple", new JournalEntry(BiomeRegistry.TEMPLE));
      temple.addMobEntries("ancientskeleton", "ancientarmoredskeleton", "ancientskeletonthrower", "ancientskeletonmage", "cropplerbeet", "fallenwizard");
      temple.addTreasureEntry(LootTablePresets.templeChest);
      temple.addEntryChallenges(JournalChallengeRegistry.TEMPLE_CHALLENGES_ID);
      JournalEntry forestDeepCaveIncursion = registerJournalEntry(
         "forestdeepcaveincursion", new JournalEntry(BiomeRegistry.FOREST_DEEP_CAVE_INCURSION, IncursionBiomeRegistry.FOREST_DEEP_CAVE_INCURSION)
      );
      forestDeepCaveIncursion.addBiomeLootEntry("deepstone", "tungstenore", "ectoplasm", "shadowessence", "upgradeshard", "alchemyshard", "gatewaytablet");
      forestDeepCaveIncursion.addMobEntries("skeleton", "skeletonthrower", "deepcavespirit", "reaper");
      JournalEntry snowDeepCaveIncursion = registerJournalEntry(
         "snowdeepcaveincursion", new JournalEntry(BiomeRegistry.SNOW_DEEP_CAVE_INCURSION, IncursionBiomeRegistry.SNOW_DEEP_CAVE_INCURSION)
      );
      snowDeepCaveIncursion.addBiomeLootEntry(
         "deepsnowstone",
         "glacialore",
         "glacialshard",
         "cryoessence",
         "upgradeshard",
         "alchemyshard",
         "salmon",
         "trout",
         "carp",
         "rockfish",
         "terrorfish",
         "gatewaytablet"
      );
      snowDeepCaveIncursion.addMobEntries("skeleton", "skeletonthrower", "snowwolf", "cryoflake", "ninja", "cryoqueen");
      JournalEntry swampDeepCaveIncursion = registerJournalEntry(
         "swampdeepcaveincursion", new JournalEntry(BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION, IncursionBiomeRegistry.SWAMP_DEEP_CAVE_INCURSION)
      );
      swampDeepCaveIncursion.addBiomeLootEntry("deepswampstone", "myceliumore", "silk", "bioessence", "upgradeshard", "alchemyshard", "gatewaytablet");
      swampDeepCaveIncursion.addMobEntries(
         "ancientskeleton", "ancientskeletonthrower", "swampskeleton", "swampdweller", "giantswampslime", "smallswampcavespider", "pestwarden"
      );
      JournalEntry desertDeepCaveIncursion = registerJournalEntry(
         "desertdeepcaveincursion", new JournalEntry(BiomeRegistry.DESERT_DEEP_CAVE_INCURSION, IncursionBiomeRegistry.DESERT_DEEP_CAVE_INCURSION)
      );
      desertDeepCaveIncursion.addBiomeLootEntry(
         "deepsandstone", "ancientfossilore", "wormcarapace", "primordialessence", "upgradeshard", "alchemyshard", "gatewaytablet"
      );
      desertDeepCaveIncursion.addMobEntries("ancientskeleton", "ancientskeletonthrower", "desertcrawler", "sandworm", "sageandgrit");
      JournalEntry slimeCaveIncursion = registerJournalEntry(
         "slimecaveincursion", new JournalEntry(BiomeRegistry.SLIME_CAVE, IncursionBiomeRegistry.SLIME_CAVE)
      );
      slimeCaveIncursion.addBiomeLootEntry("slimestone", "slimeum", "slimematter", "slimeessence", "upgradeshard", "alchemyshard", "gatewaytablet");
      slimeCaveIncursion.addMobEntries("warriorslime", "leggedslimethrower", "mageslime", "ghostslime", "slimeworm", "motherslime");
      JournalEntry graveyardIncursion = registerJournalEntry("graveyardincursion", new JournalEntry(BiomeRegistry.GRAVEYARD, IncursionBiomeRegistry.GRAVEYARD));
      graveyardIncursion.addBiomeLootEntry("cryptstone", "nightsteelore", "phantomdust", "bloodessence", "upgradeshard", "alchemyshard", "gatewaytablet");
      graveyardIncursion.addMobEntries("cryptvampire", "cryptbat", "phantom", "nightswarm");
      JournalEntry spiderCastleIncursion = registerJournalEntry(
         "spidercastleincursion", new JournalEntry(BiomeRegistry.SPIDER_CASTLE, IncursionBiomeRegistry.SPIDER_CASTLE)
      );
      spiderCastleIncursion.addBiomeLootEntry("spiderstone", "spideriteore", "spidervenom", "spideressence", "upgradeshard", "alchemyshard", "gatewaytablet");
      spiderCastleIncursion.addMobEntries("spiderkin", "spiderkinwarrior", "spiderkinarcher", "spiderkinmage", "bloatedspider", "webspinner", "spiderempress");
      JournalEntry sunArenaIncursion = registerJournalEntry("sunarenaincursion", new JournalEntry(BiomeRegistry.SUN_ARENA, IncursionBiomeRegistry.SUN_ARENA));
      sunArenaIncursion.addBiomeLootEntry("upgradeshard", "alchemyshard", "gatewaytablet");
      sunArenaIncursion.addMobEntries("sunlightchampion");
      JournalEntry moonArenaIncursion = registerJournalEntry(
         "moonarenaincursion", new JournalEntry(BiomeRegistry.MOON_ARENA, IncursionBiomeRegistry.MOON_ARENA)
      );
      moonArenaIncursion.addBiomeLootEntry("upgradeshard", "alchemyshard", "gatewaytablet");
      moonArenaIncursion.addMobEntries("moonlightdancer");
      JournalEntry crystalHollowIncursion = registerJournalEntry(
         "crystalhollowincursion", new JournalEntry(BiomeRegistry.CRYSTAL_HOLLOW, IncursionBiomeRegistry.CRYSTAL_HOLLOW)
      );
      crystalHollowIncursion.addBiomeLootEntry(
         "crystalstone",
         "omnicrystal",
         "pearlescentdiamond",
         "crystalessence",
         "amethyst",
         "sapphire",
         "emerald",
         "ruby",
         "pearlescentshard",
         "upgradeshard",
         "alchemyshard",
         "salmon",
         "carp",
         "rockfish",
         "terrorfish",
         "gatewaytablet"
      );
      crystalHollowIncursion.addMobEntries("crystalgolem", "crystalarmadillo", "crystaldragon");
      JournalEntry scrapyardIncursion = registerJournalEntry("scrapyardincursion", new JournalEntry(BiomeRegistry.SCRAPYARD, IncursionBiomeRegistry.SCRAPYARD));
      scrapyardIncursion.addBiomeLootEntry(
         "copperore",
         "ironore",
         "pristinegear",
         "fuelskull",
         "radiatedessence",
         "upgradeshard",
         "alchemyshard",
         "salmon",
         "carp",
         "rockfish",
         "terrorfish",
         "gatewaytablet"
      );
      scrapyardIncursion.addMobEntries("mutant", "magechanic", "haywireshredder", "furnacegolem");
   }

   protected void onRegister(JournalEntry object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      biomeIDToEntries = new HashMapLinkedList<>();

      for (JournalEntry element : this.getElements()) {
         biomeIDToEntries.add(element.biome.getID(), element);
      }
   }

   public static JournalEntry registerJournalEntry(String stringID, JournalEntry journalEntry) {
      instance.register(stringID, journalEntry);
      return journalEntry;
   }

   public static Iterable<JournalEntry> getJournalEntries() {
      return instance.getElements();
   }

   public static int getTotalEntryCount() {
      return instance.size();
   }

   public static JournalEntry getJournalEntry(String stringID) {
      return instance.getElement(stringID);
   }

   public static JournalEntry getJournalEntry(int id) {
      return instance.getElement(id);
   }

   public static int getJournalEntryID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getJournalEntryStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static Iterable<JournalEntry> getEntriesForBiome(int biomeID) {
      return biomeIDToEntries.get(biomeID);
   }

   public static String[] getHostileMobStringIDsForBiomeLevel(Biome biome, LevelIdentifier levelIdentifier) {
      Iterable<JournalEntry> entriesForBiome = getEntriesForBiome(biome.getID());
      if (entriesForBiome == null) {
         return null;
      }

      ArrayList<String> result = new ArrayList<>();

      for (JournalEntry journalEntry : entriesForBiome) {
         String[] mobIDs = journalEntry.mobsData
            .stream()
            .filter(item -> !item.mob.isBoss())
            .filter(item -> item.mob.isHostile)
            .map(item -> item.mob.getStringID())
            .toArray(String[]::new);
         if (levelIdentifier.isDeepCave()) {
            Collections.addAll(result, mobIDs);
         } else {
            if (levelIdentifier.isSurface() && journalEntry.levelIdentifier.isSurface()) {
               return mobIDs;
            }

            if (!levelIdentifier.isDeepCave() && !journalEntry.levelIdentifier.isDeepCave()) {
               Collections.addAll(result, mobIDs);
            }
         }
      }

      return result.toArray(new String[0]);
   }
}
