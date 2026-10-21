package necesse.engine.world;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import necesse.engine.AreaFinder;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.PointHashSet;
import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPreset;
import necesse.level.maps.biomes.Biome;

public class SpawnTileFinder {
   public static int TOTAL_PRESET_RANGE = 3;
   public static int ATTEMPTS_PER_PRESET_REGION = 50;
   public static Biome SPAWN_BIOME = BiomeRegistry.FOREST;
   public static HashSet<Integer> REQUIRED_NEARBY_BIOMES = new HashSet<>();
   public static HashSet<SpawnTileFinder.RequiredPreset> REQUIRED_NEARBY_PRESETS = new HashSet<>();
   public static int MAX_REGIONS_PER_SECTION = 400;
   public static int MIN_REGIONS_PER_SECTION = 225;
   public static int MIN_DISTANCE_TO_OTHER_BIOMES = 6;
   public static int CLEAR_SPAWN_REGION_RANGE = 7;
   private static final Point[] NEIGHBOR_COORDINATES = new Point[]{new Point(0, -1), new Point(1, 0), new Point(0, 1), new Point(-1, 0)};
   private static final int HALF_REGION_SIZE = 8;

   public static SpawnTileFinder.SpawnTileData findSpawnTile(final WorldEntity worldEntity) {
      final BiomeGeneratorStack generatorStack = worldEntity.getGeneratorStack();
      final GameRandom random = new GameRandom(worldEntity.getWorldSeed());
      final AtomicReference<Point> backupSpawnTile = new AtomicReference<>(null);
      final AtomicReference<Point> spawnTile = new AtomicReference<>(null);
      AreaFinder finder = new AreaFinder(0, 0, TOTAL_PRESET_RANGE) {
         @Override
         public boolean checkPoint(int presetRegionX, int presetRegionY) {
            int startRegionX = presetRegionX * 64 + 32;
            int startRegionY = presetRegionY * 64 + 32;
            LevelPresetsRegion regions = worldEntity.getWorldPresets(startRegionX, startRegionY).getLevelRegions(worldEntity.spawnLevelIdentifier, 0);
            PointHashSet closedRegions = new PointHashSet();

            for (int i = 0; i < SpawnTileFinder.ATTEMPTS_PER_PRESET_REGION; i++) {
               Point regionPosition = regions.getRandomBiomeRegion(random, SpawnTileFinder.SPAWN_BIOME.getID());
               if (regionPosition != null && !closedRegions.contains(regionPosition.x, regionPosition.y)) {
                  if (!SpawnTileFinder.isValidSectionRegion(regionPosition.x, regionPosition.y, regions, generatorStack)) {
                     closedRegions.add(regionPosition.x, regionPosition.y);
                  } else {
                     PointHashSet connectedRegions = new PointHashSet();
                     long totalRegionX = regionPosition.x;
                     long totalRegionY = regionPosition.y;
                     LinkedList<Point> openRegions = new LinkedList<>();
                     openRegions.addLast(regionPosition);
                     connectedRegions.add(regionPosition.x, regionPosition.y);
                     closedRegions.add(regionPosition.x, regionPosition.y);
                     HashSet<Integer> missingRequiredBiomes = new HashSet<>(SpawnTileFinder.REQUIRED_NEARBY_BIOMES);

                     while (!openRegions.isEmpty()) {
                        Point currentRegion = openRegions.removeFirst();

                        for (Point offset : SpawnTileFinder.NEIGHBOR_COORDINATES) {
                           if (connectedRegions.size() >= SpawnTileFinder.MAX_REGIONS_PER_SECTION) {
                              break;
                           }

                           int nextRegionX = currentRegion.x + offset.x;
                           int nextRegionY = currentRegion.y + offset.y;
                           if (!connectedRegions.contains(nextRegionX, nextRegionY) && !closedRegions.contains(nextRegionX, nextRegionY)) {
                              if (SpawnTileFinder.isRegionBiomeAndValid(regions, generatorStack, nextRegionX, nextRegionY, SpawnTileFinder.SPAWN_BIOME.getID())
                                 )
                               {
                                 connectedRegions.add(nextRegionX, nextRegionY);
                                 closedRegions.add(nextRegionX, nextRegionY);
                                 totalRegionX += nextRegionX;
                                 totalRegionY += nextRegionY;
                                 openRegions.addLast(new Point(nextRegionX, nextRegionY));
                              } else {
                                 missingRequiredBiomes.removeIf(
                                    biomeID -> SpawnTileFinder.isRegionBiome(regions, generatorStack, nextRegionX, nextRegionY, biomeID)
                                       && !SpawnTileFinder.isRegionOcean(generatorStack, nextRegionX, nextRegionY)
                                 );
                              }
                           }
                        }

                        if (connectedRegions.size() >= SpawnTileFinder.MAX_REGIONS_PER_SECTION) {
                           break;
                        }
                     }

                     int averageRegionX = (int)(totalRegionX / connectedRegions.size());
                     int averageRegionY = (int)(totalRegionY / connectedRegions.size());
                     if (missingRequiredBiomes.isEmpty()) {
                        if (connectedRegions.size() > SpawnTileFinder.MIN_REGIONS_PER_SECTION) {
                           if (SpawnTileFinder.isValidSectionRegion(averageRegionX, averageRegionY, regions, generatorStack)) {
                              backupSpawnTile.set(
                                 new Point(GameMath.getTileCoordByRegion(averageRegionX) + 8, GameMath.getTileCoordByRegion(averageRegionY) + 8)
                              );
                           }

                           Point foundRegion = SpawnTileFinder.runBreathFirst(
                              averageRegionX,
                              averageRegionY,
                              (regionPos, closed) -> closed.size() >= connectedRegions.size()
                                 ? true
                                 : SpawnTileFinder.isValidSectionRegion(regionPos.x, regionPos.y, regions, generatorStack)
                                    && SpawnTileFinder.runRegionAngleAndCheck(
                                       regionPos.x,
                                       regionPos.y,
                                       8,
                                       2.0F,
                                       (checkRegionX, checkRegionY) -> !SpawnTileFinder.isRegionOcean(generatorStack, checkRegionX, checkRegionY)
                                    )
                                    && SpawnTileFinder.runRegionAngleAndCheck(
                                       regionPos.x,
                                       regionPos.y,
                                       8,
                                       SpawnTileFinder.MIN_DISTANCE_TO_OTHER_BIOMES,
                                       (checkRegionX, checkRegionY) -> SpawnTileFinder.isRegionBiome(
                                          regions, generatorStack, checkRegionX, checkRegionY, SpawnTileFinder.SPAWN_BIOME.getID()
                                       )
                                    )
                                    && SpawnTileFinder.checkNearbyPresets(regionPos.x, regionPos.y, worldEntity)
                           );
                           if (foundRegion != null
                              && SpawnTileFinder.isValidSectionRegion(foundRegion.x, foundRegion.y, regions, generatorStack)
                              && SpawnTileFinder.runRegionAngleAndCheck(
                                 foundRegion.x,
                                 foundRegion.y,
                                 8,
                                 2.0F,
                                 (checkRegionX, checkRegionY) -> !SpawnTileFinder.isRegionOcean(generatorStack, checkRegionX, checkRegionY)
                              )
                              && SpawnTileFinder.runRegionAngleAndCheck(
                                 foundRegion.x,
                                 foundRegion.y,
                                 8,
                                 SpawnTileFinder.MIN_DISTANCE_TO_OTHER_BIOMES,
                                 (checkRegionX, checkRegionY) -> SpawnTileFinder.isRegionBiome(
                                    regions, generatorStack, checkRegionX, checkRegionY, SpawnTileFinder.SPAWN_BIOME.getID()
                                 )
                              )
                              && SpawnTileFinder.checkNearbyPresets(foundRegion.x, foundRegion.y, worldEntity)) {
                              spawnTile.set(new Point(GameMath.getTileCoordByRegion(foundRegion.x) + 8, GameMath.getTileCoordByRegion(foundRegion.y) + 8));
                              return true;
                           }
                        }
                        break;
                     }
                  }
               }
            }

            return false;
         }
      };
      finder.runFinder();
      if (spawnTile.get() != null) {
         return new SpawnTileFinder.SpawnTileData(spawnTile.get(), false, false);
      }

      if (backupSpawnTile.get() != null) {
         return new SpawnTileFinder.SpawnTileData(backupSpawnTile.get(), true, false);
      }

      int startRegionX = 32;
      int startRegionY = 32;
      return new SpawnTileFinder.SpawnTileData(GameMath.getTileCoordByRegion(startRegionX) + 8, GameMath.getTileCoordByRegion(startRegionY) + 8, false, true);
   }

