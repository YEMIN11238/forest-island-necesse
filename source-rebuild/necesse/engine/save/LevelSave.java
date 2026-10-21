package necesse.engine.save;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.ModSaveInfo;
import necesse.engine.network.server.Server;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.LevelRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.registries.VersionMigration;
import necesse.engine.save.levelData.LevelEventSave;
import necesse.engine.save.levelData.MobSave;
import necesse.engine.save.levelData.ObjectEntitySave;
import necesse.engine.save.levelData.PickupEntitySave;
import necesse.engine.util.InvalidLevelIdentifierException;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.pickup.PickupEntity;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.layers.LevelLayer;

public class LevelSave {
   public static PerformanceTimerManager debugLoadingPerformance;

   public static Level loadSave(LoadData var0, Server var1) {
      try {
         return Performance.recordConstant(debugLoadingPerformance, "levelLoading", () -> {
            Level var2 = Performance.recordConstant(debugLoadingPerformance, "initial", () -> {
               LevelIdentifier restoredIdentifier = getLevelSaveIdentifier(var0);
               String var3 = var0.getUnsafeString("stringID", null, false);
               if (var3 == null) {
                  var3 = restoreLevelStringID(var0, restoredIdentifier, var1);
                  if (var3 == null) {
                     GameLog.warn.println("Could not recover level type for level with identifier " + restoredIdentifier + ". Using default instead.");
                  } else {
                     GameLog.debug.println("Recovered level type for level with identifier " + restoredIdentifier + ": " + var3);
                  }
               }

               int var4 = var0.getInt("width");
               int var5 = var0.getInt("height");
               Level var6 = null;
               if (var3 != null) {
                  int var7 = LevelRegistry.getLevelID(var3);
                  if (var7 != -1) {
                     var6 = LevelRegistry.getNewLevel(var7, restoredIdentifier, var4, var5, var1.world.worldEntity);
                  }
               }

               if (var6 == null) {
                  var6 = new Level(restoredIdentifier, var4, var5, var1.world.worldEntity);
               }

               var6.makeServerLevel(var1);
               LoadData var12 = var0.getFirstLoadDataByName("MODS");
               if (var12 != null) {
                  var6.lastMods = new ArrayList<>();

                  for (LoadData var9 : var12.getLoadData()) {
                     try {
                        var6.lastMods.add(ModSaveInfo.fromSave(var9));
                     } catch (LoadDataException var11) {
                        GameLog.warn.println("Could not load mod info: " + var11.getMessage());
                     }
                  }
               }

               return var6;
            });
            Performance.recordConstant(debugLoadingPerformance, "applyLoadData", () -> var2.applyLoadData(var0));
            return var2;
         });
      } catch (Exception var3) {
         System.err.println("Level file is corrupt.");
         var3.printStackTrace();
         return null;
      }
   }

   public static SaveData getSave(Level var0) {
      SaveData var1 = new SaveData("LEVEL");
      var1.addSafeString("gameVersion", "1.3.3");
      var1.addUnsafeString("stringID", var0.getStringID());
      LevelIdentifier var2 = var0.getIdentifier();
      var1.addUnsafeString("identifier", var2.stringID);
      if (var2.isIslandPosition()) {
         var1.addPoint("island", new Point(var2.getIslandX(), var2.getIslandY()));
         var1.addInt("dimension", var2.getIslandDimension());
      }

      var1.addInt("width", var0.tileWidth);
      var1.addInt("height", var0.tileHeight);
      SaveData var3 = new SaveData("MODS");

      for (LoadedMod var5 : ModLoader.getEnabledMods()) {
         SaveData var6 = var5.getModSaveInfo().getSaveData();
         var3.addSaveData(var6);
      }

      var1.addSaveData(var3);
      var0.addSaveData(var1);
      return var1;
   }

