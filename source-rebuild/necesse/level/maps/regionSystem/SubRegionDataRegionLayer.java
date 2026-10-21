package necesse.level.maps.regionSystem;

import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.util.GameLinkedList;
import necesse.level.gameObject.DoorObject;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.regionSystem.layers.RegionLayer;

public class SubRegionDataRegionLayer extends RegionLayer {
   protected GameLinkedList<SubRegion> openRegionIDSubRegions = new GameLinkedList<>();
   protected GameLinkedList<SubRegion> openRoomIDSubRegions = new GameLinkedList<>();
   protected GameLinkedList<SubRegion> subRegionsList = new GameLinkedList<>();
   protected SubRegion[][] subRegionTilesArray;

   public SubRegionDataRegionLayer(Region region) {
      super(region);
      this.subRegionTilesArray = new SubRegion[region.tileWidth][region.tileHeight];
   }

   @Override
   public void init() {
   }

   @Override
   public void onLayerLoaded() {
      if (this.level.isLoadingComplete()) {
         this.update();
      }
   }

   @Override
   public void onLoadingComplete() {
      this.update();
   }

   @Override
   public void onLayerUnloaded() {
   }

   public void update() {
      if (this.region.isLoadingComplete() && this.level.isLoadingComplete()) {
         boolean recordConstant = this.level.debugLoadingPerformance != null;
         PerformanceTimerManager tickManager = this.level.debugLoadingPerformance != null ? this.level.debugLoadingPerformance : this.level.tickManager();
         Performance.record(tickManager, "regions", recordConstant, () -> {
            this.invalidate();
            Performance.record(tickManager, "subRegions", recordConstant, this::calculateSubRegions);
            Performance.record(tickManager, "adjacentRegions", recordConstant, this::calculateAdjacentRegions);
            Performance.record(tickManager, "ids", recordConstant, () -> {
               Performance.record(tickManager, "regionIDs", recordConstant, () -> this.updateRegionIDs(this.openRegionIDSubRegions));
               Performance.record(tickManager, "roomIDs", recordConstant, () -> this.updateRoomIDs(this.openRoomIDSubRegions));
            });
         });
      }
   }

   protected void calculateSubRegions() {
      this.invalidate();
      boolean[][] calculatedRegionTiles = new boolean[this.region.tileWidth][this.region.tileHeight];
      this.subRegionsList = new GameLinkedList<>();
      this.openRegionIDSubRegions = new GameLinkedList<>();
      this.openRoomIDSubRegions = new GameLinkedList<>();
      this.subRegionTilesArray = new SubRegion[this.region.tileWidth][this.region.tileHeight];

      for (int regionTileX = 0; regionTileX < this.region.tileWidth; regionTileX++) {
         for (int regionTileY = 0; regionTileY < this.region.tileHeight; regionTileY++) {
            if (!calculatedRegionTiles[regionTileX][regionTileY]) {
               SubRegion subRegion = SubRegion.floodFillSemiRegion(
                  this.region, this.subRegionsList, regionTileX, regionTileY, calculatedRegionTiles, this.subRegionTilesArray
               );
               this.openRegionIDSubRegions.add(subRegion);
               this.openRoomIDSubRegions.add(subRegion);
            }
         }
      }
   }

   protected void calculateAdjacentRegions() {
      for (SubRegion subRegion : this.subRegionsList) {
         subRegion.findAdjacentSemiRegions();
      }
   }

   protected void updateRegionIDs(GameLinkedList<SubRegion> openSubRegions) {
      GameLinkedList<SubRegion> nextOpenSubRegions = new GameLinkedList<>();
      boolean foundAnyRegionIDs = false;

      while (!openSubRegions.isEmpty()) {
         SubRegion subRegion = openSubRegions.removeFirst();
         if (subRegion.getRegionID() == -1) {
            SubRegionDataRegionLayer.CollectedSizeResult sizeResult = this.collectRegionSize(subRegion, RegionManager.NEW_REGION_ID_CHECK_SIZE + 1);
            if (sizeResult.totalSize > RegionManager.NEW_REGION_ID_CHECK_SIZE && sizeResult.largestID != -1) {
               subRegion.setAndExpandRegionID(sizeResult.largestID);
               foundAnyRegionIDs = true;
            } else {
               nextOpenSubRegions.add(subRegion);
            }
         }
      }

      if (!foundAnyRegionIDs) {
         for (SubRegion subRegion : nextOpenSubRegions) {
            if (subRegion.getRegionID() == -1) {
               int nextRegionID = this.manager.getNextRegionID();
               subRegion.setAndExpandRegionID(nextRegionID);
            }
         }
      } else {
         this.updateRegionIDs(nextOpenSubRegions);
      }
   }

