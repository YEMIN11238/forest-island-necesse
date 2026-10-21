package necesse.engine.registries;

import java.util.ArrayList;
import java.util.Comparator;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.worldPresets.AbandonedMineWorldPreset;
import necesse.engine.world.worldPresets.AgedChampionWorldPreset;
import necesse.engine.world.worldPresets.AncientVultureArenaWorldPreset;
import necesse.engine.world.worldPresets.BearCaveWorldPreset;
import necesse.engine.world.worldPresets.CavePresetsWorldPreset;
import necesse.engine.world.worldPresets.CaveRuinsWorldPreset;
import necesse.engine.world.worldPresets.CharredForestMiniBiomeWorldPreset;
import necesse.engine.world.worldPresets.ChestRoomWorldPreset;
import necesse.engine.world.worldPresets.ChieftainBossWorldPreset;
import necesse.engine.world.worldPresets.CoreKeeperCoreWorldPreset;
import necesse.engine.world.worldPresets.CursedCroneBossWorldPreset;
import necesse.engine.world.worldPresets.CustomCrystalsWorldPreset;
import necesse.engine.world.worldPresets.DeepCavePresetsWorldPreset;
import necesse.engine.world.worldPresets.DeepSnowCavesMusicPlayerWorldPreset;
import necesse.engine.world.worldPresets.DeepSwampSpiderNestsWorldPreset;
import necesse.engine.world.worldPresets.DesertCavelingOasisWorldPreset;
import necesse.engine.world.worldPresets.DesertTempleWorldPreset;
import necesse.engine.world.worldPresets.DungeonEntranceWorldPreset;
import necesse.engine.world.worldPresets.FishianMiniBiomeWorldPreset;
import necesse.engine.world.worldPresets.ForestLootAreaWorldPreset;
import necesse.engine.world.worldPresets.FrenchHornWorldPreset;
import necesse.engine.world.worldPresets.KatanaRuinsWorldPreset;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.MimeGardenWorldPreset;
import necesse.engine.world.worldPresets.MinecartsWorldPreset;
import necesse.engine.world.worldPresets.PalworldGrizzboltWorldPreset;
import necesse.engine.world.worldPresets.RimworldThrumboDangerWorldPreset;
import necesse.engine.world.worldPresets.RuneboundTombsWorldPreset;
import necesse.engine.world.worldPresets.RuneboundTribeWorldPreset;
import necesse.engine.world.worldPresets.ShrineOfTheMoonWorldPreset;
import necesse.engine.world.worldPresets.SnowCaveIceOpeningsWorldPreset;
import necesse.engine.world.worldPresets.SnowCavesMusicPlayerWorldPreset;
import necesse.engine.world.worldPresets.SnowLootAreaWorldPreset;
import necesse.engine.world.worldPresets.SnowSpiderNestsWorldPreset;
import necesse.engine.world.worldPresets.SpiderNestsWorldPreset;
import necesse.engine.world.worldPresets.SurfacePirateVillageWorldPreset;
import necesse.engine.world.worldPresets.SurfacePresetsWorldPreset;
import necesse.engine.world.worldPresets.SurfaceRuinsWorldPreset;
import necesse.engine.world.worldPresets.SurfaceVillageWorldPreset;
import necesse.engine.world.worldPresets.SwampAncientRootsAreasWorldPreset;
import necesse.engine.world.worldPresets.SwampCaveSporesWorldPreset;
import necesse.engine.world.worldPresets.SwampMudAreasWorldPreset;
import necesse.engine.world.worldPresets.SwampWitchCabinWorldPreset;
import necesse.engine.world.worldPresets.SwampWitchCastleWorldPreset;
import necesse.engine.world.worldPresets.ValheimHuginWorldPreset;
import necesse.engine.world.worldPresets.VampireCryptWorldPreset;
import necesse.engine.world.worldPresets.WorldPreset;

