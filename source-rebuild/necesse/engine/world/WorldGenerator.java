package necesse.engine.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import necesse.engine.GameEvents;
import necesse.engine.GlobalData;
import necesse.engine.events.worldGeneration.GeneratedLevelEvent;
import necesse.engine.network.server.Server;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.level.maps.CaveLevel;
import necesse.level.maps.DebugLevel;
import necesse.level.maps.DeepCaveLevel;
import necesse.level.maps.Level;
import necesse.level.maps.SurfaceLevel;
import necesse.level.maps.TemporaryDummyLevel;

public abstract class WorldGenerator {
   private static final List<WorldGenerator> generators = new ArrayList<>();
   private static boolean registryOpen = true;

   public static void registerGenerator(WorldGenerator generator) {
      if (!registryOpen) {
         throw new IllegalStateException("World generator registration is closed");
      }

      Objects.requireNonNull(generator);
      generators.add(0, generator);
   }

   public static void closeRegistry() {
      if (!registryOpen) {
         throw new IllegalStateException("World generator registry already closed");
      }

      registryOpen = false;
   }

   public static Level generateNewLevel(LevelIdentifier levelIdentifier, Server server, GameBlackboard blackboard) {
      Level level = null;

      for (WorldGenerator generator : generators) {
         level = generator.getNewLevel(levelIdentifier, server, blackboard);
         if (level != null) {
            break;
         }
      }

      if (level == null) {
         level = new TemporaryDummyLevel(levelIdentifier, server.world.worldEntity);
      }

      level.makeServerLevel(server);
      level.setWorldEntity(server.world.worldEntity);

      for (WorldGenerator generator : generators) {
         generator.modifyLevel(level, server);
      }

      GameEvents.triggerEvent(new GeneratedLevelEvent(level));
      return level;
   }

   public static long getSeed(LevelIdentifier levelIdentifier) {
      return levelIdentifier.isIslandPosition() ? getSeed(levelIdentifier.getIslandX(), levelIdentifier.getIslandY()) : levelIdentifier.hashCode();
   }

   public static long getSeed(int islandX, int islandY) {
      for (WorldGenerator generator : generators) {
         long seed = generator.islandSeed(islandX, islandY);
         if (seed != 0L) {
            return seed;
         }
      }

      throw new IllegalStateException("Could not get seed");
   }

   public static float getIslandSize(int islandX, int islandY) {
      for (WorldGenerator generator : generators) {
         float size = generator.islandSize(islandX, islandY);
         if (size != 0.0F) {
            return size;
         }
      }

      throw new IllegalStateException("Could not get island size");
   }

   public Level getNewLevel(LevelIdentifier levelIdentifier, Server server, GameBlackboard blackboard) {
      return null;
   }

   public void modifyLevel(Level level, Server server) {
   }

   public float islandSize(int islandX, int islandY) {
      return new GameRandom(getSeed(islandX, islandY)).nextFloat();
   }

   public long islandSeed(int islandX, int islandY) {
      return new GameRandom(islandX * 1289969L + islandY * 888161L).nextLong();
   }

   static {
      registerGenerator(
         new WorldGenerator() {
            @Override
            public Level getNewLevel(LevelIdentifier levelIdentifier, Server server, GameBlackboard blackboard) {
               if (levelIdentifier.equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
                  return new SurfaceLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
               } else if (levelIdentifier.equals(LevelIdentifier.CAVE_IDENTIFIER)) {
                  return new CaveLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
               } else if (levelIdentifier.equals(LevelIdentifier.DEEP_CAVE_IDENTIFIER)) {
                  return new DeepCaveLevel(levelIdentifier, 0, 0, server.world.worldEntity, blackboard.getInt("seed"));
               } else {
                  return GlobalData.isDevMode()
                     ? new DebugLevel(levelIdentifier, 0, 0, server.world.worldEntity, TileRegistry.getTile(levelIdentifier.stringID))
                     : null;
               }
            }
         }
      );
   }
}
