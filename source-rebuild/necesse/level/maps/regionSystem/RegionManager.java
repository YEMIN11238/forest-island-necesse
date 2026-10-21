package necesse.level.maps.regionSystem;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.AbstractSaveHandler;
import necesse.engine.network.server.Server;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.PointSetAbstract;
import necesse.entity.mobs.BasicPathDoorOption;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.path.SubRegionPathResult;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.gameObject.DoorObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.managers.RegionFilesManager;
import necesse.level.maps.regionSystem.regionsStructure.RegionStructureDataAbstract;
import necesse.level.maps.regionSystem.regionsStructure.RegionStructureDataMap;
import necesse.level.maps.splattingManager.SplattingOptions;

public class RegionManager {
   public static final int REGION_SIZE_BITS = 4;
   public static final int REGION_SIZE = 16;
   public static int INSIDE_MAX_SIZE = 500;
   public static int NEW_REGION_ID_CHECK_SIZE = 2000;
   public final Level level;
   private final RegionStructureDataAbstract regions;
   private final RegionFilesManager filesManager;
   private final HashMap<Integer, Integer> roomSizeCache = new HashMap<>();
   private final RegionManager.IDCounter regionIDCounter = new RegionManager.IDCounter();
   private final RegionManager.IDCounter roomIDCounter = new RegionManager.IDCounter();
   public PathDoorOption BASIC_DOOR_OPTIONS;
   public PathDoorOption CAN_OPEN_DOORS_OPTIONS;
   public PathDoorOption CANNOT_OPEN_CAN_CLOSE_DOORS_OPTIONS;
   public PathDoorOption CANNOT_PASS_DOORS_OPTIONS;
   public PathDoorOption SUMMONED_MOB_OPTIONS;
   public PathDoorOption CAN_BREAK_OBJECTS_OPTIONS;

   public RegionManager(Level level) {
      this.level = level;
      this.filesManager = new RegionFilesManager(this);
      this.regions = new RegionStructureDataMap(this, this::constructNewRegion, (region, forceSkipGenerate) -> this.loadNewRegion(region, forceSkipGenerate));
      this.BASIC_DOOR_OPTIONS = new BasicPathDoorOption("BASIC", level, false, false);
      this.CAN_OPEN_DOORS_OPTIONS = new BasicPathDoorOption("CAN_OPEN_DOORS", level, true, true);
      this.CANNOT_OPEN_CAN_CLOSE_DOORS_OPTIONS = new BasicPathDoorOption("CANNOT_OPEN_CAN_CLOSE_DOORS", level, false, true);
      this.CANNOT_PASS_DOORS_OPTIONS = new BasicPathDoorOption("CANNOT_PASS_DOORS", level, false, false) {
         @Override
         public boolean canPathThroughCheckTile(SubRegion subregion, int tileX, int tileY) {
            return false;
         }

         @Override
         public boolean canPass(int tileX, int tileY) {
            return super.canPass(tileX, tileY) && !this.level.getObject(tileX, tileY).isDoor;
         }

         @Override
         public boolean canPassDoor(DoorObject doorObject, int tileX, int tileY) {
            return false;
         }
      };
      this.SUMMONED_MOB_OPTIONS = new PathDoorOption("CAN_PASS_DOORS", level) {
         @Override
         public SubRegionPathResult canPathThrough(SubRegion subregion) {
            if (subregion.getType().isDoor) {
               return SubRegionPathResult.VALID;
            } else if (subregion.getType() == RegionType.SUMMON_IGNORED) {
               return SubRegionPathResult.VALID;
            } else {
               return subregion.getType().isSolid ? SubRegionPathResult.INVALID : SubRegionPathResult.VALID;
            }
         }

         @Override
         public boolean canPassDoor(DoorObject doorObject, int tileX, int tileY) {
            return true;
         }

         @Override
         public boolean canBreakDown(int tileX, int tileY) {
            return false;
         }

         @Override
         public boolean canOpen(int tileX, int tileY) {
            return false;
         }

         @Override
         public boolean canClose(int tileX, int tileY) {
            return false;
         }

         @Override
         public boolean doorChangeInvalidatesCache(DoorObject lastDoor, DoorObject newDoor, int tileX, int tileY) {
            return false;
         }
      };
      this.CAN_BREAK_OBJECTS_OPTIONS = new BasicPathDoorOption("CAN_BREAK_OBJECTS", level, true, false, false);
   }