   public static String restoreLevelStringID(LoadData var0, LevelIdentifier var1, Server var2) {
      int var3 = var1.getIslandDimension();
      if (var3 == 1337) {
         return "debug";
      }

      if (var3 == -100) {
         return "dungeon";
      }

      if (var3 == -101) {
         return "dungeonarena";
      }

      if (var3 == -200) {
         return "temple";
      }

      if (var3 == -201) {
         return "templearena";
      }

      Biome var4 = getLevelSaveBiome(var0);
      if (var4 == null) {
         return null;
      }

      if (var4 != BiomeRegistry.FOREST && var4 != BiomeRegistry.PIRATE_VILLAGE) {
         if (var4 == BiomeRegistry.PLAINS) {
            if (var3 == 0) {
               return "plainssurface";
            }

            if (var3 == -1) {
               return "plainscave";
            }

            if (var3 < -1) {
               return "forestdeepcave";
            }
         } else if (var4 == BiomeRegistry.SNOW) {
            if (var3 == 0) {
               return "snowsurface";
            }

            if (var3 == -1) {
               return "snowcave";
            }

            if (var3 < -1) {
               return "snowdeepcave";
            }
         } else if (var4 == BiomeRegistry.SWAMP) {
            if (var3 == 0) {
               return "swampsurface";
            }

            if (var3 == -1) {
               return "swampcave";
            }

            if (var3 < -1) {
               return "swampdeepcave";
            }
         } else if (var4 == BiomeRegistry.DESERT) {
            if (var3 == 0) {
               return "desertsurface";
            }

            if (var3 == -1) {
               return "desertcave";
            }

            if (var3 < -1) {
               return "desertdeepcave";
            }
         }
      } else {
         if (var3 == 0) {
            return "forestsurface";
         }

         if (var3 == -1) {
            return "forestcave";
         }

         if (var3 < -1) {
            return "forestdeepcave";
         }
      }

      return null;
   }

   public static void addLevelBasics(Level var0, SaveData var1) {
      var1.addBoolean("isCave", var0.isCave);
      var1.addLong("lastWorldTime", var0.lastWorldTime);
      var1.addUnsafeString("biome", var0.baseBiome.getStringID());
      var1.addBoolean("isProtected", var0.isProtected);
      if (var0.fallbackIdentifier != null) {
         var1.addUnsafeString("fallbackIdentifier", var0.fallbackIdentifier.stringID);
         if (var0.fallbackTilePos != null) {
            var1.addPoint("fallbackTilePos", var0.fallbackTilePos);
         }
      }

      if (!var0.childLevels.isEmpty()) {
         var1.addStringHashSet("childLevels", var0.childLevels.stream().map(var0x -> var0x.stringID).collect(Collectors.toCollection(HashSet::new)));
      }

      for (LevelLayer var5 : var0.layers) {
         var5.addSaveData(var1);
      }

      SaveData var8 = new SaveData("GNDData");
      var0.gndData.addSaveData(var8);
      var1.addSaveData(var8);
      SaveData var9 = new SaveData("LEVELDATA");
      var0.levelDataManager.addSaveData(var9);
      var1.addSaveData(var9);
      SaveData var10 = new SaveData("EVENTS");

      for (LevelEvent var6 : var0.entityManager.events.regionList.getInNoRegion()) {
         if (!var6.isOver() && var6.shouldSave()) {
            Point var7 = var6.getSaveToRegionPos();
            if (var7 == null) {
               var10.addSaveData(LevelEventSave.getSave(var6));
            }
         }
      }

      var1.addSaveData(var10);
      var0.returnedItemsManager.addSaveData(var1);
      SaveData var12 = new SaveData("STATS");
      var0.levelStats.addSaveData(var12);
      var1.addSaveData(var12);
   }