public class WorldPresetRegistry extends GameRegistry<WorldPreset> {
   public static final WorldPresetRegistry instance = new WorldPresetRegistry();
   private static ArrayList<WorldPreset> prioritySortedPresets = new ArrayList<>();

   private WorldPresetRegistry() {
      super("WorldPreset", 32762);
   }

   @Override
   public void registerCore() {
      registerPreset("forestspidernests", new SpiderNestsWorldPreset(BiomeRegistry.FOREST, LevelIdentifier.CAVE_IDENTIFIER, 0.025F, "giantcavespider"));
      registerPreset(
         "forestsapphirecrystals",
         new CustomCrystalsWorldPreset(
            BiomeRegistry.FOREST, LevelIdentifier.CAVE_IDENTIFIER, 0.012F, "sapphiregravel", "sapphireclustersmall", "sapphirecluster"
         )
      );
      registerPreset("forestvampirecrypts", new VampireCryptWorldPreset(BiomeRegistry.FOREST, LevelIdentifier.CAVE_IDENTIFIER, 0.0375F));
      registerPreset("forestbearcave", new BearCaveWorldPreset(BiomeRegistry.FOREST, LevelIdentifier.CAVE_IDENTIFIER, 0.005F));
      registerPreset("snowcaveiceopenings", new SnowCaveIceOpeningsWorldPreset());
      registerPreset("snowspidernests", new SnowSpiderNestsWorldPreset());
      registerPreset("snowvampirecrypts", new VampireCryptWorldPreset(BiomeRegistry.SNOW, LevelIdentifier.CAVE_IDENTIFIER, 0.012F));
      registerPreset("plainsbearcave", new BearCaveWorldPreset(BiomeRegistry.PLAINS, LevelIdentifier.CAVE_IDENTIFIER, 0.005F));
      registerPreset("swampcavespores", new SwampCaveSporesWorldPreset());
      registerPreset("swampmudareas", new SwampMudAreasWorldPreset());
      registerPreset("ancientrootsareas", new SwampAncientRootsAreasWorldPreset());
      registerPreset("swampspidernests", new SpiderNestsWorldPreset(BiomeRegistry.SWAMP, LevelIdentifier.CAVE_IDENTIFIER, 0.025F, "swampcavespider"));
      registerPreset(
         "desertamethystcrystals",
         new CustomCrystalsWorldPreset(
            BiomeRegistry.DESERT, LevelIdentifier.CAVE_IDENTIFIER, 0.012F, "amethystgravel", "amethystclustersmall", "amethystcluster"
         )
      );
      registerPreset(
         "deepforestrubycrystals",
         new CustomCrystalsWorldPreset(BiomeRegistry.FOREST, LevelIdentifier.DEEP_CAVE_IDENTIFIER, 0.012F, "rubygravel", "rubyclustersmall", "rubycluster")
      );
      registerPreset(
         "deepplainstopazcrystals",
         new CustomCrystalsWorldPreset(BiomeRegistry.PLAINS, LevelIdentifier.DEEP_CAVE_IDENTIFIER, 0.012F, "topazgravel", "topazclustersmall", "topazcluster")
      );
      registerPreset("deepswampfishianbiome", new FishianMiniBiomeWorldPreset());
      registerPreset(
         "deepswampemeralcrystals",
         new CustomCrystalsWorldPreset(
            BiomeRegistry.SWAMP, LevelIdentifier.DEEP_CAVE_IDENTIFIER, 0.012F, "emeraldgravel", "emeraldclustersmall", "emeraldcluster"
         )
      );
      registerPreset("deepswampspidernests", new DeepSwampSpiderNestsWorldPreset());
      registerPreset("minecarttracks", new MinecartsWorldPreset());
      registerPreset("dungeonentrance", new DungeonEntranceWorldPreset());
      registerPreset("chieftainarena", new ChieftainBossWorldPreset());
      registerPreset("cursedcronearena", new CursedCroneBossWorldPreset());
      registerPreset("vulturearenas", new AncientVultureArenaWorldPreset());
      registerPreset("deserttemple", new DesertTempleWorldPreset());
      registerPreset("piratevillages", new SurfacePirateVillageWorldPreset());
      registerPreset("surfacevillages", new SurfaceVillageWorldPreset());
      registerPreset("charredforest", new CharredForestMiniBiomeWorldPreset());
      registerPreset("surfaceruins", new SurfaceRuinsWorldPreset());
      registerPreset("swampwitchcabins", new SwampWitchCabinWorldPreset());
      registerPreset("katanaruins", new KatanaRuinsWorldPreset());
      registerPreset("mimegarden", new MimeGardenWorldPreset());
      registerPreset("plainsruneboundtribes", new RuneboundTribeWorldPreset());
      registerPreset("deepforestabandonedmines", new AbandonedMineWorldPreset());
      registerPreset("deepplainsruneboundtombs", new RuneboundTombsWorldPreset());
      registerPreset("chestrooms", new ChestRoomWorldPreset());
      registerPreset("caveruins", new CaveRuinsWorldPreset());
      registerPreset("valheimhugin", new ValheimHuginWorldPreset());
      registerPreset("thrumbodanger", new RimworldThrumboDangerWorldPreset());
      registerPreset("forestlootareas", new ForestLootAreaWorldPreset());
      registerPreset("snowlootareas", new SnowLootAreaWorldPreset());
      registerPreset("swampwitchcastle", new SwampWitchCastleWorldPreset());
      registerPreset("desertcavelingoasis", new DesertCavelingOasisWorldPreset());
      registerPreset("frenchhorn", new FrenchHornWorldPreset());
      registerPreset("snowmusicplayer", new SnowCavesMusicPlayerWorldPreset());
      registerPreset("deepsnowmusicplayer", new DeepSnowCavesMusicPlayerWorldPreset());
      registerPreset("deepswampagedchampion", new AgedChampionWorldPreset());
      registerPreset("corekeepercore", new CoreKeeperCoreWorldPreset());
      registerPreset("palworldgrizzbolt", new PalworldGrizzboltWorldPreset());
      registerPreset("shrineofthemoon", new ShrineOfTheMoonWorldPreset());
      registerPreset("surfacepresets", new SurfacePresetsWorldPreset());
      registerPreset("cavepresets", new CavePresetsWorldPreset());
      registerPreset("deepcavepresets", new DeepCavePresetsWorldPreset());
   }