   public int getRegionsWidth() {
      return this.level.tileWidth > 0 ? GameMath.divideByPowerOf2RoundedUp(this.level.tileWidth, 4) : 0;
   }

   public int getRegionsHeight() {
      return this.level.tileHeight > 0 ? GameMath.divideByPowerOf2RoundedUp(this.level.tileHeight, 4) : 0;
   }

   public boolean isRegionXWithinBounds(int regionX) {
      return this.level.tileWidth <= 0 ? true : regionX >= 0 && regionX < this.getRegionsWidth();
   }

   public boolean isRegionYWithinBounds(int regionY) {
      return this.level.tileHeight <= 0 ? true : regionY >= 0 && regionY < this.getRegionsHeight();
   }

   public boolean isRegionWithinBounds(int regionX, int regionY) {
      return this.isRegionXWithinBounds(regionX) && this.isRegionYWithinBounds(regionY);
   }

   public int limitRegionXToBounds(int regionX) {
      return this.level.tileWidth > 0 ? GameMath.limit(regionX, 0, this.getRegionsWidth() - 1) : regionX;
   }

   public int limitRegionYToBounds(int regionY) {
      return this.level.tileHeight > 0 ? GameMath.limit(regionY, 0, this.getRegionsHeight() - 1) : regionY;
   }

   public int getTileCoordByRegion(int region) {
      return GameMath.getTileCoordByRegion(region);
   }

   public int getRegionCoordByTile(int tile) {
      return GameMath.getRegionCoordByTile(tile);
   }

   public int getRegionXByTileLimited(int tileX) {
      return this.limitRegionXToBounds(this.getRegionCoordByTile(tileX));
   }

   public int getRegionYByTileLimited(int tileY) {
      return this.limitRegionYToBounds(this.getRegionCoordByTile(tileY));
   }

   public int getRegionTileWidth(int regionX, int tileXOffset) {
      int tileWidth = 16;
      if (this.level.tileWidth > 0) {
         if (regionX < 0) {
            return 0;
         }

         tileWidth = Math.min(tileWidth, this.level.tileWidth - tileXOffset);
         if (tileWidth <= 0) {
            return 0;
         }
      }

      return tileWidth;
   }

   public int getRegionTileWidth(int regionX) {
      return this.getRegionTileWidth(regionX, this.getTileCoordByRegion(regionX));
   }

   public int getRegionTileHeight(int regionY, int tileYOffset) {
      int tileHeight = 16;
      if (this.level.tileHeight > 0) {
         if (regionY < 0) {
            return 0;
         }

         tileHeight = Math.min(tileHeight, this.level.tileHeight - tileYOffset);
         if (tileHeight <= 0) {
            return 0;
         }
      }

      return tileHeight;
   }

   public int getRegionTileHeight(int regionY) {
      return this.getRegionTileHeight(regionY, this.getTileCoordByRegion(regionY));
   }

   protected Region constructNewRegion(int regionX, int regionY) {
      int tileXOffset = this.getTileCoordByRegion(regionX);
      int tileYOffset = this.getTileCoordByRegion(regionY);
      int tileWidth = this.getRegionTileWidth(regionX, tileXOffset);
      if (tileWidth <= 0) {
         return null;
      }

      int tileHeight = this.getRegionTileHeight(regionY, tileYOffset);
      return tileHeight <= 0 ? null : new Region(this, regionX, regionY, tileXOffset, tileYOffset, tileWidth, tileHeight);
   }

   public void makeServerLevel() {
      this.filesManager.makeServerLevel();
   }

