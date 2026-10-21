package necesse.engine.registries;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GameLog;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.PirateVillageJournalBiome;
import necesse.level.maps.biomes.desert.DesertBiome;
import necesse.level.maps.biomes.dungeon.DungeonBiome;
import necesse.level.maps.biomes.forest.ForestBiome;
import necesse.level.maps.biomes.incursions.ArenaBiome;
import necesse.level.maps.biomes.incursions.AscendedVoidBiome;
import necesse.level.maps.biomes.incursions.CrystalHollowBiome;
import necesse.level.maps.biomes.incursions.DesertDeepCaveBiome;
import necesse.level.maps.biomes.incursions.ForestDeepCaveBiome;
import necesse.level.maps.biomes.incursions.GraveyardBiome;
import necesse.level.maps.biomes.incursions.ScrapyardBiome;
import necesse.level.maps.biomes.incursions.SettlementRuinsBiome;
import necesse.level.maps.biomes.incursions.SlimeCaveBiome;
import necesse.level.maps.biomes.incursions.SnowDeepCaveBiome;
import necesse.level.maps.biomes.incursions.SpiderCastleBiome;
import necesse.level.maps.biomes.incursions.SwampDeepCaveBiome;
import necesse.level.maps.biomes.plains.PlainsBiome;
import necesse.level.maps.biomes.snow.SnowBiome;
import necesse.level.maps.biomes.swamp.SwampBiome;
import necesse.level.maps.biomes.temple.TempleBiome;
import necesse.level.maps.biomes.trial.TrialRoomBiome;

public class BiomeRegistry extends GameRegistry<BiomeRegistry.BiomeRegistryElement<?>> {
   public static Biome UNKNOWN;
   public static Biome FOREST;
   public static Biome PLAINS;
   public static Biome DESERT;
   public static Biome SWAMP;
   public static Biome SNOW;
   public static Biome DUNGEON;
   public static Biome PIRATE_VILLAGE;
   public static Biome TEMPLE;
   public static Biome TRIAL_ROOM;
   public static Biome FOREST_DEEP_CAVE_INCURSION;
   public static Biome SNOW_DEEP_CAVE_INCURSION;
   public static Biome SWAMP_DEEP_CAVE_INCURSION;
   public static Biome DESERT_DEEP_CAVE_INCURSION;
   public static Biome SLIME_CAVE;
   public static Biome GRAVEYARD;
   public static Biome SPIDER_CASTLE;
   public static Biome SUN_ARENA;
   public static Biome MOON_ARENA;
   public static Biome CRYSTAL_HOLLOW;
   public static Biome SCRAPYARD;
   public static Biome SETTLEMENT_RUINS;
   public static Biome ASCENDED_VOID;
   private static String[] stringIDs = null;
   private static HashSet<String> statsBiomeStringIDs = new HashSet<>();
   protected static TicketSystemList<Biome> biomeWeights = new TicketSystemList<>();
   public static final BiomeRegistry instance = new BiomeRegistry();

