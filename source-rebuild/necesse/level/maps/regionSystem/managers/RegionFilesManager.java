package necesse.level.maps.regionSystem.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.network.server.AbstractSaveHandler;
import necesse.engine.network.server.SaveHandlerList;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerSaveHandler;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PointHashMap;
import necesse.engine.world.WorldFile;
import necesse.engine.world.WorldFileSystem;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionManager;
import necesse.level.maps.regionSystem.regionsStructure.RegionStructureDataAbstract;

public class RegionFilesManager {
   public static final int WORLD_FILE_REGION_SIZE_BITS = 6;
   public static final int WORLD_FILE_REGION_SIZE = 64;
   public static final int WORLD_FILE_REGION_SIZE_HALF = 32;
   public final Object lock = new Object();
   public final RegionManager manager;
   public final Level level;
   protected HashMap<Long, RegionFilesManager.WorldRegionData> loadedCache = new HashMap<>();
   protected boolean initializedServerLevel;

   public static int getWorldRegionPos(int regionPos) {
      return GameMath.divideByPowerOf2RoundedDown(regionPos - 32, 6);
   }

   public RegionFilesManager(RegionManager manager) {
      this.manager = manager;
      this.level = manager.level;
   }

   public void makeServerLevel() {
      if (!this.initializedServerLevel) {
         this.manager.forEachLoadedRegions(region -> this.getWorldRegion(region.regionX, region.regionY, false));
         this.initializedServerLevel = true;
      }
   }

   public boolean loadRegion(Region region) {
      synchronized (this.lock) {
         Server server = this.level.getServer();
         if (server != null) {
            RegionFilesManager.WorldRegionData cache = this.getWorldRegion(region.regionX, region.regionY, false);

            try {
               LoadData save = cache.getSaveData(region.regionX, region.regionY);
               if (save != null) {
                  boolean recordConstant = this.level.debugLoadingPerformance != null;
                  PerformanceTimerManager tickManager = this.level.debugLoadingPerformance != null
                     ? this.level.debugLoadingPerformance
                     : this.level.tickManager();
                  region.loadSaveData(save, tickManager, recordConstant);
                  return true;
               }
            } catch (Exception e) {
               System.err.println("Could not load region from file at " + region.regionX + "x" + region.regionY);
               e.printStackTrace();
            }

            server.printMigrationIfShould();
         }

         return false;
      }
   }

   public void onUnloaded(Region region) {
      if (this.level.isServer()) {
         synchronized (this.lock) {
            RegionFilesManager.WorldRegionData cache = this.getWorldRegion(region.regionX, region.regionY, true);
            cache.setNewRegionSaveData(region.regionX, region.regionY, region::addSaveData);
            cache.loadedRegions.remove(GameMath.getUniqueLongKey(region.regionX, region.regionY));
            if (cache.loadedRegions.isEmpty()) {
               if (cache.isBeingSaved) {
                  cache.shouldDispose = true;
               } else {
                  System.out
                     .println(
                        "Unloading "
                           + this.level.getIdentifier()
                           + " world region "
                           + cache.worldRegionX
                           + "x"
                           + cache.worldRegionY
                           + " from memory to file system"
                     );
                  cache.saveToFile(this.level.getServer(), this.level);
                  this.loadedCache.remove(GameMath.getUniqueLongKey(cache.worldRegionX, cache.worldRegionY));
               }
            }
         }
      }
   }

   public void updateSaveFile(Region region) {
      if (this.level.isServer()) {
         synchronized (this.lock) {
            RegionFilesManager.WorldRegionData cache = this.getWorldRegion(region.regionX, region.regionY, true);
            cache.setNewRegionSaveData(region.regionX, region.regionY, region::addSaveData);
         }
      }
   }

   protected RegionFilesManager.WorldRegionData getWorldRegion(int regionX, int regionY, boolean expectedExists) {
      return this.getWorldRegion(regionX, regionY, expectedExists, true);
   }