   protected void loadNewRegion(Region region, boolean forceSkipGenerate) {
      boolean loaded = Performance.recordConstant(this.level.debugLoadingPerformance, "regionLoading", () -> this.filesManager.loadRegion(region));
      boolean regionGenerated = false;
      if (!loaded && !this.level.isClient()) {
         if (this.level.isClient()) {
            region.tileLayer.fillWithEmptyTiles();
         } else {
            regionGenerated = true;

            try {
               if (forceSkipGenerate) {
                  this.level.onGenerateRegionSkipped(region);
               } else {
                  this.level.generateRegion(region);
               }
            } catch (Exception e) {
               region.tileLayer.fillWithTilesIfEmpty(TileRegistry.waterID);
               System.err.println("Error generating level " + this.level.getIdentifier() + " region " + region.regionX + "x" + region.regionY + ":");
               e.printStackTrace();
            }
         }
      }

      if (this.level.isLoadingComplete() && !this.level.isClient()) {
         boolean recordConstant = this.level.debugLoadingPerformance != null;
         PerformanceTimerManager tickManager = this.level.debugLoadingPerformance != null ? this.level.debugLoadingPerformance : this.level.tickManager();
         Performance.record(tickManager, "regionLoaded", recordConstant, () -> {
            region.onLayerLoaded();
            Performance.record(tickManager, "lightTime", recordConstant, region::updateLight);
         });
      }

      if (regionGenerated) {
         if (!this.level.isLoadingComplete()) {
            boolean recordConstant = this.level.debugLoadingPerformance != null;
            PerformanceTimerManager tickManager = this.level.debugLoadingPerformance != null ? this.level.debugLoadingPerformance : this.level.tickManager();
            Performance.record(tickManager, "regionLoaded", recordConstant, () -> region.onLayerLoaded());
         }

         this.level.onRegionGenerated(region, forceSkipGenerate);
      }
   }