   private static boolean placeOldLayerData(Level var0, int[] var1, LevelSave.SetterFunction<Integer> var2) {
      if (var1 != null && var1.length == var0.tileWidth * var0.tileHeight) {
         for (int var3 = 0; var3 < var0.tileWidth; var3++) {
            for (int var4 = 0; var4 < var0.tileHeight; var4++) {
               var2.set(var3, var4, var1[var3 + var4 * var0.tileWidth]);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean placeOldLayerData(Level var0, byte[] var1, LevelSave.SetterFunction<Byte> var2) {
      if (var1 != null && var1.length == var0.tileWidth * var0.tileHeight) {
         for (int var3 = 0; var3 < var0.tileWidth; var3++) {
            for (int var4 = 0; var4 < var0.tileHeight; var4++) {
               var2.set(var3, var4, var1[var3 + var4 * var0.tileWidth]);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean placeOldLayerData(Level var0, boolean[] var1, LevelSave.SetterFunction<Boolean> var2) {
      if (var1 == null) {
         return false;
      }

      if (var1.length != var0.tileWidth * var0.tileHeight) {
         return false;
      }

      for (int var3 = 0; var3 < var0.tileWidth; var3++) {
         for (int var4 = 0; var4 < var0.tileHeight; var4++) {
            var2.set(var3, var4, var1[var3 + var4 * var0.tileWidth]);
         }
      }

      return true;
   }

   private static void loadOldObjectData(Level var0, LoadData var1, int var2, String[] var3, AtomicBoolean var4) {
      if (var1 != null) {
         int[] var5 = var1.getIntArray("objects", null, false);
         if (var5 != null) {
            if (var3 != null) {
               String[] var6 = ObjectRegistry.getObjectStringIDs();
               VersionMigration.convertArray(var5, var3, var6, 0, VersionMigration.oldObjectStringIDs);
            }

            if (placeOldLayerData(var0, var5, (var2x, var3x, var4x) -> var0.objectLayer.setObject(var2, var2x, var3x, var4x)) && !var4.get()) {
               System.out
                  .println(
                     "Migrated "
                        + ObjectLayerRegistry.getLayerStringID(var2)
                        + " object layer on level "
                        + var0.getIdentifier().stringID
                        + " from old level system"
                  );
               var4.set(true);
            }

            placeOldLayerData(
               var0, var1.getByteArray("objectRotations", null, false), (var2x, var3x, var4x) -> var0.objectLayer.setObjectRotation(var2, var2x, var3x, var4x)
            );
            placeOldLayerData(
               var0,
               var1.getSmallBooleanArray("objectIsPlayerPlaced", null, false),
               (var2x, var3x, var4x) -> var0.objectLayer.setIsPlayerPlaced(var2, var2x, var3x, var4x)
            );
         }
      }
   }

   public static void applyLevelBasics(Level var0, LoadData var1) {
      Performance.recordConstant(debugLoadingPerformance, "basic", () -> {
         LevelIdentifier var2 = var0.getIdentifier();
         var0.isCave = var1.getBoolean("isCave", !var2.isIslandPosition() || var2.getIslandDimension() < 0);
         var0.lastWorldTime = var1.getLong("lastWorldTime", 0L, false);
         Biome var3 = getLevelSaveBiome(var1);
         if (var3 != null) {
            var0.baseBiome = var3;
         }

         var0.isProtected = var1.getBoolean("isProtected", false);
         String var4 = var1.getUnsafeString("fallbackIdentifier", null, false);
         if (var4 != null) {
            try {
               var0.fallbackIdentifier = new LevelIdentifier(var4);
               var0.fallbackTilePos = var1.getPoint("fallbackTilePos", null, false);
            } catch (InvalidLevelIdentifierException var6) {
               GameLog.warn.println("Loaded invalid fallback for " + var0.getIdentifier().stringID + ": " + var4);
            }
         }
      });
      var0.childLevels.addAll(getChildLevels(var1));
      if (var0.tileWidth >= 0 && var0.tileHeight >= 0) {
         AtomicBoolean var2 = new AtomicBoolean(false);
         int[] var3 = var1.getIntArray("tiles", null, false);
         if (var3 != null) {
            String[] var4 = var1.getStringArray("tileData", null, false);
            if (var4 != null) {
               String[] var5 = TileRegistry.getTileStringIDs();
               VersionMigration.convertArray(var3, var4, var5, 0, VersionMigration.oldTileStringIDs);
            }

            if (placeOldLayerData(var0, var3, var0.tileLayer::setTile) && !var2.get()) {
               System.out.println("Migrated tiles on level " + var0.getIdentifier().stringID + " from old level system");
               var2.set(true);
            }

            placeOldLayerData(var0, var1.getSmallBooleanArray("tileIsPlayerPlaced", null, false), var0.tileLayer::setIsPlayerPlaced);
         }

         String[] var9 = var1.getStringArray("objectData", null, false);
         loadOldObjectData(var0, var1, 0, var9, var2);

         for (int var10 = 1; var10 < ObjectLayerRegistry.getTotalLayers(); var10++) {
            String var6 = ObjectLayerRegistry.getLayerStringID(var10);
            loadOldObjectData(var0, var1.getFirstLoadDataByName(var6 + "Objects"), var10, var9, var2);
         }

         if (placeOldLayerData(var0, var1.getByteArray("wire", null, false), var0.regionManager::setWireData) && !var2.get()) {
            System.out.println("Migrated wire data on level " + var0.getIdentifier().stringID + " from old level system");
            var2.set(true);
         }

         if (placeOldLayerData(var0, var1.getSmallBooleanArray("tilesProtected", null, false), var0.regionManager::setTileProtected) && !var2.get()) {
            System.out.println("Migrated protected data on level " + var0.getIdentifier().stringID + " from old level system");
            var2.set(true);
         }
      }

      Performance.recordConstant(debugLoadingPerformance, "layers", () -> {
         for (LevelLayer var5 : var0.layers) {
            Performance.recordConstant(debugLoadingPerformance, var5.getStringID(), () -> var5.loadSaveData(var1));
         }
      });
      LoadData var8 = var1.getFirstLoadDataByName("GNDData");
      if (var8 != null) {
         try {
            var0.gndData.applyLoadData(var8);
         } catch (Exception var7) {
            System.err.println("Error loading GNDData for level " + var0.getIdentifier());
            var7.printStackTrace();
         }
      }

      Performance.recordConstant(debugLoadingPerformance, "regions", () -> var0.regionManager.calculateRegions());
      Performance.recordConstant(debugLoadingPerformance, "levelData", () -> {
         LoadData var2 = var1.getFirstLoadDataByName("LEVELDATA");
         if (var2 != null) {
            var0.levelDataManager.loadSaveData(var2);
         } else {
            System.err.println("Could not load any level data for " + var0.getIdentifier());
         }
      });
      Performance.recordConstant(debugLoadingPerformance, "mobs", () -> {
         try {
            for (LoadData var4 : var1.getFirstLoadDataByName("MOBS").getLoadData()) {
               try {
                  Mob var5 = MobSave.loadSave(var4, var0);
                  if (var5 != null) {
                     var0.regionManager.ensureTileIsLoaded(var5.getTileX(), var5.getTileY());
                     var0.entityManager.mobs.addHidden(var5);
                  }
               } catch (Exception var6) {
                  var6.printStackTrace();
               }
            }
         } catch (Exception var7x) {
         }
      });
      Performance.recordConstant(debugLoadingPerformance, "pickupEntities", () -> {
         try {
            for (LoadData var4 : var1.getFirstLoadDataByName("PICKUPENTITIES").getLoadData()) {
               try {
                  PickupEntity var5 = PickupEntitySave.loadSave(var4, var0);
                  if (var5 != null) {
                     var0.regionManager.ensureTileIsLoaded(var5.getTileX(), var5.getTileY());
                     var0.entityManager.pickups.addHidden(var5);
                  }
               } catch (Exception var6) {
                  var6.printStackTrace();
               }
            }
         } catch (Exception var7x) {
         }
      });
      Performance.recordConstant(debugLoadingPerformance, "objectEntities", () -> {
         try {
            for (LoadData var4 : var1.getFirstLoadDataByName("OBJECTENTITIES").getLoadData()) {
               try {
                  ObjectEntity var5 = ObjectEntitySave.loadSave(var4, var0);
                  if (var5 != null) {
                     var0.entityManager.objectEntities.addHidden(var5);
                  }
               } catch (Exception var6) {
                  var6.printStackTrace();
               }
            }
         } catch (Exception var7x) {
         }
      });
      Performance.recordConstant(debugLoadingPerformance, "events", () -> {
         try {
            for (LoadData var4 : var1.getFirstLoadDataByName("EVENTS").getLoadData()) {
               try {
                  LevelEvent var5 = LevelEventSave.loadSaveData(var4, var0);
                  if (var5 != null) {
                     var0.entityManager.events.addHidden(var5);
                  }
               } catch (Exception var6) {
                  var6.printStackTrace();
               }
            }
         } catch (Exception var7) {
            System.err.println("Could not load level events, resulting in a wipe.");
         }
      });
      Performance.recordConstant(debugLoadingPerformance, "returnedItems", () -> {
         try {
            var0.returnedItemsManager.loadSaveData(var1);
         } catch (Exception var3) {
            System.err.println("Could not load returned items, resulting in a wipe.");
         }
      });
      Performance.recordConstant(debugLoadingPerformance, "stats", () -> {
         try {
            LoadData var2 = var1.getFirstLoadDataByName("STATS");
            if (var2 != null) {
               var0.levelStats.applyLoadData(var2);
            }
         } catch (Exception var3) {
            System.err.println("Error loading level stats for " + var0.getIdentifier());
            var3.printStackTrace();
         }
      });
   }

   public static int getMigratedBiomeID(String var0, boolean var1) {
      int var2 = -1;

      try {
         var2 = BiomeRegistry.getBiomeIDRaw(var0);
      } catch (NoSuchElementException var5) {
         String var4 = VersionMigration.tryFixStringID(var0, VersionMigration.oldBiomeStringIDs);
         if (!var0.equals(var4)) {
            if (var1) {
               System.out.println("Migrated biome from " + var0 + " to " + var4);
            }

            var2 = BiomeRegistry.getBiomeID(var4);
         }
      }

      return var2;
   }

   public static Biome getLevelSaveBiomeRaw(LoadData var0, boolean var1) {
      String var2 = var0.getUnsafeString("biome");
      int var3 = getMigratedBiomeID(var2, var1);
      return BiomeRegistry.getBiome(var3);
   }

   public static Biome getLevelSaveBiome(LoadData var0) {
      try {
         return getLevelSaveBiomeRaw(var0, true);
      } catch (Exception var2) {
         System.err.println("Could not load level biome");
         return null;
      }
   }

   public static LevelIdentifier getLevelSaveIdentifier(LoadData var0) {
      LevelIdentifier var1;
      try {
         var1 = new LevelIdentifier(var0.getUnsafeString("identifier", null, false));
      } catch (InvalidLevelIdentifierException var5) {
         Point var3 = var0.getPoint("island");
         int var4 = var0.getInt("dimension");
         var1 = new LevelIdentifier(var3.x, var3.y, var4);
      }

      return var1;
   }

   public static HashSet<LevelIdentifier> getChildLevels(LoadData var0) {
      HashSet var1 = new HashSet<>();
      HashSet<String> var2 = var0.getStringHashSet("childLevels", null, false);
      if (var2 != null) {
         for (String var4 : var2) {
            try {
               var1.add(new LevelIdentifier(var4));
            } catch (Exception var6) {
            }
         }
      }

      return var1;
   }

   private interface SetterFunction<T> {
      void set(int var1, int var2, T var3);
   }
}
