package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.furniture.RoomFurniture;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.RoomQualitySettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.RoomSizeSettlerThought;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;
import necesse.level.maps.regionSystem.RegionType;
import necesse.level.maps.regionSystem.SubRegion;

public class SettlementRoom {
   public final ServerSettlementData data;
   private final PointHashMap<SettlementRoom> roomsMap;
   public final int tileX;
   public final int tileY;
   private boolean calculatedStats = false;
   private boolean invalidated;
   private final HashMap<String, Integer> roomProperties = new HashMap<>();
   private final HashMap<String, Integer> furnitureTypes = new HashMap<>();
   private final HashMap<Integer, HappinessObject> happinessObjects = new HashMap<>();
   private final PointHashSet roomTiles = new PointHashSet();
   private final PointHashSet doors = new PointHashSet();
   private final PointHashSet walls = new PointHashSet();
   private final ArrayList<SettlementBed> beds = new ArrayList<>();
   private final SettlerThoughtsList baseThoughts = new SettlerThoughtsList();
   private int happinessScore = 0;

   public SettlementRoom(ServerSettlementData data, PointHashMap<SettlementRoom> roomsMap, int tileX, int tileY) {
      this.data = data;
      this.roomsMap = roomsMap;
      this.tileX = tileX;
      this.tileY = tileY;
   }

   protected void calculateStats() {
      try {
         if (this.calculatedStats) {
            return;
         }

         this.roomTiles.clear();
         this.doors.clear();
         this.walls.clear();
         this.beds.clear();
         this.roomProperties.clear();
         this.furnitureTypes.clear();
         this.happinessObjects.clear();
         this.baseThoughts.clear();
         if (this.getLevel().isOutside(this.tileX, this.tileY)) {
            return;
         }

         ConnectedSubRegionsResult room = this.getLevel().regionManager.getRoomConnectedByTile(this.tileX, this.tileY, true, 2000);
         if (room == null) {
            return;
         }

         int size = this.getLevel().getRoomSize(this.tileX, this.tileY);
         this.addRoomProperty("size", size);
         HashSet<SubRegion> doorRegions = new HashSet<>();
         HashSet<SubRegion> wallRegions = new HashSet<>();
         ArrayList<HappinessObject> happinessObjectsInRoom = new ArrayList<>();

         for (SubRegion roomSr : room.connectedRegions) {
            for (SubRegion adj : roomSr.getAdjacentRegions()) {
               if (adj.getType() == RegionType.WALL) {
                  wallRegions.add(adj);
               } else if (adj.getType() == RegionType.DOOR
                  && !doorRegions.contains(adj)
                  && adj.streamAdjacentRegions()
                     .anyMatch(adjDoor -> adjDoor.getType().roomInt == RegionType.OPEN.roomInt && !room.connectedRegions.contains(adjDoor))) {
                  for (Point levelTile : adj.getLevelTiles()) {
                     this.doors.add(levelTile.x, levelTile.y);
                  }

                  doorRegions.add(adj);
               }
            }

            for (Point levelTile : roomSr.getLevelTiles()) {
               if (this.data.networkData.isTileWithinBounds(levelTile.x, levelTile.y)) {
                  this.roomTiles.add(levelTile.x, levelTile.y);

                  for (int layer = 0; layer < ObjectLayerRegistry.getTotalLayers(); layer++) {
                     GameObject object = this.getLevel().getObject(layer, levelTile.x, levelTile.y);
                     object.roomProperties.forEach(s -> this.addRoomProperty(s, 1));
                     if (object instanceof HappinessObject && object.isMultiTileMaster()) {
                        happinessObjectsInRoom.add((HappinessObject)object);
                     }

                     if (object instanceof RoomFurniture) {
                        String type = ((RoomFurniture)object).getFurnitureType();
                        if (type != null) {
                           this.addFurnitureType(type, 1);
                        }
                     }
                  }

                  GameTile tile = this.getLevel().getTile(levelTile.x, levelTile.y);
                  tile.roomProperties.forEach(s -> this.addRoomProperty(s, 1));
                  if (!tile.isFloor) {
                     this.addRoomProperty("outsidefloor", 1);
                  }

                  SettlementBed bed = this.data.addOrValidateBed(levelTile.x, levelTile.y, true);
                  if (bed != null) {
                     this.beds.add(bed);
                  }

                  if (this.roomsMap != null) {
                     this.roomsMap.put(levelTile.x, levelTile.y, this);
                  }
               }
            }
         }

         happinessObjectsInRoom.sort(Comparator.comparingInt(HappinessObject::getHappinessBonus).reversed());
         this.beds.sort(Comparator.<SettlementBed>comparingInt(b -> b.tileY).thenComparingInt(b -> b.tileX));
         int happinessObjectIndex = 0;

         for (SettlementBed bed : this.beds) {
            if (happinessObjectIndex >= happinessObjectsInRoom.size()) {
               break;
            }

            LevelSettler settler = bed.getSettler();
            if (settler != null) {
               HappinessObject happinessObject = happinessObjectsInRoom.get(happinessObjectIndex++);
               this.happinessObjects.put(settler.mobUniqueID, happinessObject);
            }
         }

         for (SubRegion wallRegion : wallRegions) {
            for (Point levelTile : wallRegion.getLevelTiles()) {
               if (Arrays.stream(Level.adjacentGetters).anyMatch(offset -> this.roomTiles.contains(levelTile.x + offset.x, levelTile.y + offset.y))) {
                  GameObject object = this.getLevel().getObject(levelTile.x, levelTile.y);
                  if (object.isWindow) {
                     this.addRoomProperty("windows", 1);
                  }

                  this.walls.add(levelTile.x, levelTile.y);
               }
            }
         }

         this.addRoomProperty("doors", doorRegions.size());
      } finally {
         this.calculatedStats = true;
      }

      for (SettlementBed bed : this.beds) {
         LevelSettler settler = bed.getSettler();
         if (settler != null) {
            settler.markRoomDirty();
         }
      }

      this.calculateHappinessModifiers();
   }