   public void onLoadingComplete() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.onLoadingComplete();
         }
      }
   }

   public AbstractSaveHandler getSaveHandler(PerformanceTimerManager performanceTimer, Server server) {
      synchronized (this.level.entityManager.lock) {
         return this.filesManager.getSaveHandler(performanceTimer, server, this.regions);
      }
   }

   public void saveRegions() {
      if (this.level.isServer()) {
         Performance.record(this.level.tickManager(), "updateRegionFiles", () -> {
            synchronized (this.level.entityManager.lock) {
               for (Region region : this.regions) {
                  this.filesManager.updateSaveFile(region);
               }
            }
         });
         Performance.record(this.level.tickManager(), "saveRegionFiles", this.filesManager::saveAll);
      }
   }

   protected void updateSaveFile(Region region) {
      this.filesManager.updateSaveFile(region);
   }

   public void unloadRegion(Region region) {
      synchronized (this.level.entityManager.lock) {
         region.onUnloading();
         this.regions.removeRegion(region.regionX, region.regionY);
      }

      this.filesManager.onUnloaded(region);
      region.onUnloaded();
      region.dispose();
   }

   public boolean unloadRegion(int regionX, int regionY) {
      synchronized (this.level.entityManager.lock) {
         Region region = this.regions.getRegion(regionX, regionY, false, false);
         if (region == null) {
            return false;
         }

         this.unloadRegion(region);
         return true;
      }
   }

   public Region getRegion(int regionX, int regionY, boolean loadIfNotLoaded) {
      return this.regions.getRegion(regionX, regionY, loadIfNotLoaded, false);
   }

   public Region getRegionByTile(int tileX, int tileY, boolean loadIfNotLoaded) {
      if (this.level.tileWidth <= 0 || tileX >= 0 && tileX < this.level.tileWidth) {
         return this.level.tileHeight <= 0 || tileY >= 0 && tileY < this.level.tileHeight
            ? this.getRegion(this.getRegionCoordByTile(tileX), this.getRegionCoordByTile(tileY), loadIfNotLoaded)
            : null;
      } else {
         return null;
      }
   }

   public boolean isRegionLoaded(int regionX, int regionY) {
      return this.regions.isRegionLoaded(regionX, regionY);
   }

   public boolean isRegionLoadingComplete(int regionX, int regionY) {
      Region region = this.getRegion(regionX, regionY, false);
      return region != null && region.isLoadingComplete();
   }

   public boolean isRegionLoadingCompleteByTile(int tileX, int tileY) {
      if (this.level.tileWidth <= 0 || tileX >= 0 && tileX < this.level.tileWidth) {
         return this.level.tileHeight <= 0 || tileY >= 0 && tileY < this.level.tileHeight
            ? this.isRegionLoadingComplete(this.getRegionCoordByTile(tileX), this.getRegionCoordByTile(tileY))
            : false;
      } else {
         return false;
      }
   }

   public boolean isTileLoaded(int tileX, int tileY) {
      return this.isRegionLoaded(this.getRegionCoordByTile(tileX), this.getRegionCoordByTile(tileY));
   }

   public boolean isRegionGenerated(int regionX, int regionY) {
      return this.regions.isRegionLoaded(regionX, regionY) || this.filesManager.isRegionGenerated(regionX, regionY);
   }

   public int getLoadedRegionsSize() {
      return this.regions.getLoadedRegionsSize();
   }

   public Collection<Region> collectLoadedRegions() {
      ArrayList<Region> out = new ArrayList<>(this.regions.getLoadedRegionsSize());
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            out.add(region);
         }

         return out;
      }
   }

   public void forEachLoadedRegions(Consumer<Region> forEach) {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            forEach.accept(region);
         }
      }
   }

   public void ensureRegionIsLoadedButDontGenerate(int regionX, int regionY) {
      this.regions.getRegion(regionX, regionY, true, true);
   }

   public void ensureRegionsAreLoadedButDontGenerate(int startRegionX, int startRegionY, int endRegionX, int endRegionY) {
      for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
         for (int regionY = startRegionY; regionY <= endRegionY; regionY++) {
            this.ensureRegionIsLoadedButDontGenerate(regionX, regionY);
         }
      }
   }

   public void ensureTilesAreLoadedButDontGenerate(int startTileX, int startTileY, int endTileX, int endTileY) {
      this.ensureRegionsAreLoadedButDontGenerate(
         this.getRegionCoordByTile(startTileX), this.getRegionCoordByTile(startTileY), this.getRegionCoordByTile(endTileX), this.getRegionCoordByTile(endTileY)
      );
   }

   public void forceUnloadClientsRegion(int regionX, int regionY) {
      if (this.level.isServer()) {
         Server server = this.level.getServer();
         server.streamClients().forEach(client -> client.removeLoadedRegion(this.level, regionX, regionY, true, true));
      }
   }

   public void ensureRegionIsLoaded(int regionX, int regionY) {
      this.getRegion(regionX, regionY, true);
   }

   public void ensureTileIsLoaded(int tileX, int tileY) {
      this.ensureRegionIsLoaded(this.getRegionCoordByTile(tileX), this.getRegionCoordByTile(tileY));
   }

   public void ensureRegionsAreLoaded(int startRegionX, int startRegionY, int endRegionX, int endRegionY) {
      for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
         for (int regionY = startRegionY; regionY <= endRegionY; regionY++) {
            this.ensureRegionIsLoaded(regionX, regionY);
         }
      }
   }

   public void ensureTilesAreLoaded(int startTileX, int startTileY, int endTileX, int endTileY) {
      this.ensureRegionsAreLoaded(
         this.getRegionCoordByTile(startTileX), this.getRegionCoordByTile(startTileY), this.getRegionCoordByTile(endTileX), this.getRegionCoordByTile(endTileY)
      );
   }

   public void ensureRegionsAreLoaded(PointSetAbstract<?> regionPositions) {
      if (regionPositions != null) {
         for (Point regionPosition : regionPositions) {
            this.ensureRegionIsLoaded(regionPosition.x, regionPosition.y);
         }
      }
   }

   public void ensureEntireLevelIsLoaded() {
      if (this.level.tileWidth >= 0 && this.level.tileHeight >= 0) {
         for (int regionX = 0; regionX < this.getRegionsWidth(); regionX++) {
            for (int regionY = 0; regionY < this.getRegionsHeight(); regionY++) {
               this.ensureRegionIsLoaded(regionX, regionY);
            }
         }
      } else {
         GameLog.warn.println("Attempted to ensure entire level is loaded on infinite level. Stacktrace:");
         new Throwable().printStackTrace(GameLog.warn);
      }
   }

   public void simulateWorldTime(long timeIncrease, boolean sendChanges) {
      if (timeIncrease > 0L) {
         synchronized (this.level.entityManager.lock) {
            for (Region region : this.regions) {
               region.simulateWorldTime(timeIncrease, sendChanges);
            }
         }
      }
   }

   public void frameTick(TickManager tickManager) {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.frameTick(tickManager);
         }
      }
   }

   public void clientTick() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.clientTick();
         }
      }
   }

   public LinkedList<Region> tickUnloadRegions(int unloadTimeSeconds) {
      synchronized (this.level.entityManager.lock) {
         LinkedList<Region> regionsToUnload = new LinkedList<>();

         for (Region region : this.regions) {
            if (region.unloadRegionBuffer.shouldUnload(unloadTimeSeconds)) {
               regionsToUnload.add(region);
            }
         }

         return regionsToUnload;
      }
   }

   public void serverTick() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.serverTick();
         }

         this.filesManager.serverTick();
      }
   }

   public void resetFinalInsideLights() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.lightLayer.resetFinalInsideLights();
         }
      }
   }

   public void tickTiles() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.tickTiles();
         }
      }
   }

   public void tickTileEffect(GameCamera camera, PlayerMob perspective, int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         region.tickTileEffect(camera, perspective, tileX, tileY);
      }
   }

   public void changeRoomSizeCache(int roomID, int change) {
      this.roomSizeCache.compute(roomID, (key, currentSize) -> {
         if (currentSize == null) {
            return change > 0 ? change : null;
         }

         int newSize = currentSize + change;
         return newSize <= 0 ? null : newSize;
      });
   }

   public int getRoomSize(int roomID) {
      return this.roomSizeCache.getOrDefault(roomID, -1);
   }

   public void debugPrintRoomSizes() {
      this.roomSizeCache.forEach((roomID, size) -> System.out.println(roomID + ": " + size));
   }

   public int getNextRegionID() {
      return this.regionIDCounter.getNextID();
   }

   public int getRoomNextID() {
      return this.roomIDCounter.getNextID();
   }

   public int getRegionIDByTile(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.subRegionData.getRegionIDByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public RegionType getRegionTypeByTile(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      if (region == null) {
         return RegionType.SOLID;
      }

      SubRegion subregion = region.subRegionData.getSubRegionByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
      return subregion == null ? RegionType.SOLID : subregion.getType();
   }

   public int getRoomIDByTile(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.subRegionData.getRoomIDByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public boolean isOutsideByTile(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? true : region.subRegionData.isOutsideByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public SubRegion getSubRegionByTile(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? null : region.subRegionData.getSubRegionByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public ConnectedSubRegionsResult getTypeConnectedByTile(int tileX, int tileY, int maxSize) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? null : region.subRegionData.getTypeConnectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, maxSize);
   }

   public ConnectedSubRegionsResult getRoomConnectedByTile(int tileX, int tileY, boolean onlyReturnOpenRoomInt, int maxSize) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null
         ? null
         : region.subRegionData.getRoomConnectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, onlyReturnOpenRoomInt, maxSize);
   }

   public ConnectedSubRegionsResult getHouseConnectedByTile(int tileX, int tileY, int maxSize) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? null : region.subRegionData.getHouseConnectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, maxSize);
   }

   public SubRegion findFirstTypeConnectedByTile(int tileX, int tileY, int maxSize, Predicate<SubRegion> test) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? null : region.subRegionData.findFirstTypeConnectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, maxSize, test);
   }

   public SubRegion findFirstRoomConnectedByTile(int tileX, int tileY, boolean onlyReturnOpenRoomInt, int maxSize, Predicate<SubRegion> test) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null
         ? null
         : region.subRegionData.findFirstRoomConnectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, onlyReturnOpenRoomInt, maxSize, test);
   }

   public SubRegion findFirstHouseConnectedByTile(int tileX, int tileY, int maxSize, Predicate<SubRegion> test) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null
         ? null
         : region.subRegionData.findFirstHouseConnectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, maxSize, test);
   }

   public void onWireUpdate(int tileX, int tileY, int wireID, boolean active) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         region.onWireUpdate(tileX, tileY, wireID, active);
      }
   }

   public Packet getRegionDataPacket(int regionX, int regionY) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      Region region = this.getRegion(regionX, regionY, true);
      if (region == null) {
         throw new NullPointerException("Tried to send region data out side bounds (" + regionX + "x" + regionY + ") at " + this.level.getIdentifier());
      }

      region.writeRegionDataPacket(writer);
      return packet;
   }

   public boolean applyRegionDataPacket(int regionX, int regionY, Packet packet) {
      Region region = this.getRegion(regionX, regionY, true);
      if (region == null) {
         GameLog.warn.println("Received invalid region packet for " + regionX + "x" + regionY + " at " + this.level.getIdentifier());
         return true;
      }

      boolean valid = region.applyRegionDataPacket(new PacketReader(packet));
      if (valid && this.level.isLoadingComplete()) {
         region.onLayerLoaded();
      }

      return valid;
   }

   public synchronized void calculateRegions() {
      for (Region region : this.regions) {
         region.updateSubRegions();
      }
   }

   public void updateLiquidManager(int regionX, int regionY) {
      int tileXOffset = GameMath.multiplyByPowerOf2(regionX, 4);
      int tileYOffset = GameMath.multiplyByPowerOf2(regionY, 4);
      this.level.liquidManager.updateLevel(null, tileXOffset - 1, tileYOffset - 1, tileXOffset + 16 + 1, tileYOffset + 16 + 1, true, true);
   }

   public void updateSplattingManager(int regionX, int regionY) {
      if (!this.level.isServer()) {
         int tileXOffset = GameMath.multiplyByPowerOf2(regionX, 4);
         int tileYOffset = GameMath.multiplyByPowerOf2(regionY, 4);
         new RegionBoundsExecutor(this, tileXOffset - 1, tileYOffset - 1, tileXOffset + 16 + 1, tileYOffset + 16 + 1, false)
            .runCoordinates((region, regionTileX, regionTileY) -> region.splattingLayer.updateSplattingByRegion(regionTileX, regionTileY));
      }
   }

   public void replaceObjectEntities(int regionX, int regionY) {
      Region region = this.getRegion(regionX, regionY, false);
      if (region != null) {
         region.replaceObjectEntities();
      }
   }

   public void updateLight(int regionX, int regionY) {
      Region region = this.getRegion(regionX, regionY, false);
      if (region != null) {
         region.updateLight();
      }
   }

   public void onSplattingChange(int tileX, int tileY) {
      new RegionBoundsExecutor(this.level.regionManager, tileX - 1, tileY - 1, tileX + 1, tileY + 1, false)
         .runCoordinates((region, regionTileX, regionTileY) -> region.splattingLayer.updateSplattingByRegion(regionTileX, regionTileY));
   }

   public SplattingOptions getSplatTiles(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? null : region.splattingLayer.getSplatTilesByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public void addSplattingDebugTooltips(int tileX, int tileY, StringTooltips tooltips) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         region.splattingLayer.addDebugTooltips(tileX - region.tileXOffset, tileY - region.tileYOffset, tooltips);
      }
   }

   public void addBiomeBlendingDebugTooltips(int tileX, int tileY, StringTooltips tooltips) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         region.biomeBlendingLayer.addDebugTooltips(tileX - region.tileXOffset, tileY - region.tileYOffset, tooltips);
      }
   }

   public void setTileProtected(int tileX, int tileY, boolean value) {
      Region region = this.getRegionByTile(tileX, tileY, true);
      if (region != null) {
         region.tilesProtectedLayer.setTileProtectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, value);
      }
   }

   public boolean isTileProtected(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? false : region.tilesProtectedLayer.isTileProtectedByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public byte getWireData(int tileX, int tileY) {
      Region region = this.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.wireLayer.getWireDataByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public void setWireData(int tileX, int tileY, byte data) {
      Region region = this.getRegionByTile(tileX, tileY, true);
      if (region != null) {
         region.wireLayer.setWireDataByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, data);
      }
   }

   public void calculateFullLiquidData() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.liquidData.calculateFull();
         }
      }
   }

   public void calculateShoresLiquidData() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.liquidData.updateShoresFull();
         }
      }
   }

   public void deleteLevelFiles() {
      this.filesManager.deleteLevelFiles();
   }

   public void onUnloading() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.onUnloading();
         }
      }
   }

   public void dispose() {
      synchronized (this.level.entityManager.lock) {
         for (Region region : this.regions) {
            region.dispose();
         }
      }
   }

   public static class IDCounter {
      private int currentID = 1;

      public int getNextID() {
         int current = this.currentID++;
         if (this.currentID == -1 || this.currentID == 0) {
            this.currentID = 1;
         }

         return current;
      }
   }
}