   public static Point runBreathFirst(int startRegionX, int startRegionY, BiPredicate<Point, PointHashSet> onFind) {
      PointHashSet closedRegions = new PointHashSet();
      LinkedList<Point> openRegions = new LinkedList<>();
      openRegions.addLast(new Point(startRegionX, startRegionY));
      closedRegions.add(startRegionX, startRegionY);

      while (!openRegions.isEmpty()) {
         Point currentRegion = openRegions.removeFirst();
         if (onFind.test(currentRegion, closedRegions)) {
            return currentRegion;
         }

         for (Point offset : NEIGHBOR_COORDINATES) {
            int nextRegionX = currentRegion.x + offset.x;
            int nextRegionY = currentRegion.y + offset.y;
            if (!closedRegions.contains(nextRegionX, nextRegionY)) {
               closedRegions.add(nextRegionX, nextRegionY);
               openRegions.addLast(new Point(nextRegionX, nextRegionY));
            }
         }
      }

      return null;
   }

   public static boolean isLevelRegionWithinPresetRegion(int regionX, int regionY, LevelPresetsRegion presetsRegion, int padding) {
      return regionX >= presetsRegion.worldRegion.startLevelRegionX + padding
         && regionX < presetsRegion.worldRegion.startLevelRegionX + 64 - padding
         && regionY >= presetsRegion.worldRegion.startLevelRegionY + padding
         && regionY < presetsRegion.worldRegion.startLevelRegionY + 64 - padding;
   }

