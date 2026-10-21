package necesse.engine.registries;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Stream;
import necesse.engine.expeditions.CaveExpedition;
import necesse.engine.expeditions.CommonFishingTripExpedition;
import necesse.engine.expeditions.DeepCaveExpedition;
import necesse.engine.expeditions.DeepDesertCaveExpedition;
import necesse.engine.expeditions.DeepPlainsCaveExpedition;
import necesse.engine.expeditions.DeepSnowCaveExpedition;
import necesse.engine.expeditions.DeepSwampCaveExpedition;
import necesse.engine.expeditions.DesertCaveExpedition;
import necesse.engine.expeditions.DungeonExpedition;
import necesse.engine.expeditions.FishingTripExpedition;
import necesse.engine.expeditions.MiningTripExpedition;
import necesse.engine.expeditions.PirateExpedition;
import necesse.engine.expeditions.PlainsCaveExpedition;
import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.expeditions.SnowCaveExpedition;
import necesse.engine.expeditions.SwampCaveExpedition;
import necesse.engine.expeditions.TypesFishingTripExpedition;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;

public class ExpeditionMissionRegistry extends GameRegistry<SettlerExpedition> {
   public static LinkedHashMap<String, GameMessage> categoryDisplayNames = new LinkedHashMap<>();
   public static final ExpeditionMissionRegistry instance = new ExpeditionMissionRegistry();
   public static Set<Integer> explorerExpeditionIDs = new HashSet<>();
   public static Set<Integer> miningTripExpeditionIDs = new HashSet<>();
   public static Set<Integer> fishingTripIDs = new HashSet<>();

   private ExpeditionMissionRegistry() {
      super("Expedition", 32762);
   }