   protected void updateRoomIDs(GameLinkedList<SubRegion> openSubRegions) {
      if (!openSubRegions.isEmpty()) {
         GameLinkedList<SubRegion> nextOpenSubRegions = new GameLinkedList<>();
         boolean foundAnyRoomIDs = false;

         while (!openSubRegions.isEmpty()) {
            SubRegion subRegion = openSubRegions.removeFirst();
            if (subRegion.getRoomID() == -1) {
               SubRegionDataRegionLayer.CollectedSizeResult sizeResult = this.collectRoomSize(subRegion, RegionManager.INSIDE_MAX_SIZE + 1);
               if (sizeResult.totalSize > RegionManager.INSIDE_MAX_SIZE && sizeResult.largestID != -1) {
                  subRegion.setAndExpandRoomID(sizeResult.largestID);
                  foundAnyRoomIDs = true;
               } else {
                  nextOpenSubRegions.add(subRegion);
               }
            }
         }

         if (!foundAnyRoomIDs) {
            for (SubRegion subRegion : nextOpenSubRegions) {
               if (subRegion.getRoomID() == -1) {
                  int nextRoomID = this.manager.getRoomNextID();
                  subRegion.setAndExpandRoomID(nextRoomID);
               }
            }
         } else {
            this.updateRoomIDs(nextOpenSubRegions);
         }
      }
   }

   protected SubRegionDataRegionLayer.CollectedSizeResult collectRegionSize(SubRegion subRegion, int maxSize) {
      int totalSize = subRegion.size();
      if (totalSize >= maxSize) {
         return new SubRegionDataRegionLayer.CollectedSizeResult(totalSize, totalSize, subRegion.getRegionID());
      }

      HashMap<Integer, Integer> regionSizes = new HashMap<>();
      regionSizes.put(subRegion.getRegionID(), subRegion.size());

      for (SubRegion adjacentRegion : subRegion.getConnectedRegionsOfSameType()) {
         regionSizes.merge(adjacentRegion.getRegionID(), adjacentRegion.size(), Integer::sum);
         totalSize += adjacentRegion.size();
         if (totalSize >= maxSize) {
            break;
         }
      }

      regionSizes.remove(-1);
      Entry<Integer, Integer> largestRegion = regionSizes.entrySet()
         .stream()
         .max(Entry.comparingByValue())
         .orElse(new SimpleEntry<>(subRegion.getRegionID(), subRegion.size()));
      return new SubRegionDataRegionLayer.CollectedSizeResult(totalSize, largestRegion.getValue(), largestRegion.getKey());
   }

   protected SubRegionDataRegionLayer.CollectedSizeResult collectRoomSize(SubRegion subRegion, int maxSize) {
      int totalSize = subRegion.size();
      if (totalSize >= maxSize) {
         return new SubRegionDataRegionLayer.CollectedSizeResult(totalSize, totalSize, subRegion.getRoomID());
      }

      HashMap<Integer, Integer> roomSizes = new HashMap<>();
      roomSizes.put(subRegion.getRoomID(), subRegion.size());

      for (SubRegion adjacentRegion : subRegion.getConnectedRegionsOfSameRoomInt()) {
         roomSizes.merge(adjacentRegion.getRoomID(), adjacentRegion.size(), Integer::sum);
         totalSize += adjacentRegion.size();
         if (totalSize >= maxSize) {
            break;
         }
      }

      roomSizes.remove(-1);
      Entry<Integer, Integer> largestRoom = roomSizes.entrySet()
         .stream()
         .max(Entry.comparingByValue())
         .orElse(new SimpleEntry<>(subRegion.getRoomID(), subRegion.size()));
      return new SubRegionDataRegionLayer.CollectedSizeResult(totalSize, largestRoom.getValue(), largestRoom.getKey());
   }

