package necesse.engine.world.worldData;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.network.PacketReader;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketRemoveSettlementData;
import necesse.engine.network.packet.PacketRequestSettlementData;
import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.engine.world.OneWorldMigration;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldEntityGameClock;
import necesse.engine.world.WorldFile;
import necesse.entity.manager.WorldLevelUnloadedEntityComponent;
import necesse.entity.manager.WorldRegionLoadedEntityComponent;
import necesse.entity.manager.WorldRegionUnloadedEntityComponent;
import necesse.entity.manager.WorldSavedEntityComponent;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;
import necesse.level.maps.regionSystem.Region;

public class SettlementsWorldData
   extends WorldData
   implements WorldSavedEntityComponent,
   WorldRegionLoadedEntityComponent,
   WorldRegionUnloadedEntityComponent,
   WorldLevelUnloadedEntityComponent {
   private final HashMap<LevelIdentifier, SettlementsWorldData.SettlementsLevelRegionData> regionData = new HashMap<>();
   private final HashMap<Integer, SettlementsWorldData.SettlementData> settlements = new HashMap<>();
   private final HashMap<Integer, SettlementsWorldData.SettlementData> loadedSettlements = new HashMap<>();
   private final TreeSet<Integer> clientRequestQueue = new TreeSet<>();
   private final HashMap<Integer, Long> clientRequestedTimes = new HashMap<>();

   @Override
   public void onWorldSaved() {
      synchronized (this) {
         this.saveSettlements();
      }
   }

   @Override
   public void onLevelRegionLoaded(Level level, Region region) {
      synchronized (this) {
         if (level.isServer()) {
            SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(level.getIdentifier());
            int settlementUniqueID = regionData.regionSettlements.getOrDefault(region.regionX, region.regionY, 0);
            if (settlementUniqueID != 0) {
               this.getOrLoadServerData(settlementUniqueID);
            }
         }
      }
   }

   @Override
   public void onLevelRegionUnloaded(Level level, Region region) {
      synchronized (this) {
         SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(level.getIdentifier());
         int settlementUniqueID = regionData.regionSettlements.getOrDefault(region.regionX, region.regionY, 0);
         if (settlementUniqueID != 0) {
            NetworkSettlementData settlement = this.getNetworkData(settlementUniqueID);
            if (settlement != null) {
               Iterable<Point> regionPositions = SettlementBoundsManager.getRegionPositions(
                  settlement.getTileX(), settlement.getTileY(), settlement.getFlagTier()
               );
               boolean hasAnyRegionsLoaded = false;

               for (Point regionPosition : regionPositions) {
                  if (level.regionManager.isRegionLoaded(regionPosition.x, regionPosition.y)) {
                     hasAnyRegionsLoaded = true;
                     break;
                  }
               }

               if (!hasAnyRegionsLoaded) {
                  System.out
                     .println(
                        level.getHostString()
                           + " unloading settlement with uniqueID: "
                           + settlementUniqueID
                           + " at "
                           + settlement.getTileX()
                           + "x"
                           + settlement.getTileY()
                           + " because no regions are loaded within the settlement"
                     );
                  this.unloadSettlement(settlementUniqueID, true);
               }
            }
         }
      }
   }

   @Override
   public void onLevelUnloaded(LevelIdentifier identifier) {
      synchronized (this) {
         HashSet<Integer> unloadUniqueIDs = new HashSet<>();

         for (SettlementsWorldData.SettlementData value : this.loadedSettlements.values()) {
            if (value.networkData.level.getIdentifier().equals(identifier)) {
               unloadUniqueIDs.add(value.networkData.uniqueID);
            }
         }

         for (int settlementUniqueID : unloadUniqueIDs) {
            this.unloadSettlement(settlementUniqueID, true);
         }
      }
   }

   public void saveSettlements() {
      for (SettlementsWorldData.SettlementData settlementData : this.loadedSettlements.values()) {
         this.saveSettlementData(settlementData.networkData, settlementData.serverData);
      }
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      synchronized (this) {
         SaveData settlementsSave = new SaveData("SETTLEMENTS");

         for (SettlementsWorldData.SettlementData data : this.settlements.values()) {
            CachedSettlementData cacheData;
            if (data.networkData != null) {
               cacheData = data.networkData.getCacheData(false);
            } else {
               cacheData = data.cachedData;
            }

            settlementsSave.addSaveData(this.getCacheSave(cacheData));
         }

         if (!settlementsSave.isEmpty()) {
            save.addSaveData(settlementsSave);
         }
      }
   }

   private SaveData getCacheSave(CachedSettlementData cacheData) {
      SaveData settlementSave = new SaveData("SETTLEMENT");
      settlementSave.addUnsafeString("levelIdentifier", cacheData.levelIdentifier.stringID);
      settlementSave.addInt("uniqueID", cacheData.uniqueID);
      cacheData.addSaveData(settlementSave);
      return settlementSave;
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      LoadData settlementsSave = save.getFirstLoadDataByName("SETTLEMENTS");
      if (settlementsSave != null) {
         for (LoadData settlementSave : settlementsSave.getLoadDataByName("SETTLEMENT")) {
            try {
               String levelIdentifierString = settlementSave.getUnsafeString("levelIdentifier", null, false);
               if (levelIdentifierString == null) {
                  throw new LoadDataException("Found invalid settlement levelIdentifier");
               }

               LevelIdentifier levelIdentifier = new LevelIdentifier(levelIdentifierString);
               int settlementUniqueID = settlementSave.getInt("uniqueID", 0, false);
               if (settlementUniqueID == 0) {
                  throw new LoadDataException("Found invalid settlement uniqueID");
               }

               SettlementsWorldData.SettlementData settlementData = new SettlementsWorldData.SettlementData();
               settlementData.cachedData = new CachedSettlementData(levelIdentifier, settlementUniqueID, settlementSave);
               this.settlements.put(settlementUniqueID, settlementData);
               Iterable<Point> regions = SettlementBoundsManager.getRegionPositions(
                  settlementData.cachedData.getTileX(), settlementData.cachedData.getTileY(), settlementData.cachedData.getFlagTier()
               );
               SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(levelIdentifier);
               boolean settlementIsInvalid = false;
               Iterator var12 = regions.iterator();

               while (true) {
                  if (var12.hasNext()) {
                     Point region = (Point)var12.next();
                     int foundSettlementUniqueID = regionData.regionSettlements.getOrDefault(region.x, region.y, 0);
                     if (foundSettlementUniqueID == 0) {
                        continue;
                     }

                     GameLog.warn.println("Found overlapping settlement on region " + region.x + "x" + region.y);
                     String loadedShouldDisband = this.loadShouldSettlementDisbandOnLoading(settlementUniqueID);
                     if (loadedShouldDisband != null) {
                        GameLog.warn
                           .println("Removed newly loaded settlement overlapping with another settlement: " + settlementUniqueID + ", " + loadedShouldDisband);
                        this.removeSettlementOnLoading(settlementData.cachedData);
                        settlementIsInvalid = true;
                     } else {
                        String foundShouldDisband = this.loadShouldSettlementDisbandOnLoading(foundSettlementUniqueID);
                        if (foundShouldDisband == null) {
                           continue;
                        }

                        GameLog.warn
                           .println(
                              "Removed already loaded settlement overlapping with another settlement: " + foundSettlementUniqueID + ", " + foundShouldDisband
                           );
                        this.removeSettlementOnLoading(foundSettlementUniqueID);
                     }
                  }

                  if (!settlementIsInvalid) {
                     this.updateSettlement(settlementData.cachedData);
                  }
                  break;
               }
            } catch (Exception e) {
               System.err.println("Could not load settlement cache from save:");
               e.printStackTrace();
            }
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      synchronized (this) {
         HashSet<Integer> uniqueIDsToUnload = new HashSet<>();
         LinkedList<SettlementsWorldData.SettlementData> settlementsToDisband = new LinkedList<>();

         for (SettlementsWorldData.SettlementData settlementData : this.loadedSettlements.values()) {
            if (settlementData.networkData.tickIsRegionsUnloadedCheck()) {
               uniqueIDsToUnload.add(settlementData.networkData.uniqueID);
            } else {
               settlementData.networkData.tickSettlementFlagBuff();
               if (this.isServer()) {
                  settlementData.networkData.serverTick();
                  if (settlementData.networkData.isDisbanded()) {
                     settlementsToDisband.add(settlementData);
                  } else {
                     settlementData.serverData.serverTick();
                  }
               }
            }
         }

         for (SettlementsWorldData.SettlementData settlementData : settlementsToDisband) {
            settlementData.networkData.onDisbanded();
            settlementData.serverData.onDisbanded();
            this.getServer()
               .network
               .sendToClientsWithAnyRegion(
                  new PacketRemoveSettlementData(settlementData.serverData.uniqueID),
                  settlementData.serverData.getLevel(),
                  settlementData.networkData.getLoadedRegionRectangle()
               );
            this.deleteSettlement(settlementData.serverData.uniqueID);
         }

         for (int settlementUniqueID : uniqueIDsToUnload) {
            this.unloadSettlement(settlementUniqueID, true);
         }

         if (this.isClient()) {
            while (!this.clientRequestQueue.isEmpty()) {
               int settlementUniqueID = this.clientRequestQueue.pollFirst();
               this.getClient().network.sendPacket(new PacketRequestSettlementData(settlementUniqueID));
               this.clientRequestedTimes.put(settlementUniqueID, this.getLocalTime());
            }

            if (!this.clientRequestedTimes.isEmpty()) {
               HashSet<Integer> removes = new HashSet<>();

               for (Entry<Integer, Long> entry : this.clientRequestedTimes.entrySet()) {
                  long timeSinceRequest = this.getLocalTime() - entry.getValue();
                  if (timeSinceRequest >= 5000L) {
                     int settlementUniqueID = entry.getKey();
                     this.clientRequestQueue.add(settlementUniqueID);
                     removes.add(settlementUniqueID);
                  }
               }

               for (int settlementUniqueID : removes) {
                  this.clientRequestedTimes.remove(settlementUniqueID);
               }
            }
         }
      }
   }

   public void migrateFromOldSettlementSystem(
      OneWorldMigration migration,
      LevelIdentifier oldLevelIdentifier,
      Level newLevel,
      LoadData settlementLayerSave,
      LoadData settlementLevelDataSave,
      Point tileOffset,
      Point positionOffset
   ) {
      synchronized (this) {
         int settlementUniqueID = this.getNewSettlementUniqueID();
         SettlementsWorldData.SettlementData settlementData = new SettlementsWorldData.SettlementData();
         Point flagTile = settlementLevelDataSave.getPoint("entityPos", null, false);
         if (flagTile != null) {
            flagTile.translate(tileOffset.x, tileOffset.y);
            settlementData.networkData = new NetworkSettlementData(newLevel, settlementUniqueID, flagTile.x, flagTile.y);
            settlementData.networkData.setFlagTier(SettlementBoundsManager.flagTiers.length - 2);
            settlementData.serverData = new ServerSettlementData(this, settlementData.networkData, settlementUniqueID);
            settlementData.networkData.setServerData(settlementData.serverData);

            try {
               settlementData.networkData.applyLoadData(settlementLayerSave, migration, tileOffset.x, tileOffset.y);
            } catch (Exception e) {
               GameLog.warn.println("Error migrating old settlement layer from " + oldLevelIdentifier + ":");
               e.printStackTrace(GameLog.warn);
            }

            try {
               settlementData.serverData.applyLoadData(settlementLevelDataSave, migration, tileOffset.x, tileOffset.y);
            } catch (Exception e) {
               GameLog.warn.println("Error migrating old settlement level data from " + oldLevelIdentifier + ":");
               e.printStackTrace(GameLog.warn);
            }

            System.out.println("Successfully migrated old settlement at " + oldLevelIdentifier + " to new system with uniqueID: " + settlementUniqueID);
            this.settlements.put(settlementUniqueID, settlementData);
            this.loadedSettlements.put(settlementUniqueID, settlementData);
            this.updateSettlement(settlementData.networkData);
            migration.setOldSettlementLevelIdentifier(oldLevelIdentifier, settlementUniqueID);
         } else {
            GameLog.warn.println("Could not migrate old settlement from " + oldLevelIdentifier + " because flag tile was not found");
         }
      }
   }

   public int getNewSettlementUniqueID() {
      int uniqueID = GameRandom.globalRandom.nextInt();

      while (uniqueID == 0 || this.settlements.containsKey(uniqueID)) {
         uniqueID = GameRandom.globalRandom.nextInt();
      }

      return uniqueID;
   }

   protected SettlementsWorldData.SettlementsLevelRegionData getRegionData(LevelIdentifier levelIdentifier) {
      synchronized (this) {
         return this.regionData.compute(levelIdentifier, (key, value) -> value == null ? new SettlementsWorldData.SettlementsLevelRegionData() : value);
      }
   }

   public int getSettlementUniqueIDAtRegion(LevelIdentifier levelIdentifier, int regionX, int regionY) {
      return this.getRegionData(levelIdentifier).regionSettlements.getOrDefault(regionX, regionY, 0);
   }

   public boolean canPlaceSettlementFlagAt(LevelIdentifier levelIdentifier, int tileX, int tileY, int flagTier) {
      synchronized (this) {
         SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(levelIdentifier);
         Rectangle regionRectangle = SettlementBoundsManager.getRegionRectangleFromTier(tileX, tileY, flagTier);
         int foundSettlementUniqueID = 0;

         for (int regionX = regionRectangle.x; regionX < regionRectangle.x + regionRectangle.width; regionX++) {
            for (int regionY = regionRectangle.y; regionY < regionRectangle.y + regionRectangle.height; regionY++) {
               int settlementUniqueID = regionData.regionSettlements.getOrDefault(regionX, regionY, 0);
               if (settlementUniqueID != 0) {
                  if (foundSettlementUniqueID != 0 && foundSettlementUniqueID != settlementUniqueID) {
                     return false;
                  }

                  foundSettlementUniqueID = settlementUniqueID;
               }
            }
         }

         if (foundSettlementUniqueID == 0) {
            return true;
         }

         SettlementsWorldData.SettlementData settlement = this.getOrLoadData(foundSettlementUniqueID);
         if (settlement == null) {
            return true;
         }

         if (!settlement.networkData.isDisbandingPrevented()) {
            return settlement.networkData.isDisbanding() && settlement.networkData.isTileWithinBounds(tileX, tileY);
         }

         if (settlement.networkData.isTileWithinBounds(tileX, tileY)) {
            return true;
         }

         if (settlement.serverData == null) {
            this.getOrLoadServerData(foundSettlementUniqueID);
         }

         settlement.networkData.onDisbanded();
         settlement.serverData.onDisbanded();
         this.getServer()
            .network
            .sendToClientsWithAnyRegion(
               new PacketRemoveSettlementData(settlement.serverData.uniqueID),
               settlement.serverData.getLevel(),
               settlement.networkData.getLoadedRegionRectangle()
            );
         this.deleteSettlement(foundSettlementUniqueID);
         return true;
      }
   }

   public void updateSettlement(ServerSettlementData settlement) {
      synchronized (this) {
         Iterable<Point> regionPositions = settlement.boundsManager.getRegionPositions();
         this.updateSettlement(settlement.getLevel().getIdentifier(), settlement.uniqueID, regionPositions);
      }
   }

   public void updateSettlement(NetworkSettlementData data) {
      synchronized (this) {
         Iterable<Point> regionPositions = SettlementBoundsManager.getRegionPositions(data.getTileX(), data.getTileY(), data.getFlagTier());
         this.updateSettlement(data.level.getIdentifier(), data.uniqueID, regionPositions);
      }
   }

   public void updateSettlement(CachedSettlementData data) {
      synchronized (this) {
         Iterable<Point> regionPositions = SettlementBoundsManager.getRegionPositions(data.getTileX(), data.getTileY(), data.getFlagTier());
         this.updateSettlement(data.levelIdentifier, data.uniqueID, regionPositions);
      }
   }

   protected void updateSettlement(LevelIdentifier levelIdentifier, int settlementUniqueID, Iterable<Point> regionPositions) {
      synchronized (this) {
         SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(levelIdentifier);
         PointHashSet lastBounds = regionData.settlementRegions.get(settlementUniqueID);
         if (lastBounds != null) {
            for (Point region : lastBounds) {
               regionData.regionSettlements.remove(region.x, region.y);
            }
         }

         PointHashSet newRegionPositions = new PointHashSet();

         for (Point regionPosition : regionPositions) {
            regionData.regionSettlements.put(regionPosition.x, regionPosition.y, settlementUniqueID);
            newRegionPositions.add(regionPosition.x, regionPosition.y);
         }

         regionData.settlementRegions.put(settlementUniqueID, newRegionPositions);
      }
   }

   protected void clearSettlementRegions(NetworkSettlementData settlement) {
      synchronized (this) {
         SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(settlement.level.getIdentifier());
         PointHashSet lastBounds = regionData.settlementRegions.get(settlement.uniqueID);
         if (lastBounds != null) {
            for (Point region : lastBounds) {
               regionData.regionSettlements.remove(region.x, region.y);
            }
         }

         regionData.settlementRegions.remove(settlement.uniqueID);
      }
   }

   protected SettlementsWorldData.SettlementData getSettlementData(int uniqueID) {
      synchronized (this) {
         return this.settlements.get(uniqueID);
      }
   }

   public List<CachedSettlementData> collectCachedSettlements(Predicate<CachedSettlementData> filer) {
      synchronized (this) {
         return this.settlements
            .values()
            .stream()
            .map(data -> data.networkData != null ? data.networkData.getCacheData(true) : data.cachedData)
            .filter(filer)
            .collect(Collectors.toList());
      }
   }

   public CachedSettlementData getCachedData(int uniqueID) {
      synchronized (this) {
         SettlementsWorldData.SettlementData settlementData = this.getSettlementData(uniqueID);
         if (settlementData != null) {
            return settlementData.networkData != null ? settlementData.networkData.getCacheData(true) : settlementData.cachedData;
         } else {
            return null;
         }
      }
   }

   public NetworkSettlementData getNetworkData(int uniqueID) {
      synchronized (this) {
         SettlementsWorldData.SettlementData settlementData = this.getSettlementData(uniqueID);
         return settlementData == null ? null : settlementData.networkData;
      }
   }

   public ServerSettlementData getServerData(int uniqueID) {
      synchronized (this) {
         SettlementsWorldData.SettlementData settlementData = this.getSettlementData(uniqueID);
         return settlementData == null ? null : settlementData.serverData;
      }
   }

   public boolean hasSettlementAtTile(Level level, int tileX, int tileY) {
      synchronized (this) {
         int regionX = GameMath.getRegionCoordByTile(tileX);
         int regionY = GameMath.getRegionCoordByTile(tileY);
         return this.getRegionData(level.getIdentifier()).regionSettlements.getOrDefault(regionX, regionY, 0) != 0;
      }
   }

   public int getSettlementUniqueIDAtTile(LevelIdentifier levelIdentifier, int tileX, int tileY) {
      synchronized (this) {
         int regionX = GameMath.getRegionCoordByTile(tileX);
         int regionY = GameMath.getRegionCoordByTile(tileY);
         return this.getRegionData(levelIdentifier).regionSettlements.getOrDefault(regionX, regionY, 0);
      }
   }

   public CachedSettlementData getCachedDataAtTile(LevelIdentifier levelIdentifier, int tileX, int tileY) {
      synchronized (this) {
         int uniqueID = this.getSettlementUniqueIDAtTile(levelIdentifier, tileX, tileY);
         return uniqueID != 0 ? this.getCachedData(uniqueID) : null;
      }
   }

   public NetworkSettlementData getNetworkDataAtTile(LevelIdentifier levelIdentifier, int tileX, int tileY) {
      synchronized (this) {
         int uniqueID = this.getSettlementUniqueIDAtTile(levelIdentifier, tileX, tileY);
         return uniqueID != 0 ? this.getNetworkData(uniqueID) : null;
      }
   }

   public ServerSettlementData getServerDataAtTile(LevelIdentifier levelIdentifier, int tileX, int tileY) {
      synchronized (this) {
         int uniqueID = this.getSettlementUniqueIDAtTile(levelIdentifier, tileX, tileY);
         return uniqueID != 0 ? this.getServerData(uniqueID) : null;
      }
   }

   public ServerSettlementData getOrLoadServerDataAtTile(LevelIdentifier levelIdentifier, int tileX, int tileY) {
      synchronized (this) {
         int uniqueID = this.getSettlementUniqueIDAtTile(levelIdentifier, tileX, tileY);
         return uniqueID != 0 ? this.getOrLoadServerData(uniqueID) : null;
      }
   }

   public void updateOwnerTeamID(long ownerAuth, int teamID) {
      synchronized (this) {
         for (SettlementsWorldData.SettlementData data : this.settlements.values()) {
            if (data.cachedData != null) {
               if (data.cachedData.getOwnerAuth() == ownerAuth) {
                  data.cachedData.updateOwnerTeamID(teamID);
               }
            } else if (data.networkData.getOwnerAuth() == ownerAuth) {
               data.networkData.updateOwnerVariables();
               data.networkData.markDirty(false);
            }
         }
      }
   }

   protected void saveSettlementData(NetworkSettlementData networkData, ServerSettlementData serverData) {
      WorldFile file = this.getServer().world.fileSystem.getSettlementFile(serverData.uniqueID);
      SaveData saveData = new SaveData("");
      SaveData networkSave = new SaveData("NETWORK");
      networkData.addSaveData(networkSave);
      saveData.addSaveData(networkSave);
      SaveData serverSave = new SaveData("SERVER");
      serverData.addSaveData(serverSave);
      saveData.addSaveData(serverSave);
      saveData.saveScript(file);
   }

   protected boolean loadSettlementData(NetworkSettlementData networkData, ServerSettlementData serverData) {
      try {
         WorldFile file = this.getServer().world.fileSystem.getSettlementFile(serverData.uniqueID);
         if (file.exists()) {
            LoadData saveData = new LoadData(file);
            LoadData networkDataSave = saveData.getFirstLoadDataByName("NETWORK");
            if (networkDataSave == null) {
               throw new LoadDataException("Settlement file for uniqueID: " + serverData.uniqueID + " does not contain network data.");
            }

            networkData.applyLoadData(networkDataSave, null, 0, 0);
            serverData.ensureRegionsLoaded(true);
            LoadData serverDataSave = saveData.getFirstLoadDataByName("SERVER");
            if (serverDataSave == null) {
               throw new LoadDataException("Settlement file for uniqueID: " + serverData.uniqueID + " does not contain server data.");
            }

            serverData.applyLoadData(serverDataSave, null, 0, 0);
         } else {
            System.err.println("Could not find settlement file for uniqueID: " + serverData.uniqueID);
         }
      } catch (Exception e) {
         System.err.println("Error loading settlement data for uniqueID: " + serverData.uniqueID);
         e.printStackTrace();
      }

      return false;
   }

   protected String loadShouldSettlementDisbandOnLoading(int settlementUniqueID) {
      try {
         WorldFile file = this.getServer().world.fileSystem.getSettlementFile(settlementUniqueID);
         if (file.exists()) {
            LoadData saveData = new LoadData(file);
            LoadData networkDataSave = saveData.getFirstLoadDataByName("NETWORK");
            if (networkDataSave == null) {
               return "No network data";
            }

            if (networkDataSave.getBoolean("disbandingPrevented", false, false)) {
               return "Disbanding prevented";
            }

            LoadData serverDataSave = saveData.getFirstLoadDataByName("SERVER");
            if (serverDataSave == null) {
               return "No server data";
            }

            LoadData settlersSave = serverDataSave.getFirstLoadDataByName("SETTLERS");
            if (settlersSave == null) {
               return "No settlers data";
            }

            System.out.println("hFKRSJD? " + settlersSave.getLoadData().size() + ", " + settlementUniqueID);
            return settlersSave.getLoadData().size() <= 1 ? "Has " + settlersSave.getLoadData().size() + " settlers" : null;
         } else {
            return "No file";
         }
      } catch (Exception e) {
         System.err.println("Error loading settlement is disband prevented data for uniqueID: " + settlementUniqueID);
         e.printStackTrace();
         return null;
      }
   }

   protected void removeSettlementOnLoading(int settlementUniqueID) {
      SettlementsWorldData.SettlementData settlementData = this.settlements.get(settlementUniqueID);
      if (settlementData != null) {
         this.removeSettlementOnLoading(settlementData.cachedData);
      }
   }

   protected void removeSettlementOnLoading(CachedSettlementData settlement) {
      synchronized (this) {
         SettlementsWorldData.SettlementsLevelRegionData regionData = this.getRegionData(settlement.levelIdentifier);
         PointHashSet lastBounds = regionData.settlementRegions.get(settlement.uniqueID);
         if (lastBounds != null) {
            for (Point region : lastBounds) {
               regionData.regionSettlements.remove(region.x, region.y);
            }
         }

         regionData.settlementRegions.remove(settlement.uniqueID);
         this.settlements.remove(settlement.uniqueID);
      }
   }

   protected SettlementsWorldData.SettlementData getOrCreateLoadedData(Level level, int tileX, int tileY) {
      if (level.isClient()) {
         throw new IllegalStateException("Cannot load settlement data on the client side");
      }

      synchronized (this) {
         int uniqueID = this.getSettlementUniqueIDAtTile(level.getIdentifier(), tileX, tileY);
         if (uniqueID != 0) {
            SettlementsWorldData.SettlementData settlement = this.settlements.get(uniqueID);
            if (settlement != null) {
               if (settlement.networkData == null) {
                  settlement.networkData = new NetworkSettlementData(level, uniqueID, settlement.cachedData.getTileX(), settlement.cachedData.getTileY());
                  if (this.isServer()) {
                     settlement.serverData = new ServerSettlementData(this, settlement.networkData, uniqueID);
                     settlement.networkData.setServerData(settlement.serverData);
                     settlement.cachedData = null;
                     this.loadedSettlements.put(uniqueID, settlement);
                     this.loadSettlementData(settlement.networkData, settlement.serverData);
                  } else {
                     this.loadedSettlements.put(uniqueID, settlement);
                  }
               }
            } else {
               GameLog.warn.println("Could not find settlement data when expected at " + tileX + "x" + tileY);
               settlement = new SettlementsWorldData.SettlementData();
               settlement.networkData = new NetworkSettlementData(level, uniqueID, tileX, tileY);
               if (this.isServer()) {
                  settlement.serverData = new ServerSettlementData(this, settlement.networkData, uniqueID);
                  settlement.networkData.setServerData(settlement.serverData);
               }

               this.loadedSettlements.put(uniqueID, settlement);
               this.settlements.put(uniqueID, settlement);
               this.updateSettlement(settlement.networkData);
            }

            return settlement;
         } else {
            int newUniqueID = this.getNewSettlementUniqueID();
            SettlementsWorldData.SettlementData settlement = new SettlementsWorldData.SettlementData();
            settlement.networkData = new NetworkSettlementData(level, newUniqueID, tileX, tileY);
            if (this.isServer()) {
               settlement.serverData = new ServerSettlementData(this, settlement.networkData, newUniqueID);
               settlement.networkData.setServerData(settlement.serverData);
            }

            this.loadedSettlements.put(newUniqueID, settlement);
            this.settlements.put(newUniqueID, settlement);
            this.updateSettlement(settlement.networkData);
            return settlement;
         }
      }
   }

   public NetworkSettlementData getOrCreateNetworkData(Level level, int tileX, int tileY) {
      return this.getOrCreateLoadedData(level, tileX, tileY).networkData;
   }

   protected SettlementsWorldData.SettlementData getOrLoadData(int uniqueID) {
      if (uniqueID == 0) {
         throw new IllegalArgumentException("Cannot load settlement data with uniqueID 0");
      }

      if (!this.isServer()) {
         throw new IllegalStateException("Cannot load settlement data on the client side");
      }

      SettlementsWorldData.SettlementData settlement = this.settlements.get(uniqueID);
      if (settlement != null) {
         if (settlement.networkData == null) {
            Level level = this.getServer().world.getLevel(settlement.cachedData.levelIdentifier);
            settlement.networkData = new NetworkSettlementData(level, uniqueID, settlement.cachedData.getTileX(), settlement.cachedData.getTileY());
            settlement.serverData = new ServerSettlementData(this, settlement.networkData, uniqueID);
            settlement.networkData.setServerData(settlement.serverData);
            settlement.cachedData = null;
            this.loadedSettlements.put(uniqueID, settlement);
            this.loadSettlementData(settlement.networkData, settlement.serverData);
            settlement.serverData.ensureRegionsLoaded(true);
         } else {
            this.loadedSettlements.put(uniqueID, settlement);
         }

         return settlement;
      } else {
         return null;
      }
   }

   public ServerSettlementData getOrLoadServerData(int uniqueID) {
      synchronized (this) {
         SettlementsWorldData.SettlementData settlement = this.getOrLoadData(uniqueID);
         return settlement == null ? null : settlement.serverData;
      }
   }

   public NetworkSettlementData applyNetworkDataPacket(Level level, int uniqueID, PacketReader reader, boolean isFull) {
      Point lastSettlementTile = null;
      SettlementsWorldData.SettlementData settlement = this.settlements.get(uniqueID);
      int lastFlagTier = 0;
      if (settlement == null) {
         settlement = new SettlementsWorldData.SettlementData();
         settlement.networkData = new NetworkSettlementData(level, uniqueID, Integer.MIN_VALUE, Integer.MIN_VALUE);
         this.settlements.put(uniqueID, settlement);
         this.loadedSettlements.put(uniqueID, settlement);
         if (!isFull && this.isClient()) {
            this.ensureClientRequestedSettlement(uniqueID);
         }
      } else {
         lastSettlementTile = new Point(settlement.networkData.getTileX(), settlement.networkData.getTileY());
         lastFlagTier = settlement.networkData.getFlagTier();
      }

      settlement.networkData.readPacket(reader, isFull);
      if (lastSettlementTile == null
         || lastSettlementTile.x != settlement.networkData.getTileX()
         || lastSettlementTile.y != settlement.networkData.getTileY()
         || lastFlagTier != settlement.networkData.getFlagTier()) {
         this.updateSettlement(settlement.networkData);
      }

      if (isFull && this.isClient()) {
         this.submitSettlementRequestFulfilled(uniqueID);
      }

      return settlement.networkData;
   }

   public ServerSettlementData getOrCreateServerData(Level level, int tileX, int tileY) {
      return !this.isServer() ? null : this.getOrCreateLoadedData(level, tileX, tileY).serverData;
   }

   public Stream<CachedSettlementData> streamSettlements() {
      synchronized (this) {
         return this.settlements.values().stream().map(data -> data.networkData != null ? data.networkData.getCacheData(true) : data.cachedData);
      }
   }

   public void deleteSettlementsAt(LevelIdentifier levelIdentifier) {
      synchronized (this) {
         HashSet<Integer> removes = new HashSet<>();

         for (SettlementsWorldData.SettlementData settlement : this.settlements.values()) {
            if (settlement.cachedData != null) {
               if (settlement.cachedData.levelIdentifier.equals(levelIdentifier)) {
                  removes.add(settlement.cachedData.uniqueID);
               }
            } else if (settlement.networkData.level.getIdentifier().equals(levelIdentifier)) {
               removes.add(settlement.networkData.uniqueID);
            }
         }

         for (int settlementUniqueID : removes) {
            this.unloadSettlement(settlementUniqueID, false);
            WorldFile file = this.getServer().world.fileSystem.getSettlementFile(settlementUniqueID);
            if (file.exists()) {
               try {
                  file.delete();
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }

            this.settlements.remove(settlementUniqueID);
         }
      }
   }

   public void deleteSettlement(int uniqueID) {
      SettlementsWorldData.SettlementData settlementData = this.settlements.get(uniqueID);
      NetworkSettlementData networkCache = settlementData != null ? settlementData.networkData : null;
      this.unloadSettlement(uniqueID, false);
      if (networkCache != null) {
         this.clearSettlementRegions(networkCache);
      }

      if (this.isServer()) {
         WorldFile file = this.getServer().world.fileSystem.getSettlementFile(uniqueID);
         if (file.exists()) {
            try {
               file.delete();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }

         this.settlements.remove(uniqueID);
      }
   }

   public boolean unloadSettlement(int uniqueID, boolean saveSettlement) {
      synchronized (this) {
         SettlementsWorldData.SettlementData settlementData = this.settlements.get(uniqueID);
         if (settlementData != null) {
            boolean out = true;
            if (this.isServer()) {
               if (settlementData.cachedData != null) {
                  out = false;
               } else {
                  if (saveSettlement) {
                     this.saveSettlementData(settlementData.networkData, settlementData.serverData);
                  }

                  settlementData.networkData.markUnloaded();
                  settlementData.cachedData = settlementData.networkData.getCacheData(false);
                  settlementData.networkData = null;
                  settlementData.serverData = null;
               }
            } else {
               settlementData.networkData.markUnloaded();
               this.clearSettlementRegions(settlementData.networkData);
               this.settlements.remove(uniqueID);
            }

            this.loadedSettlements.remove(uniqueID);
            return out;
         } else {
            return false;
         }
      }
   }

   public boolean ensureClientRequestedSettlement(int settlementUniqueID) {
      if (!this.isClient()) {
         throw new IllegalStateException("Cannot request settlement data on the server side");
      }

      synchronized (this) {
         if (!this.loadedSettlements.containsKey(settlementUniqueID)
            && !this.clientRequestQueue.contains(settlementUniqueID)
            && !this.clientRequestedTimes.containsKey(settlementUniqueID)) {
            this.clientRequestQueue.add(settlementUniqueID);
            return true;
         } else {
            return false;
         }
      }
   }

   public void submitSettlementRequestFulfilled(int settlementUniqueID) {
      if (!this.isClient()) {
         throw new IllegalStateException("Cannot submit settlement request fulfilled on the server side");
      }

      synchronized (this) {
         this.clientRequestQueue.remove(settlementUniqueID);
         this.clientRequestedTimes.remove(settlementUniqueID);
      }
   }

   public int getTotalSettlements() {
      return this.settlements.size();
   }

   public int getTotalLoadedSettlements() {
      return this.loadedSettlements.size();
   }

   public void runFinalSettlementMigrations(OneWorldMigration migrationData) {
      for (SettlementsWorldData.SettlementData settlement : this.settlements.values()) {
         try {
            boolean unloadAfter = false;
            int uniqueID;
            if (settlement.serverData == null) {
               unloadAfter = true;
               uniqueID = settlement.cachedData.uniqueID;
               Level level = migrationData.world.getLevel(settlement.cachedData.levelIdentifier);
               settlement.networkData = new NetworkSettlementData(level, uniqueID, settlement.cachedData.getTileX(), settlement.cachedData.getTileY());
               settlement.serverData = new ServerSettlementData(this, settlement.networkData, uniqueID);
               settlement.networkData.setServerData(settlement.serverData);
               settlement.cachedData = null;
               this.loadSettlementData(settlement.networkData, settlement.serverData);
            } else {
               uniqueID = settlement.networkData.uniqueID;
            }

            settlement.serverData.runFinalMigration(migrationData);
            if (unloadAfter) {
               this.unloadSettlement(uniqueID, true);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public static SettlementsWorldData getSettlementsData(WorldEntity world) {
      WorldData lastLevelData = world.getWorldData("settlements");
      if (lastLevelData instanceof SettlementsWorldData) {
         return (SettlementsWorldData)lastLevelData;
      }

      SettlementsWorldData data = new SettlementsWorldData();
      world.addWorldData("settlements", data);
      return data;
   }

   public static SettlementsWorldData getSettlementsData(WorldEntityGameClock world) {
      return getSettlementsData(world.getWorldEntity());
   }

   public static SettlementsWorldData getSettlementsData(Server server) {
      return getSettlementsData(server.world.worldEntity);
   }

   public static SettlementsWorldData getSettlementsData(Client client) {
      return getSettlementsData(client.worldEntity);
   }

   protected static class SettlementData {
      protected CachedSettlementData cachedData;
      protected NetworkSettlementData networkData;
      protected ServerSettlementData serverData;
   }

   protected static class SettlementsLevelRegionData {
      protected PointHashMap<Integer> regionSettlements = new PointHashMap<>();
      protected HashMap<Integer, PointHashSet> settlementRegions = new HashMap<>();
   }
}