   private BiomeRegistry() {
      super("Biome", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "biomes"));
      UNKNOWN = registerBiome("unknown", new Biome(), false);
      FOREST = registerBiome("forest", new ForestBiome().setGenerationWeight(1.0F), true);
      PLAINS = registerBiome("plains", new PlainsBiome().setGenerationWeight(1.0F), true);
      DESERT = registerBiome("desert", new DesertBiome().setGenerationWeight(3.0F), true);
      SWAMP = registerBiome("swamp", new SwampBiome().setGenerationWeight(3.0F), true);
      SNOW = registerBiome("snow", new SnowBiome().setGenerationWeight(2.5F), true);
      DUNGEON = registerBiome("dungeon", new DungeonBiome(), true);
      PIRATE_VILLAGE = registerBiome("piratevillage", new PirateVillageJournalBiome(), true);
      TEMPLE = registerBiome("temple", new TempleBiome(), true);
      TRIAL_ROOM = registerBiome("trialroom", new TrialRoomBiome(), false);
      FOREST_DEEP_CAVE_INCURSION = registerBiome("forestdeepcave", new ForestDeepCaveBiome(), false);
      SNOW_DEEP_CAVE_INCURSION = registerBiome("snowdeepcave", new SnowDeepCaveBiome(), false);
      SWAMP_DEEP_CAVE_INCURSION = registerBiome("swampdeepcave", new SwampDeepCaveBiome(), false);
      DESERT_DEEP_CAVE_INCURSION = registerBiome("desertdeepcave", new DesertDeepCaveBiome(), false);
      SLIME_CAVE = registerBiome("slimecave", new SlimeCaveBiome(), false);
      GRAVEYARD = registerBiome("graveyard", new GraveyardBiome(), false);
      SPIDER_CASTLE = registerBiome("spidercastle", new SpiderCastleBiome(), false);
      SUN_ARENA = registerBiome("sunarena", new ArenaBiome(), false);
      MOON_ARENA = registerBiome("moonarena", new ArenaBiome(), false);
      CRYSTAL_HOLLOW = registerBiome("crystalhollow", new CrystalHollowBiome(), false);
      SCRAPYARD = registerBiome("scrapyard", new ScrapyardBiome(), false);
      SETTLEMENT_RUINS = registerBiome("settlementruins", new SettlementRuinsBiome(), false);
      ASCENDED_VOID = registerBiome("ascendedvoid", new AscendedVoidBiome(), false);
   }

   @Override
   protected void onRegistryClose() {
      instance.streamElements().map(e -> e.biome).forEach(Biome::updateLocalDisplayName);

      for (BiomeRegistry.BiomeRegistryElement<?> element : this.getElements()) {
         element.biome.onBiomeRegistryClosed();
      }

      statsBiomeStringIDs = new HashSet<>();
      biomeWeights = new TicketSystemList<>();

      for (BiomeRegistry.BiomeRegistryElement<?> element : this.getElements()) {
         if (element.countInStats) {
            statsBiomeStringIDs.add(element.biome.getStringID());
         }

         float weight = element.biome.getBiomeGenerationWeight();
         if (weight > 0.0F) {
            int tickets = (int)(weight * 10000.0F);
            if (tickets > 0) {
               biomeWeights.addObject(tickets, element.biome);
            } else {
               GameLog.warn.println("Biome " + element.biome.getStringID() + " has a weight of " + weight + ", which is too low to generate it.");
            }
         }
      }

      if (biomeWeights.isEmpty()) {
         System.err.println("No biomes found for generation because no biomes have generation weights. Adding forest biome as fallback.");
         biomeWeights.addObject(100, FOREST);
      }

      stringIDs = instance.streamElements().map(e -> e.biome.getStringID()).toArray(String[]::new);
   }

   protected void onRegister(BiomeRegistry.BiomeRegistryElement<?> object, int id, String stringID, boolean isReplace) {
   }

   public static <T extends Biome> T registerBiome(String stringID, T biome, boolean countInStats) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register biomes");
      } else {
         return (T)instance.registerObj(stringID, new BiomeRegistry.BiomeRegistryElement(biome, countInStats)).biome;
      }
   }

   public static List<Biome> getBiomes() {
      return instance.streamElements().map(e -> e.biome).collect(Collectors.toList());
   }

   public static int getTotalBiomes() {
      return instance.size() - 1;
   }

   public static int getTotalStatsBiomes() {
      return statsBiomeStringIDs.size();
   }

   public static Biome getBiome(String stringID) {
      return getBiome(getBiomeID(stringID));
   }

   public static Biome getBiome(int id) {
      return instance.getElement(id).biome;
   }

   public static int getBiomeID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getBiomeIDRaw(String stringID) throws NoSuchElementException {
      return instance.getElementIDRaw(stringID);
   }

   public static String getBiomeStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static String[] getBiomeStringIDs() {
      if (stringIDs == null) {
         throw new IllegalStateException("BiomeRegistry not yet closed");
      } else {
         return stringIDs;
      }
   }

   public static Biome getRandomBiome(GameRandom random) {
      return biomeWeights.getRandomObject(random);
   }

   public static boolean doesBiomeCountInStats(int id) {
      return id == -1 ? false : instance.getElement(id).countInStats;
   }

   protected static class BiomeRegistryElement<T extends Biome> implements IDDataContainer {
      public final T biome;
      public final boolean countInStats;

      public BiomeRegistryElement(T biome, boolean countInStats) {
         this.biome = biome;
         this.countInStats = countInStats;
      }

      @Override
      public IDData getIDData() {
         return this.biome.idData;
      }
   }
}