   protected void onRegister(WorldPreset object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      prioritySortedPresets = new ArrayList<>(instance.size());

      for (WorldPreset element : instance.getElements()) {
         prioritySortedPresets.add(element);
      }

      prioritySortedPresets.sort(Comparator.comparingInt(preset -> -preset.getPriority()));

      for (WorldPreset element : instance.getElements()) {
         element.onRegistryClosed();
      }
   }

   public static <T extends WorldPreset> T registerPreset(String stringID, T preset) {
      instance.register(stringID, preset);
      return preset;
   }

   public static WorldPreset getPreset(String stringID) {
      return instance.getElement(stringID);
   }

   public static void initRegion(LevelPresetsRegion region, int customSeed, PerformanceTimerManager timer) {
      GameRandom random = customSeed != 0
         ? new GameRandom(customSeed).nextSeeded(45)
         : region.worldRegion
            .worldEntity
            .getNewWorldRandom()
            .nextSeeded(region.worldRegion.worldPresetRegionX)
            .nextSeeded(region.worldRegion.worldPresetRegionY)
            .nextSeeded(region.identifier.hashCode());

      for (WorldPreset preset : prioritySortedPresets) {
         GameRandom seededRandom = random.nextSeeded(preset.getStringID().hashCode());
         if (preset.shouldAddToRegion(region)) {
            Performance.record(
               timer, preset.getStringID(), () -> preset.addToRegion(seededRandom, region, region.worldRegion.worldEntity.getGeneratorStack(), timer)
            );
         }
      }

      region.finalizeRegionSetup();
   }
}