   @Override
   public void registerCore() {
      categoryDisplayNames.put("expedition", new LocalMessage("ui", "expeditionmission"));
      categoryDisplayNames.put("miningtrip", new LocalMessage("ui", "miningtripmission"));
      categoryDisplayNames.put("fishingtrip", new LocalMessage("ui", "fishingtripmission"));
      registerExplorerExpedition("cave", new CaveExpedition());
      registerExplorerExpedition("snowcave", new SnowCaveExpedition());
      registerExplorerExpedition("dungeon", new DungeonExpedition());
      registerExplorerExpedition("plainscave", new PlainsCaveExpedition());
      registerExplorerExpedition("swampcave", new SwampCaveExpedition());
      registerExplorerExpedition("desertcave", new DesertCaveExpedition());
      registerExplorerExpedition("pirate", new PirateExpedition());
      registerExplorerExpedition("deepcave", new DeepCaveExpedition());
      registerExplorerExpedition("deepsnowcave", new DeepSnowCaveExpedition());
      registerExplorerExpedition("deepplainscave", new DeepPlainsCaveExpedition());
      registerExplorerExpedition("deepswampcave", new DeepSwampCaveExpedition());
      registerExplorerExpedition("deepdesertcave", new DeepDesertCaveExpedition());
      registerMiningTrip(
         "copperminingtrip",
         new MiningTripExpedition(
            null, false, 100, 75, 100, "stone", new MiningTripExpedition.OreConfig("copperore", 400), new MiningTripExpedition.OreConfig("sapphire", 0.3F, 200)
         )
      );
      registerMiningTrip(
         "ironminingtrip",
         new MiningTripExpedition(
            null, false, 150, 100, 150, "stone", new MiningTripExpedition.OreConfig("ironore", 400), new MiningTripExpedition.OreConfig("sapphire", 0.5F, 200)
         )
      );
      registerMiningTrip(
         "goldminingtrip",
         new MiningTripExpedition(
            "defeatevilsprotector",
            false,
            200,
            175,
            250,
            "stone",
            new MiningTripExpedition.OreConfig("goldore", 400),
            new MiningTripExpedition.OreConfig("sapphire", 0.7F, 200)
         )
      );
      registerMiningTrip("clayminingtrip", new MiningTripExpedition(null, false, 200, 175, 250, "stone", new MiningTripExpedition.OreConfig("clay", 400)));
      registerMiningTrip("frostshardminingtrip", new MiningTripExpedition("defeatqueenspider", false, 250, 200, 300, "snowstone", "frostshard"));
      registerMiningTrip("runestoneminingtrip", new MiningTripExpedition("defeatchieftain", false, 300, 225, 325, "granite", "runestone"));
      registerMiningTrip("ivyminingtrip", new MiningTripExpedition("defeatswampguardian", false, 350, 250, 350, "swampstone", "ivyore"));
      registerMiningTrip(
         "quartzminingtrip",
         new MiningTripExpedition(
            "defeatancientvulture",
            false,
            450,
            300,
            450,
            "sandstone",
            new MiningTripExpedition.OreConfig("quartz", 400),
            new MiningTripExpedition.OreConfig("amethyst", 0.7F, 200)
         )
      );
      registerMiningTrip("lifequartzminingtrip", new MiningTripExpedition("defeatreaper", true, 600, 400, 600, "deepstone", "lifequartz"));
      registerMiningTrip(
         "tungstenminingtrip",
         new MiningTripExpedition(
            "defeatreaper",
            true,
            600,
            400,
            600,
            "deepstone",
            new MiningTripExpedition.OreConfig("tungstenore", 400),
            new MiningTripExpedition.OreConfig("ruby", 0.7F, 200)
         )
      );
      registerMiningTrip("glacialminingtrip", new MiningTripExpedition("defeatcryoqueen", true, 600, 400, 600, "deepsnowstone", "glacialore"));
      registerMiningTrip(
         "amberminingtrip",
         new MiningTripExpedition(
            "defeatthecursedcrone",
            true,
            600,
            400,
            600,
            "basalt",
            new MiningTripExpedition.OreConfig("amber", 400),
            new MiningTripExpedition.OreConfig("topaz", 0.6F, 200)
         )
      );
      registerMiningTrip(
         "myceliumminingtrip",
         new MiningTripExpedition(
            "defeatpestwarden",
            true,
            600,
            400,
            600,
            "deepswampstone",
            new MiningTripExpedition.OreConfig("myceliumore", 400),
            new MiningTripExpedition.OreConfig("emerald", 0.6F, 200)
         )
      );
      registerMiningTrip("ancientfossilminingtrip", new MiningTripExpedition("defeatsageandgrit", true, 600, 400, 600, "deepsandstone", "ancientfossilore"));
      registerFishingTrip("commonfishtrip", new CommonFishingTripExpedition());
      registerFishingTrip("gobfishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "gobfish", "halffish", "furfish"));
      registerFishingTrip("terrorfishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "terrorfish", "rockfish"));
      registerFishingTrip("halffishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "halffish", "gobfish", "furfish"));
      registerFishingTrip("rockfishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "rockfish", "terrorfish"));
      registerFishingTrip("furfishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "furfish", "gobfish", "halffish"));
      registerFishingTrip("icefishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "icefish", "gobfish", "halffish"));
      registerFishingTrip("swampfishtrip", new TypesFishingTripExpedition(null, 500, 400, 500, "swampfish", "gobfish", "halffish"));
   }

   protected void onRegister(SettlerExpedition object, int id, String stringID, boolean isReplace) {
      object.initDisplayName();
   }

   @Override
   protected void onRegistryClose() {
      for (SettlerExpedition element : this.getElements()) {
         element.onExpeditionMissionRegistryClosed();
      }

      explorerExpeditionIDs = Collections.unmodifiableSet(explorerExpeditionIDs);
      miningTripExpeditionIDs = Collections.unmodifiableSet(miningTripExpeditionIDs);
      fishingTripIDs = Collections.unmodifiableSet(fishingTripIDs);
   }

   public static int registerExplorerExpedition(String stringID, SettlerExpedition expedition) {
      expedition.setCategory("expedition");
      int id = registerExpedition(stringID, expedition);
      explorerExpeditionIDs.add(id);
      return id;
   }

   @Deprecated
   public static int registerMinerExpedition(String stringID, SettlerExpedition expedition) {
      return registerMiningTrip(stringID, expedition);
   }

   public static int registerMiningTrip(String stringID, SettlerExpedition expedition) {
      expedition.setCategory("miningtrip");
      int id = registerExpedition(stringID, expedition);
      miningTripExpeditionIDs.add(id);
      return id;
   }

   public static int registerFishingTrip(String stringID, FishingTripExpedition expedition) {
      expedition.setCategory("fishingtrip");
      int id = registerExpedition(stringID, expedition);
      fishingTripIDs.add(id);
      return id;
   }

   public static int registerExpedition(String stringID, SettlerExpedition expedition) {
      return instance.register(stringID, expedition);
   }

   public static int replaceExpedition(String stringID, SettlerExpedition expedition) {
      return instance.replace(stringID, expedition);
   }

   public static Iterable<SettlerExpedition> getExpeditions() {
      return instance.getElements();
   }

   public static Stream<SettlerExpedition> streamExpeditions() {
      return instance.streamElements();
   }

   public static SettlerExpedition getExpedition(int id) {
      if (id >= instance.size()) {
         id = 0;
      }

      return instance.getElement(id);
   }

   public static int getExpeditionID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static SettlerExpedition getExpedition(String stringID) {
      return instance.getElement(stringID);
   }
}