   protected RegionFilesManager.WorldRegionData getWorldRegion(int regionX, int regionY, boolean expectedExists, boolean loadRegion) {
      int worldRegionX = getWorldRegionPos(regionX);
      int worldRegionY = getWorldRegionPos(regionY);
      long worldUniqueKey = GameMath.getUniqueLongKey(worldRegionX, worldRegionY);
      RegionFilesManager.WorldRegionData cache = this.loadedCache.get(worldUniqueKey);
      if (cache == null) {
         if (expectedExists) {
            System.err
               .println(
                  "Error trying to find world cache for "
                     + this.level.getIdentifier()
                     + " region "
                     + regionX
                     + "x"
                     + regionY
                     + ", World region: "
                     + worldRegionX
                     + "x"
                     + worldRegionY
                     + ", "
                     + this.level.getHostString()
               );
            new Throwable().printStackTrace(System.err);
         }

         if (this.level.isServer()) {
            Server server = this.level.getServer();
            WorldFileSystem fileSystem = server.world.fileSystem;
            if (fileSystem.worldRegionFileExists(this.level.getIdentifier(), worldRegionX, worldRegionY)) {
               WorldFile file = fileSystem.getWorldRegionFile(this.level.getIdentifier(), worldRegionX, worldRegionY);
               if (expectedExists) {
                  WorldFile backupFile = fileSystem.getBackupWorldRegionFile(this.level.getIdentifier(), worldRegionX, worldRegionY);
                  System.err.println("Trying to make a backup of the world file to " + backupFile.getFileName());

                  try {
                     file.copyTo(backupFile);
                  } catch (IOException e) {
                     System.err.println("Error making backup:");
                     e.printStackTrace();
                  }
               }

               LoadData save;
               try {
                  save = new LoadData(file);
               } catch (Exception e) {
                  save = new SaveData("").toLoadData();
                  System.err
                     .println(
                        "Could not load "
                           + this.level.getIdentifier()
                           + " region from file at "
                           + regionX
                           + "x"
                           + regionY
                           + ", World region: "
                           + worldRegionX
                           + "x"
                           + worldRegionY
                     );
                  e.printStackTrace();
               }

               cache = new RegionFilesManager.WorldRegionData(this, worldRegionX, worldRegionY, save);
            }
         }

         if (cache == null) {
            cache = new RegionFilesManager.WorldRegionData(this, worldRegionX, worldRegionY, new SaveData("").toLoadData());
         }

         synchronized (this.lock) {
            this.loadedCache.put(worldUniqueKey, cache);
         }
      } else {
         cache.shouldDispose = false;
      }

      if (loadRegion) {
         cache.loadedRegions.add(GameMath.getUniqueLongKey(regionX, regionY));
      }

      return cache;
   }

   public void saveAll() {
      if (this.level.isServer()) {
         synchronized (this.lock) {
            for (RegionFilesManager.WorldRegionData data : this.loadedCache.values()) {
               data.saveToFile(this.level.getServer(), this.level);
            }
         }
      }
   }

   public AbstractSaveHandler getSaveHandler(PerformanceTimerManager performanceTimer, Server server, RegionStructureDataAbstract regions) {
      SaveHandlerList saveHandlerList = new SaveHandlerList(performanceTimer, "worldRegions", 10);
      PointHashMap<RegionFilesManager.WorldRegionSaveHandler> regionHandlers = new PointHashMap<>();

      for (Region region : regions) {
         RegionFilesManager.WorldRegionData cache = this.getWorldRegion(region.regionX, region.regionY, true);
         regionHandlers.compute(cache.worldRegionX, cache.worldRegionY, (x, y, lastValue) -> {
            if (lastValue == null) {
               lastValue = new RegionFilesManager.WorldRegionSaveHandler(performanceTimer, server, this, cache);
               saveHandlerList.addHandler(lastValue);
            }

            lastValue.regionsToSave.add(region);
            return lastValue;
         });
      }

      return saveHandlerList;
   }

   public void serverTick() {
      if (this.level.isServer()) {
         synchronized (this.lock) {
            if (this.initializedServerLevel) {
               for (RegionFilesManager.WorldRegionData value : this.loadedCache.values()) {
                  value.serverTick(this.level.getServer(), this.level);
               }
            }
         }
      }
   }

   public void deleteLevelFiles() {
      Server server = this.level.getServer();
      WorldFileSystem fileSystem = server.world.fileSystem;

      try {
         fileSystem.deleteAllLevelFiles(this.level.getIdentifier());
         SettlementsWorldData.getSettlementsData(server).deleteSettlementsAt(this.level.getIdentifier());
      } catch (IOException e) {
         System.err.println("Error deleting files for level " + this.level.getIdentifier());
      }
   }

   public boolean isRegionGenerated(int regionX, int regionY) {
      RegionFilesManager.WorldRegionData cache = this.getWorldRegion(regionX, regionY, false, false);
      return cache.getSaveData(regionX, regionY) != null;
   }

   protected static class WorldRegionData {
      protected RegionFilesManager manager;
      protected final int worldRegionX;
      protected final int worldRegionY;
      protected HashSet<Long> loadedRegions = new HashSet<>();
      protected PointHashMap<LoadData> regionSaveData = new PointHashMap<>();
      protected LoadData masterSaveData;
      protected boolean shouldDispose;
      protected boolean isBeingSaved;
      protected int saveRegionsFileBuffer = GameRandom.globalRandom.nextInt(1200);