   public void invalidate() {
      this.invalidated = true;
   }

   public boolean isInvalidated() {
      return this.invalidated;
   }

   public void recalculateStats() {
      this.calculatedStats = false;
   }

   public ServerSettlementData getData() {
      return this.data;
   }

   public Level getLevel() {
      return this.data.getLevel();
   }

   private void addRoomProperty(String property, int amount) {
      this.roomProperties.compute(property, (key, i) -> i == null ? amount : i + amount);
   }

   private void addFurnitureType(String type, int amount) {
      this.furnitureTypes.compute(type, (key, i) -> i == null ? amount : i + amount);
   }

   public int getRoomProperty(String property) {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.roomProperties.getOrDefault(property, 0);
   }

   public int getFurnitureTypes(String type) {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.furnitureTypes.getOrDefault(type, 0);
   }

   public int getFurnitureScore() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.furnitureTypes.values().stream().mapToInt(i -> (int)Math.pow(i.intValue(), 0.44)).sum();
   }

   public int getRoomSize() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.roomProperties.getOrDefault("size", 0);
   }

   public Iterable<Point> getRoomTiles() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.roomTiles;
   }

   public Stream<Point> streamRoomTiles() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.roomTiles.stream();
   }

   public Iterable<Point> getRoomWalls() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.walls;
   }

   public Stream<Point> streamRoomWalls() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.walls.stream();
   }

   public boolean containsTile(int tileX, int tileY) {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.roomTiles.contains(tileX, tileY);
   }

   public HappinessObject getHappinessObjectForSettler(LevelSettler settler) {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.happinessObjects.get(settler.mobUniqueID);
   }

   public int getOccupiedBeds() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return (int)this.beds.stream().filter(b -> b.getSettler() != null).count();
   }

   public Iterable<Point> getDoorTiles() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.doors;
   }

   public Collection<SubRegion> getSubRegions() {
      if (this.getLevel().isOutside(this.tileX, this.tileY)) {
         return new HashSet<>();
      }

      ConnectedSubRegionsResult room = this.data.getLevel().regionManager.getRoomConnectedByTile(this.tileX, this.tileY, true, 2000);
      return room == null ? new HashSet<>() : room.connectedRegions;
   }

   public IntStream streamTileIDsInRoom() {
      return this.streamRoomTiles().mapToInt(tile -> this.getLevel().getTileID(tile.x, tile.y));
   }

   public Stream<GameTile> streamTilesInRoom() {
      return this.streamTileIDsInRoom().mapToObj(TileRegistry::getTile);
   }

   public Stream<LevelObject> streamLevelObjectsInRoom(boolean onlyMainLayer, boolean onlyMultiTileMasters) {
      return onlyMainLayer ? this.streamRoomTiles().map(tile -> {
         LevelObject lo = new LevelObject(this.getLevel(), tile.x, tile.y);
         if (lo.object.getID() == 0) {
            return null;
         } else {
            return onlyMultiTileMasters && !lo.object.isMultiTileMaster() ? null : lo;
         }
      }).filter(Objects::nonNull) : this.streamRoomTiles().flatMap(tile -> IntStream.range(0, ObjectLayerRegistry.getTotalLayers()).mapToObj(layer -> {
         LevelObject lo = new LevelObject(this.getLevel(), layer, tile.x, tile.y);
         if (lo.object.getID() == 0) {
            return null;
         } else {
            return onlyMultiTileMasters && !lo.object.isMultiTileMaster() ? null : lo;
         }
      }).filter(Objects::nonNull));
   }

   public Iterable<LevelObject> getLevelObjectsInRoom(boolean onlyMainLayer, boolean onlyMultiTileMasters) {
      return this.streamLevelObjectsInRoom(onlyMainLayer, onlyMultiTileMasters)::iterator;
   }

   public Stream<GameObject> streamObjectsInRoom(boolean onlyMainLayer, boolean onlyMultiTileMasters) {
      return onlyMainLayer ? this.streamRoomTiles().map(tile -> {
         GameObject object = this.getLevel().getObject(tile.x, tile.y);
         if (object.getID() == 0) {
            return null;
         } else {
            return onlyMultiTileMasters && !object.isMultiTileMaster() ? null : object;
         }
      }).filter(Objects::nonNull) : this.streamRoomTiles().flatMap(tile -> IntStream.range(0, ObjectLayerRegistry.getTotalLayers()).mapToObj(layer -> {
         GameObject object = this.getLevel().getObject(layer, tile.x, tile.y);
         if (object.getID() == 0) {
            return null;
         } else {
            return onlyMultiTileMasters && !object.isMultiTileMaster() ? null : object;
         }
      }).filter(Objects::nonNull));
   }

   public Iterable<GameObject> getObjectsInRoom(boolean onlyMainLayer, boolean onlyMultiTileMasters) {
      return this.streamObjectsInRoom(onlyMainLayer, onlyMultiTileMasters)::iterator;
   }

   public Stream<GameObject> streamWallsInRoom() {
      return this.streamRoomWalls().map(tile -> this.getLevel().getObject(tile.x, tile.y)).filter(object -> object.getID() != 0);
   }

   public Iterable<GameObject> getWallsInRoom() {
      return this.streamWallsInRoom()::iterator;
   }

   private void calculateHappinessModifiers() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      this.baseThoughts.clear();
      this.happinessScore = 0;
      RoomSizeSettlerThought roomSize = SettlerThoughtRegistry.getRoomSizeThought(this.getRoomSize());
      if (roomSize != null) {
         this.baseThoughts.addThought(roomSize);
      }

      RoomQualitySettlerThought roomQuality = SettlerThoughtRegistry.getRoomQualityThought(this.getFurnitureScore());
      if (roomQuality != null) {
         this.baseThoughts.addThought(roomQuality);
      }

      if (this.getRoomProperty("outsidefloor") > 0) {
         this.baseThoughts.addThought(SettlerThoughtRegistry.missingFloorThought);
      }

      if (this.getRoomProperty("lights") <= 0) {
         this.baseThoughts.addThought(SettlerThoughtRegistry.missingLightsThought);
      }

      int occupiedBeds = this.getOccupiedBeds();
      if (occupiedBeds > 1) {
         this.baseThoughts.addThought(SettlerThoughtRegistry.sharingRoomThought, occupiedBeds - 1);
      }

      this.happinessScore = this.baseThoughts.stream().mapToInt(m -> m.getHappinessModifier(null)).sum();
   }

   public SettlerThoughtsList getBaseThoughts() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      return this.baseThoughts;
   }

   public int getHappinessScore() {
      return this.happinessScore;
   }

   public List<String> getDebugTooltips() {
      if (!this.calculatedStats) {
         this.calculateStats();
      }

      LinkedList<String> list = new LinkedList<>();
      if (!this.roomProperties.isEmpty()) {
         list.add("Room properties:");

         for (Entry<String, Integer> e : this.roomProperties.entrySet()) {
            list.add("  " + e.getKey() + ": " + e.getValue());
         }
      }

      if (!this.furnitureTypes.isEmpty()) {
         list.add("Furniture score: " + this.getFurnitureScore());

         for (Entry<String, Integer> e : this.furnitureTypes.entrySet()) {
            list.add("  " + e.getKey() + ": " + e.getValue());
         }
      }

      return list;
   }
}