   public static boolean runRegionCornerAndCheck(int regionX, int regionY, WorldPreset.ValidTilePredicate isValidTile) {
      int regionStartTileX = GameMath.getTileCoordByRegion(regionX);
      int regionStartTileY = GameMath.getTileCoordByRegion(regionY);
      return isValidTile.isValidPosition(regionStartTileX, regionStartTileY)
         && isValidTile.isValidPosition(regionStartTileX + 16 - 1, regionStartTileY)
         && isValidTile.isValidPosition(regionStartTileX, regionStartTileY + 16 - 1)
         && isValidTile.isValidPosition(regionStartTileX + 16 - 1, regionStartTileY + 16 - 1);
   }

   public static boolean runRegionCornerOrCheck(int regionX, int regionY, WorldPreset.ValidTilePredicate isValidTile) {
      int regionStartTileX = GameMath.getTileCoordByRegion(regionX);
      int regionStartTileY = GameMath.getTileCoordByRegion(regionY);
      return isValidTile.isValidPosition(regionStartTileX, regionStartTileY)
         || isValidTile.isValidPosition(regionStartTileX + 16 - 1, regionStartTileY)
         || isValidTile.isValidPosition(regionStartTileX, regionStartTileY + 16 - 1)
         || isValidTile.isValidPosition(regionStartTileX + 16 - 1, regionStartTileY + 16 - 1);
   }

   public static boolean runRegionAngleAndCheck(int regionX, int regionY, int angleChecks, float range, WorldPreset.ValidRegionPredicate isValidRegion) {
      float anglePerCheck = 360.0F / angleChecks;

      for (int i = 0; i < angleChecks; i++) {
         float angle = anglePerCheck * i;
         Float dir = GameMath.getAngleDir(angle);
         int checkRegionX = Math.round(regionX + dir.x * range);
         int checkRegionY = Math.round(regionY + dir.y * range);
         if (!isValidRegion.isValidRegion(checkRegionX, checkRegionY)) {
            return false;
         }
      }

      return true;
   }

   public static boolean runRegionAngleOrCheck(int regionX, int regionY, int angleChecks, float range, WorldPreset.ValidRegionPredicate isValidRegion) {
      float anglePerCheck = 360.0F / angleChecks;

      for (int i = 0; i < angleChecks; i++) {
         float angle = anglePerCheck * i;
         Float dir = GameMath.getAngleDir(angle);
         int checkRegionX = Math.round(regionX + dir.x * range);
         int checkRegionY = Math.round(regionY + dir.y * range);
         if (isValidRegion.isValidRegion(checkRegionX, checkRegionY)) {
            return true;
         }
      }

      return false;
   }

