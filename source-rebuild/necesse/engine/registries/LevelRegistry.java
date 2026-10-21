package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.AscendedVoidLevel;
import necesse.level.maps.CaveLevel;
import necesse.level.maps.DebugLevel;
import necesse.level.maps.DeepCaveLevel;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.SurfaceLevel;
import necesse.level.maps.TemporaryDummyLevel;
import necesse.level.maps.biomes.BasicCaveLevel;
import necesse.level.maps.biomes.BasicDeepCaveLevel;
import necesse.level.maps.biomes.BasicSurfaceLevel;
import necesse.level.maps.biomes.desert.DesertCaveLevel;
import necesse.level.maps.biomes.desert.DesertDeepCaveLevel;
import necesse.level.maps.biomes.desert.DesertSurfaceLevel;
import necesse.level.maps.biomes.dungeon.DungeonArenaLevel;
import necesse.level.maps.biomes.dungeon.DungeonLevel;
import necesse.level.maps.biomes.dungeon.DungeonSurfaceLevel;
import necesse.level.maps.biomes.forest.ForestCaveLevel;
import necesse.level.maps.biomes.forest.ForestDeepCaveLevel;
import necesse.level.maps.biomes.forest.ForestSurfaceLevel;
import necesse.level.maps.biomes.plains.PlainsCaveLevel;
import necesse.level.maps.biomes.plains.PlainsDeepCaveLevel;
import necesse.level.maps.biomes.plains.PlainsSurfaceLevel;
import necesse.level.maps.biomes.snow.SnowCaveLevel;
import necesse.level.maps.biomes.snow.SnowDeepCaveLevel;
import necesse.level.maps.biomes.snow.SnowSurfaceLevel;
import necesse.level.maps.biomes.swamp.SwampCaveLevel;
import necesse.level.maps.biomes.swamp.SwampDeepCaveLevel;
import necesse.level.maps.biomes.swamp.SwampSurfaceLevel;
import necesse.level.maps.biomes.temple.TempleArenaLevel;
import necesse.level.maps.biomes.temple.TempleLevel;
import necesse.level.maps.biomes.trial.TrialRoomLevel;
import necesse.level.maps.incursion.CrystalHollowIncursionLevel;
import necesse.level.maps.incursion.DesertDeepCaveIncursionLevel;
import necesse.level.maps.incursion.ForestDeepCaveIncursionLevel;
import necesse.level.maps.incursion.GraveyardIncursionLevel;
import necesse.level.maps.incursion.MoonArenaIncursionLevel;
import necesse.level.maps.incursion.ScrapyardIncursionLevel;
import necesse.level.maps.incursion.SettlementRuinsIncursionLevel;
import necesse.level.maps.incursion.SlimeCaveIncursionLevel;
import necesse.level.maps.incursion.SnowDeepCaveIncursionLevel;
import necesse.level.maps.incursion.SpiderCastleIncursionLevel;
import necesse.level.maps.incursion.SunArenaIncursionLevel;
import necesse.level.maps.incursion.SwampDeepCaveIncursionLevel;

public class LevelRegistry extends ClassedGameRegistry<Level, LevelRegistry.LevelRegistryElement> {
   public static final LevelRegistry instance = new LevelRegistry();

   public LevelRegistry() {
      super("Level", 32762);
   }

   @Override
   public void registerCore() {
      registerLevel("level", Level.class);
      registerLevel("dummy", TemporaryDummyLevel.class);
      registerLevel("debug", DebugLevel.class);
      registerLevel("surface", SurfaceLevel.class);
      registerLevel("cave", CaveLevel.class);
      registerLevel("deepcave", DeepCaveLevel.class);
      registerLevel("basicsurface", BasicSurfaceLevel.class);
      registerLevel("basiccave", BasicCaveLevel.class);
      registerLevel("basicdeepcave", BasicDeepCaveLevel.class);
      registerLevel("forestsurface", ForestSurfaceLevel.class);
      registerLevel("forestcave", ForestCaveLevel.class);
      registerLevel("forestdeepcave", ForestDeepCaveLevel.class);
      registerLevel("plainssurface", PlainsSurfaceLevel.class);
      registerLevel("plainscave", PlainsCaveLevel.class);
      registerLevel("plainsdeepcave", PlainsDeepCaveLevel.class);
      registerLevel("snowsurface", SnowSurfaceLevel.class);
      registerLevel("snowcave", SnowCaveLevel.class);
      registerLevel("snowdeepcave", SnowDeepCaveLevel.class);
      registerLevel("swampsurface", SwampSurfaceLevel.class);
      registerLevel("swampcave", SwampCaveLevel.class);
      registerLevel("swampdeepcave", SwampDeepCaveLevel.class);
      registerLevel("desertsurface", DesertSurfaceLevel.class);
      registerLevel("desertcave", DesertCaveLevel.class);
      registerLevel("desertdeepcave", DesertDeepCaveLevel.class);
      registerLevel("trialroom", TrialRoomLevel.class);
      registerLevel("dungeonsurface", DungeonSurfaceLevel.class);
      registerLevel("dungeon", DungeonLevel.class);
      registerLevel("dungeonarena", DungeonArenaLevel.class);
      registerLevel("temple", TempleLevel.class);
      registerLevel("templearena", TempleArenaLevel.class);
      registerLevel("incursion", IncursionLevel.class);
      registerLevel("forestdeepcaveincursion", ForestDeepCaveIncursionLevel.class);
      registerLevel("snowdeepcaveincursion", SnowDeepCaveIncursionLevel.class);
      registerLevel("swampdeepcaveincursion", SwampDeepCaveIncursionLevel.class);
      registerLevel("desertdeepcaveincursion", DesertDeepCaveIncursionLevel.class);
      registerLevel("slimecaveincursion", SlimeCaveIncursionLevel.class);
      registerLevel("graveyardincursion", GraveyardIncursionLevel.class);
      registerLevel("spidercastleincursion", SpiderCastleIncursionLevel.class);
      registerLevel("sunarenaincursion", SunArenaIncursionLevel.class);
      registerLevel("moonarenaincursion", MoonArenaIncursionLevel.class);
      registerLevel("crystalhollowincursion", CrystalHollowIncursionLevel.class);
      registerLevel("scrapyardincursion", ScrapyardIncursionLevel.class);
      registerLevel("settlementruinsincursion", SettlementRuinsIncursionLevel.class);
      registerLevel("ascendedvoid", AscendedVoidLevel.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerLevel(String stringID, Class<? extends Level> levelClass) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register levels");
      }

      try {
         return instance.register(stringID, new LevelRegistry.LevelRegistryElement(levelClass));
      } catch (NoSuchMethodException e) {
         System.err
            .println(
               "Could not register level "
                  + levelClass.getSimpleName()
                  + ": Missing constructor with parameters: LevelIdentifier, int (width), int (height), WorldEntity"
            );
         return -1;
      }
   }

   public static Level getNewLevel(int id, LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      try {
         return instance.getElement(id).newInstance(identifier, width, height, worldEntity);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         throw new RuntimeException(e);
      }
   }

   public static int getLevelID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getLevelStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static int getLevelID(Class<? extends Level> clazz) {
      return instance.getElementID(clazz);
   }

   public static Stream<String> streamLevelStringIDs() {
      return instance.streamElements().map(IDDataContainer::getStringID);
   }

   protected static class LevelRegistryElement extends ClassIDDataContainer<Level> {
      public LevelRegistryElement(Class<? extends Level> levelClass) throws NoSuchMethodException {
         super(levelClass, LevelIdentifier.class, int.class, int.class, WorldEntity.class);
      }
   }
}