   public void onObjectChanged(int regionTileX, int regionTileY, GameObject oldObject, GameObject newObject) {
      RegionType oldType = oldObject.getRegionType();
      RegionType newType = newObject.getRegionType();
      SubRegion subregion = this.getSubRegionByRegion(regionTileX, regionTileY);
      if (oldType.isDoor && newType.isDoor) {
         if (subregion != null) {
            subregion.changeDoorTypeByRegion(newType, (DoorObject)oldObject, (DoorObject)newObject, regionTileX, regionTileY);
         }
      } else if (oldType != newType) {
         this.update();
         if (regionTileX == 0) {
            SubRegion.forceUpdate(this.level, this.region.tileXOffset - 1, this.region.tileYOffset + regionTileY);
         } else if (regionTileX == this.region.tileWidth - 1) {
            SubRegion.forceUpdate(this.level, this.region.tileXOffset + this.region.tileWidth, this.region.tileYOffset + regionTileY);
         }

         if (regionTileY == 0) {
            SubRegion.forceUpdate(this.level, this.region.tileXOffset + regionTileX, this.region.tileYOffset - 1);
         } else if (regionTileY == this.region.tileHeight - 1) {
            SubRegion.forceUpdate(this.level, this.region.tileXOffset + regionTileX, this.region.tileYOffset + this.region.tileHeight);
         }
      }
   }

   public void invalidate() {
      for (SubRegion subRegion : this.subRegionsList) {
         subRegion.invalidate();
      }
   }

   public SubRegion getSubRegionByRegion(int regionTileX, int regionTileY) {
      return this.subRegionTilesArray[regionTileX][regionTileY];
   }

   public int getRegionIDByRegion(int regionTileX, int regionTileY) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      return subRegion == null ? 0 : subRegion.getRegionID();
   }

   public int getRoomIDByRegion(int regionTileX, int regionTileY) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      return subRegion == null ? 0 : subRegion.getRoomID();
   }

   public boolean isOutsideByRegion(int regionTileX, int regionTileY) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      return subRegion == null ? true : subRegion.isOutside();
   }

   public ConnectedSubRegionsResult getTypeConnectedByRegion(int regionTileX, int regionTileY, int maxSize) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      return subRegion == null ? null : subRegion.getAllConnected(sr -> sr.getType() == subRegion.getType(), maxSize);
   }

   public ConnectedSubRegionsResult getRoomConnectedByRegion(int regionTileX, int regionTileY, boolean onlyReturnOpenRoomInt, int maxSize) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      if (subRegion == null) {
         return null;
      } else {
         return onlyReturnOpenRoomInt && subRegion.getType().roomInt != RegionType.OPEN.roomInt
            ? null
            : subRegion.getAllConnected(sr -> sr.getType().roomInt == subRegion.getType().roomInt, maxSize);
      }
   }

   public ConnectedSubRegionsResult getHouseConnectedByRegion(int regionTileX, int regionTileY, int maxSize) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      if (subRegion == null) {
         return null;
      } else if (subRegion.isOutside()) {
         return null;
      } else {
         return subRegion.getType() == RegionType.WALL ? null : subRegion.getAllConnected(sr -> !sr.isOutside() && sr.getType() != RegionType.WALL, maxSize);
      }
   }

   public SubRegion findFirstTypeConnectedByRegion(int regionTileX, int regionTileY, int maxSize, Predicate<SubRegion> test) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      return subRegion == null ? null : subRegion.findFirstAllConnected(sr -> sr.getType() == subRegion.getType(), maxSize, test);
   }

   public SubRegion findFirstRoomConnectedByRegion(int regionTileX, int regionTileY, boolean onlyReturnOpenRoomInt, int maxSize, Predicate<SubRegion> test) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      if (subRegion == null) {
         return null;
      } else {
         return onlyReturnOpenRoomInt && subRegion.getType().roomInt != RegionType.OPEN.roomInt
            ? null
            : subRegion.findFirstAllConnected(sr -> sr.getType().roomInt == subRegion.getType().roomInt, maxSize, test);
      }
   }

   public SubRegion findFirstHouseConnectedByRegion(int regionTileX, int regionTileY, int maxSize, Predicate<SubRegion> test) {
      SubRegion subRegion = this.getSubRegionByRegion(regionTileX, regionTileY);
      if (subRegion == null) {
         return null;
      } else if (subRegion.isOutside()) {
         return null;
      } else {
         return subRegion.getType() == RegionType.WALL
            ? null
            : subRegion.findFirstAllConnected(sr -> !sr.isOutside() && sr.getType() != RegionType.WALL, maxSize, test);
      }
   }

   protected static class CollectedSizeResult {
      public final int totalSize;
      public final int largestSize;
      public final int largestID;

      public CollectedSizeResult(int totalSize, int largestSize, int largestID) {
         this.totalSize = totalSize;
         this.largestSize = largestSize;
         this.largestID = largestID;
      }
   }
}