   public static boolean checkNearbyPresets(int regionX, int regionY, WorldEntity worldEntity) {
      int centerTileX = GameMath.getTileCoordByRegion(regionX) + 8;
      int centerTileY = GameMath.getTileCoordByRegion(regionY) + 8;

      for (SpawnTileFinder.RequiredPreset preset : REQUIRED_NEARBY_PRESETS) {
         LevelPresetsRegion.FoundPresetData closest = worldEntity.findClosestWorldPreset(
            LevelIdentifier.SURFACE_IDENTIFIER,
            pp -> true,
            centerTileX,
            centerTileY,
            preset.maxTileDistance,
            p -> p.getPreset().getStringID().equals(preset.presetStringID)
         );
         if (closest == null) {
            return false;
         }

         double distance = GameMath.getExactDistance(centerTileX, centerTileY, closest.getTileX(), closest.getTileY());
         if (distance < preset.minTileDistance || distance > preset.maxTileDistance) {
            return false;
         }
      }

      return true;
   }

   public static boolean isRegionOcean(BiomeGeneratorStack generatorStack, int regionX, int regionY) {
      return runRegionCornerOrCheck(regionX, regionY, generatorStack::isSurfaceOcean);
   }

   public static boolean isValidSectionRegion(int regionX, int regionY, LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack) {
      return !isRegionOcean(generatorStack, regionX, regionY);
   }

   public static boolean isRegionBiome(LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int regionX, int regionY, int biomeID) {
      if (isLevelRegionWithinPresetRegion(regionX, regionY, presetsRegion, 0)) {
         return presetsRegion.isRegionBiome(regionX, regionY, biomeID);
      }

      int centerTileX = GameMath.getTileCoordByRegion(regionX) + 8;
      int centerTileY = GameMath.getTileCoordByRegion(regionY) + 8;
      return generatorStack.getLazyBiomeID(centerTileX, centerTileY) == biomeID;
   }

   public static boolean isRegionBiomeAndValid(LevelPresetsRegion presetsRegion, BiomeGeneratorStack generatorStack, int regionX, int regionY, int biomeID) {
      if (isLevelRegionWithinPresetRegion(regionX, regionY, presetsRegion, 0)) {
         return presetsRegion.isRegionBiome(regionX, regionY, biomeID) && isValidSectionRegion(regionX, regionY, presetsRegion, generatorStack);
      }

      int centerTileX = GameMath.getTileCoordByRegion(regionX) + 8;
      int centerTileY = GameMath.getTileCoordByRegion(regionY) + 8;
      return generatorStack.getLazyBiomeID(centerTileX, centerTileY) == biomeID && isValidSectionRegion(regionX, regionY, presetsRegion, generatorStack);
   }

   static {
      REQUIRED_NEARBY_BIOMES.add(BiomeRegistry.SNOW.getID());
      REQUIRED_NEARBY_BIOMES.add(BiomeRegistry.PLAINS.getID());
      REQUIRED_NEARBY_PRESETS.add(new SpawnTileFinder.RequiredPreset("dungeonentrance", 250, 800));
   }

   public static class RequiredPreset {
      public String presetStringID;
      public int minTileDistance;
      public int maxTileDistance;

      public RequiredPreset(String presetStringID, int minTileDistance, int maxTileDistance) {
         this.presetStringID = presetStringID;
         this.minTileDistance = minTileDistance;
         this.maxTileDistance = maxTileDistance;
      }
   }

   public static class SpawnTileData {
      public int tileX;
      public int tileY;
      public boolean isBackup;
      public boolean isCompletelyRandom;

      public SpawnTileData(int tileX, int tileY, boolean isBackup, boolean isCompletelyRandom) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.isBackup = isBackup;
         this.isCompletelyRandom = isCompletelyRandom;
      }

      public SpawnTileData(Point point, boolean isBackup, boolean isCompletelyRandom) {
         this(point.x, point.y, isBackup, isCompletelyRandom);
      }

      public Point toPoint() {
         return new Point(this.tileX, this.tileY);
      }
   }
}