      public WorldRegionData(RegionFilesManager manager, int worldRegionX, int worldRegionY, LoadData save) {
         this.manager = manager;
         this.worldRegionX = worldRegionX;
         this.worldRegionY = worldRegionY;
         Objects.requireNonNull(save);
         this.masterSaveData = save;
      }

      public LoadData getSaveData(int regionX, int regionY) {
         synchronized (this.manager.lock) {
            LoadData save = this.regionSaveData.get(regionX, regionY);
            if (save == null) {
               save = this.masterSaveData.getFirstLoadDataByName(regionX + "x" + regionY);
               if (save != null) {
                  this.regionSaveData.put(regionX, regionY, save);
               }
            }

            return save;
         }
      }

      public void setNewRegionSaveData(int regionX, int regionY, Consumer<SaveData> saveHandler) {
         synchronized (this.manager.lock) {
            LoadData loadData = this.regionSaveData.get(regionX, regionY);
            if (loadData != null) {
               SaveData regionSave = loadData.toSaveData();
               regionSave.clearComponents();
               saveHandler.accept(regionSave);
            } else {
               SaveData regionSave = new SaveData(regionX + "x" + regionY);
               saveHandler.accept(regionSave);
               this.regionSaveData.put(regionX, regionY, regionSave.toLoadData());
               this.masterSaveData.toSaveData().addSaveData(regionSave);
            }
         }
      }

      public void serverTick(Server server, Level level) {
         this.saveRegionsFileBuffer++;
         if (this.saveRegionsFileBuffer >= 1200) {
            this.saveRegionsFileBuffer = 0;
            Performance.record(level.tickManager(), "regionFileSave", () -> this.saveToFile(server, level));
         }
      }

      public void saveToFile(Server server, Level level) {
         synchronized (this.manager.lock) {
            WorldFile file = server.world.fileSystem.getWorldRegionFile(level.getIdentifier(), this.worldRegionX, this.worldRegionY);
            this.masterSaveData.toSaveData().saveScript(file);
         }
      }
   }

   protected static class WorldRegionSaveHandler extends AbstractSaveHandler {
      private final Server server;
      private final RegionFilesManager manager;
      private final RegionFilesManager.WorldRegionData worldRegion;
      private final ArrayList<Region> regionsToSave = new ArrayList<>();
      private int currentRegionsTick = 0;
      private boolean savedToFile;

      public WorldRegionSaveHandler(
         PerformanceTimerManager performanceTimer, Server server, RegionFilesManager manager, RegionFilesManager.WorldRegionData worldRegion
      ) {
         super(performanceTimer, "worldRegion" + worldRegion.worldRegionX + "x" + worldRegion.worldRegionY);
         this.server = server;
         this.manager = manager;
         this.worldRegion = worldRegion;
         worldRegion.isBeingSaved = true;
      }

      @Override
      public boolean tickInternal() throws IOException {
         synchronized (this.manager.lock) {
            if (this.currentRegionsTick >= 0 && !this.regionsToSave.isEmpty()) {
               int regionsPerTick = ServerSaveHandler.regionsPerTickOnClient;
               if (this.server.getLocalClient() == null) {
                  regionsPerTick = ServerSaveHandler.regionsPerTickOnServer;
               }

               for (int i = 0; i < regionsPerTick; i++) {
                  Region region = this.regionsToSave.get(this.currentRegionsTick);
                  if (!region.isDisposed()) {
                     this.worldRegion.setNewRegionSaveData(region.regionX, region.regionY, region::addSaveData);
                  }

                  this.currentRegionsTick++;
                  if (this.currentRegionsTick >= this.regionsToSave.size()) {
                     this.currentRegionsTick = -1;
                     break;
                  }
               }

               return false;
            } else if (!this.savedToFile) {
               this.savedToFile = true;
               this.worldRegion.saveToFile(this.server, this.manager.level);
               this.worldRegion.isBeingSaved = false;
               if (this.worldRegion.shouldDispose) {
                  this.manager.loadedCache.remove(GameMath.getUniqueLongKey(this.worldRegion.worldRegionX, this.worldRegion.worldRegionY));
                  System.out
                     .println(
                        "Unloading "
                           + this.manager.level.getIdentifier()
                           + " world region "
                           + this.worldRegion.worldRegionX
                           + "x"
                           + this.worldRegion.worldRegionY
                           + " from memory to file system post save"
                     );
               }

               return false;
            } else {
               return true;
            }
         }
      }
   }
}
